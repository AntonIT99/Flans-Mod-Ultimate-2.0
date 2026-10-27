package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Takes a ground vehicle NPC to where its main weapon has a shot at its target. One whose gun cannot lower itself onto a
 * target close below it, or raise itself onto one close above, backs away until the shot lies within its elevation, in
 * reverse when the way back lies behind it. One whose line of fire is blocked drives on toward its target, round what
 * blocks it as its path leads, until the line is clear. Whatever moves the NPC in a fight asks here before it stands it
 * still or closes it in. One per NPC, kept by its {@link NpcVehicleState}. Server thread.
 */
public final class NpcFiringPosition
{
    /** Ticks between checks whether the main weapon's line of fire is clear. */
    private static final int SIGHT_INTERVAL = 5;
    /** Squared blocks the point driven to may move before the vehicle is sent there again. */
    private static final double REPATH_DISTANCE_SQR = 4D;
    /** Ticks before the vehicle is sent again to a point it stopped short of or found no way to. */
    private static final int REPATH_TICKS = 20;
    /** Blocks beyond the nearest distance its gun reaches the target from that a vehicle backs away to, so that it does not stop on the edge. */
    static final double BACK_MARGIN = 2D;
    /** Degrees within its elevation limit the shot is brought to by backing away, and the fewest degrees off level it is brought to. */
    static final float ELEVATION_MARGIN = 1F;
    static final float MIN_ANGLE = 0.5F;
    /** Degrees off straight away from its target that are tried in turn for a way back. */
    private static final float[] BACK_ANGLES = {0F, 45F, -45F, 90F, -90F};

    private int targetId = -1;
    private int sightCheckedAt = Integer.MIN_VALUE;
    private boolean clear = true;
    /** Where the vehicle was last sent for a shot, and when; null while it is not on its way to one. */
    @Nullable
    private Vec3 ordered;
    private long orderedAt;

    /**
     * Drives a ground vehicle NPC to where its main weapon has a shot at its target, when it has none from where it stands.
     * One that no longer needs to stops where it is.
     *
     * @return whether it does: the caller then leaves the NPC's navigation alone, rather than standing it still or closing in
     */
    public static boolean reposition(EntityNPCInterface npc, @Nullable LivingEntity target)
    {
        NpcVehicleState state = NpcVehicleState.of(npc);
        NpcFiringPosition position = state.getFiringPosition();
        if (target != null && applies(npc, state, target) && position.move(npc, state, target))
            return true;
        if (position.ordered != null)
            npc.getNavigation().stop();
        position.ordered = null;
        return false;
    }

    /** Whether the NPC is a ground vehicle that drives itself for a shot at a target it may fire at. */
    private static boolean applies(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target)
    {
        boolean switched = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.MANOEUVRE_TO_FIRE);
        return switched && TacticKind.of(npc) == TacticKind.VEHICLE && npc.isAlive() && !npc.isKilled() && target.isAlive() && target.level() == npc.level()
            && !(npc.isVehicle() && npc.ais.mountControl) && !state.getFlight().holdsFire();
    }

    /** @return whether the vehicle is on its way to a shot */
    private boolean move(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target)
    {
        Optional<Vec3> origin = state.fireOrigin();
        if (origin.isEmpty())
            return false;
        Vec3 aim = NpcVehicleState.aimPoint(target);
        Optional<Float> limit = state.elevationLimit();
        if (limit.isPresent())
        {
            OptionalDouble back = backDistance(horizontal(origin.get(), aim), origin.get().y - aim.y, limit.get(), npc.stats.ranged.getRange());
            if (back.isPresent())
            {
                backAway(npc, state, target, back.getAsDouble());
                return true;
            }
        }
        // A shell lobbed over what stands in the way needs no clear line.
        if (clear(npc, target, origin.get(), aim) || NpcVehicleState.indirect(npc, target))
            return false;
        closeIn(npc, target);
        return true;
    }

    /**
     * @param horizontal
     *            blocks across the ground from where the rounds leave to the point aimed at
     * @param drop
     *            blocks that point lies below where the rounds leave, negative above
     * @param limit
     *            the pitch nearest to the shot that the gun reaches, relative to the hull and positive downwards
     * @param range
     *            the NPC's firing range
     * @return blocks the vehicle has to back away for its gun to reach that point; empty when backing away does not bring
     *         the shot within the gun's elevation, or only beyond the firing range
     */
    static OptionalDouble backDistance(double horizontal, double drop, float limit, double range)
    {
        // Backing away flattens the shot, which only helps a gun that reaches some way toward the target's side of level.
        float toward = drop >= 0D ? limit : -limit;
        if (toward <= 0F || drop == 0D)
            return OptionalDouble.empty();
        double needed = Math.abs(drop) / Math.tan(Math.toRadians(Math.max(MIN_ANGLE, toward - ELEVATION_MARGIN)));
        double back = needed + BACK_MARGIN - horizontal;
        // A shot that should already be within reach misses it for its arc or the hull's tilt, which backing away does not mend.
        return needed > range || back <= 0D ? OptionalDouble.empty() : OptionalDouble.of(back);
    }

    /** Backs the vehicle away from its target, or as near that way as it finds a path, far enough for its gun to reach it. */
    private void backAway(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target, double distance)
    {
        Vec3 away = new Vec3(npc.getX() - target.getX(), 0D, npc.getZ() - target.getZ());
        // A target straight above or below is backed away from along the hull.
        away = away.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0F, state.getHullYaw() + 180F) : away.normalize();
        Vec3 wanted = npc.position().add(away.scale(distance));
        if (underway(npc, wanted))
            return;
        PathNavigation navigation = npc.getNavigation();
        for (float angle : BACK_ANGLES)
        {
            Vec3 point = npc.position().add(away.yRot((float) Math.toRadians(angle)).scale(distance));
            if (navigation.moveTo(point.x, point.y, point.z, 1D))
                break;
        }
        order(npc, wanted);
    }

    /** Drives the vehicle on toward its target, along the path round whatever stands in the way. */
    private void closeIn(EntityNPCInterface npc, LivingEntity target)
    {
        if (underway(npc, target.position()))
            return;
        npc.getNavigation().moveTo(target, 1D);
        order(npc, target.position());
    }

    /** Whether nothing the NPC may not fire through stands between its main weapon and the target, looked at every few ticks. */
    private boolean clear(EntityNPCInterface npc, LivingEntity target, Vec3 origin, Vec3 aim)
    {
        if (target.getId() != targetId || sightCheckedAt == Integer.MIN_VALUE || npc.tickCount - sightCheckedAt >= SIGHT_INTERVAL || npc.tickCount < sightCheckedAt)
        {
            targetId = target.getId();
            sightCheckedAt = npc.tickCount;
            NpcTuning tuning = NpcTuning.of(npc);
            int transparent = tuning.sight() == NpcTuning.Sight.SEE_AND_FIRE_THROUGH ? tuning.get(Value.FIRE_LIMIT) : 0;
            clear = NpcSight.clear(npc.level(), origin, aim, transparent, npc);
        }
        return clear;
    }

    /**
     * Whether the vehicle is already on its way to a point, or about it: it is sent again to one that has moved, or one it
     * stopped short of a while ago.
     */
    private boolean underway(EntityNPCInterface npc, Vec3 point)
    {
        boolean stalled = npc.getNavigation().isDone() && npc.level().getGameTime() - orderedAt >= REPATH_TICKS;
        return ordered != null && ordered.distanceToSqr(point) <= REPATH_DISTANCE_SQR && !stalled;
    }

    private void order(EntityNPCInterface npc, Vec3 point)
    {
        ordered = point;
        orderedAt = npc.level().getGameTime();
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }
}
