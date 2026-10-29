package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcBlastSafety;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.sim.NpcVehicleState.GunReach;
import com.wolffsmod.npcs.tactics.NpcTactics;
import com.wolffsmod.npcs.tactics.ShipBearing;
import com.wolffsmod.npcs.tactics.ShipBearing.Course;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Takes a ground vehicle or ship NPC to where its main weapon has a shot at its target. One standing inside the reach of
 * its own explosive rounds backs away until they cannot catch it. One whose gun cannot lower itself onto a
 * target close below it, or raise itself onto one close above, backs away until the shot lies within its elevation, in
 * reverse when the way back lies behind it; so does one whose target stands under its barrel, nearer the turret than the
 * muzzle, where no elevation reaches it. One whose line of fire is blocked drives on toward its target, round what
 * blocks it as its path leads, until the line is clear. A ship steams a course of its own instead, as
 * {@link ShipBearing} lays it, since only its bow or its side brings its weapons to bear. Whatever moves the NPC in a
 * fight asks here before it stands it still or closes it in. One per NPC, kept by its {@link NpcVehicleState}. Server
 * thread.
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
    /** What a ship is doing with its target, so that it does not flap between running in and turning away; null for anything else. */
    @Nullable
    private Course course;

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
        position.course = null;
        return false;
    }

    /** Whether the NPC is a ground vehicle or ship that drives itself for a shot at a target it may fire at. */
    private static boolean applies(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        boolean switched = options.enabled(Feature.MANOEUVRE_TO_FIRE) || options.enabled(Feature.BLAST_STANDOFF) || options.enabled(Feature.SHIP_WEAPON_BEARING);
        return switched && TacticKind.of(npc) == TacticKind.VEHICLE && npc.isAlive() && !npc.isKilled() && target.isAlive() && target.level() == npc.level()
            && !(npc.isVehicle() && npc.ais.mountControl) && !state.getFlight().holdsFire();
    }

    /** @return whether the vehicle is on its way to a shot */
    private boolean move(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        // Out of the reach of its own rounds first: there is no shot worth lining up from inside its own blast.
        double standoff = NpcBlastSafety.standoff(npc);
        double gap = npc.distanceTo(target);
        if (standoff > gap)
        {
            backAway(npc, state, target, standoff - gap);
            return true;
        }
        if (options.enabled(Feature.SHIP_WEAPON_BEARING) && state.isShip())
        {
            steam(npc, state, target, standoff);
            return true;
        }
        course = null;
        if (!options.enabled(Feature.MANOEUVRE_TO_FIRE))
            return false;
        Optional<Vec3> origin = state.fireOrigin();
        if (origin.isEmpty())
            return false;
        Vec3 aim = NpcVehicleState.aimPoint(target);
        double range = npc.stats.ranged.getRange();
        Optional<GunReach> gun = state.gunReach(npc);
        // A turret's gun elevates about its pivot, so the shot it can lay runs from there rather than from where the muzzle swings to.
        Vec3 from = gun.filter(GunReach::elevates).map(GunReach::pivot).orElse(origin.get());
        Optional<Float> limit = state.elevationLimit();
        OptionalDouble back = limit.isPresent() ? backDistance(horizontal(from, aim), from.y - aim.y, limit.get(), range) : OptionalDouble.empty();
        // A target under the barrel is out of reach whichever way the gun points, and a shot aimed from the muzzle swings it round after it.
        OptionalDouble clearOfBarrel = gun.map(reach -> reachDistance(horizontal(reach.pivot(), aim), reach.reach(), range)).orElse(OptionalDouble.empty());
        if (back.isPresent() || clearOfBarrel.isPresent())
        {
            backAway(npc, state, target, Math.max(back.orElse(0D), clearOfBarrel.orElse(0D)));
            return true;
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

    /**
     * @param horizontal
     *            blocks across the ground from the point the gun turns about to the point aimed at
     * @param reach
     *            blocks across the ground from that point to the muzzles
     * @param range
     *            the NPC's firing range
     * @return blocks the vehicle has to back away for the point aimed at to lie beyond its muzzles; empty when it already
     *         does, or would only from beyond the firing range
     */
    static OptionalDouble reachDistance(double horizontal, double reach, double range)
    {
        return horizontal >= reach || reach > range ? OptionalDouble.empty() : OptionalDouble.of(reach + BACK_MARGIN - horizontal);
    }

    /**
     * Steams a ship so that its weapons bear: bow on to fire what is fixed to its hull, away again after a run, and round
     * its target to bring its turrets to bear once it has nothing fixed to fire. It is sent on again whenever its course
     * changes, and otherwise now and then, since the point it steams for moves along with it.
     *
     * @param standoff
     *            blocks from its target the ship's own rounds would catch it within, which it never circles inside
     */
    private void steam(EntityNPCInterface npc, NpcVehicleState state, LivingEntity target, double standoff)
    {
        double hold = Math.max(standoff, NpcTactics.engagement(npc).preferred());
        Course wanted = ShipBearing.course(state.hullFixedWeapon(), horizontal(npc.position(), target.position()), hold, course);
        long now = npc.level().getGameTime();
        boolean renew = ordered == null || wanted != course || npc.getNavigation().isDone() || now - orderedAt >= REPATH_TICKS;
        course = wanted;
        if (!renew)
            return;
        Vec3 point = ShipBearing.point(wanted, npc.position(), target.position(), hold, (npc.getId() & 1) == 0 ? 1 : -1);
        npc.getNavigation().moveTo(point.x, point.y, point.z, 1D);
        order(npc, point);
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
