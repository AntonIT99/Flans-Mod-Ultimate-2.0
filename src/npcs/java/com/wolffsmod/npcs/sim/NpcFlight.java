package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcTactics;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Makes a flying NPC fly like a plane or a helicopter instead of drifting like a ghast.
 *
 * <p>
 * Both keep a minimum altitude above the terrain under and ahead of them and hold their fire, without giving their
 * target up, while they are below it. A plane never stops: it flies along its heading and comes round in a circle,
 * toward its target or about its home while it has none. Its tactic may instead circle its target, or carry it on past
 * the target after each pass, holding its fire, before it comes round again. A helicopter may hover and turns its hull
 * toward its target.
 * Custom NPCs' flying movement still supplies the climb and descent toward its own goals in between. One instance per
 * NPC, server side.
 * </p>
 */
public final class NpcFlight
{
    private static final int SAMPLE_INTERVAL = 10;
    /** Blocks above the attack altitude each type settles at. */
    private static final double PLANE_MARGIN = 5D;
    private static final double HELICOPTER_MARGIN = 2D;
    /** Blocks an aircraft has to climb past its attack altitude before it fires again, so that it does not flicker. */
    private static final double ATTACK_HYSTERESIS = 2D;
    /** Blocks per tick: a plane left to itself neither stalls below the first nor outruns the second. */
    static final double STALL_SPEED = 0.13D;
    static final double TOP_SPEED = 0.42D;
    private static final double MIN_LOITER_RADIUS = 16D;
    private static final float HELICOPTER_TURN = 3F;
    /** Blocks across the ground within which a plane on a strafing run has passed over its target, and the least it carries on past it. */
    static final double PASS_DISTANCE = 10D;
    static final double MIN_EXTEND = 24D;

    private int sampledAt = Integer.MIN_VALUE;
    /** Height of the highest terrain under and ahead of the aircraft. */
    @Getter
    private int terrain;
    /** Blocks between the aircraft and that terrain. */
    @Getter
    private double clearance;
    private boolean attackAllowed = true;
    /** Whether a plane on a strafing run carries on past its target, holding its fire, before it comes round again. */
    private boolean extending;
    private boolean flying;
    private float yaw;

    public static Flight type(EntityNPCInterface npc)
    {
        return npc.ais.movementType == 1 ? NpcTuning.of(npc).flight() : Flight.NORMAL;
    }

    /** @return whether the aircraft is too low to attack and climbing back */
    public boolean holdsFire()
    {
        return flying && (!attackAllowed || extending);
    }

    /** @return one word for what the aircraft is doing, for the movement debug log */
    public String state(EntityNPCInterface npc)
    {
        if (!flying)
            return "NORMAL";
        if (!attackAllowed)
            return "ALTITUDE_RECOVERY";
        if (type(npc) == Flight.PLANE)
        {
            if (npc.getTarget() == null)
                return "LOITER";
            if (extending)
                return "EXTEND";
            return NpcTactics.of(npc) == Tactic.ORBIT ? "ORBIT" : "ATTACK_RUN";
        }
        return npc.getNavigation().isDone() ? "HOVERING" : "REPOSITIONING";
    }

    /** Runs once per tick after the NPC has moved. */
    public void tick(EntityNPCInterface npc)
    {
        Flight type = type(npc);
        if (type == Flight.NORMAL || npc.isKilled())
        {
            flying = false;
            attackAllowed = true;
            extending = false;
            return;
        }
        if (!flying)
        {
            flying = true;
            yaw = npc.getYRot();
            sampledAt = Integer.MIN_VALUE;
        }
        NpcTuning tuning = NpcTuning.of(npc);
        int minimum = tuning.get(Value.ATTACK_ALTITUDE);
        sample(npc, type, minimum);
        Vec3 motion = npc.getDeltaMovement();
        double vertical = climb(motion.y, terrain + minimum + (type == Flight.PLANE ? PLANE_MARGIN : HELICOPTER_MARGIN) - npc.getY(), type, npc.getNavigation().isDone());
        LivingEntity target = npc.getTarget();
        boolean engaged = target != null && target.isAlive();
        if (type == Flight.PLANE)
        {
            float wanted = engaged ? attackYaw(npc, target) : loiterYaw(npc.position(), new Vec3(npc.getStartXPos(), npc.getY(), npc.getStartZPos()), loiterRadius(npc), (npc.getId() & 1) == 0, yaw);
            if (!engaged)
                extending = false;
            double speed = planeSpeed(tuning.get(Value.FLIGHT_SPEED), motion.horizontalDistance());
            yaw = MountMotion.turn(yaw, wanted, planeTurn(speed, loiterRadius(npc)));
            Vec3 forward = Vec3.directionFromRotation(0F, yaw);
            npc.setDeltaMovement(forward.x * speed, vertical, forward.z * speed);
        }
        else
        {
            float wanted = yaw;
            if (engaged)
                wanted = MountMotion.yaw(target.position().subtract(npc.position()));
            else if (motion.horizontalDistanceSqr() > 4.0E-4D)
                wanted = MountMotion.yaw(motion);
            yaw = MountMotion.turn(yaw, wanted, HELICOPTER_TURN);
            npc.setDeltaMovement(motion.x, vertical, motion.z);
        }
        npc.setYRot(yaw);
        npc.setYBodyRot(yaw);
    }

    /**
     * The heading of a plane with a target, by its tactic: at the target, round it, or straight on past it after a pass.
     * Custom NPCs' own tactics fly at it.
     */
    private float attackYaw(EntityNPCInterface npc, LivingEntity target)
    {
        Tactic tactic = NpcTactics.of(npc);
        double horizontal = Math.sqrt(npc.distanceToSqr(target.getX(), npc.getY(), target.getZ()));
        if (tactic == Tactic.ORBIT)
        {
            extending = false;
            double radius = Math.max(MIN_LOITER_RADIUS, NpcTactics.engagement(npc).preferred());
            return loiterYaw(npc.position(), new Vec3(target.getX(), npc.getY(), target.getZ()), radius, (npc.getId() & 1) == 0, yaw);
        }
        extending = tactic == Tactic.STRAFING_RUN && extending(extending, horizontal, Math.max(MIN_EXTEND, NpcTactics.engagement(npc).preferred()));
        if (extending || horizontal <= 1D)
            return yaw;
        return MountMotion.yaw(target.position().subtract(npc.position()));
    }

    /**
     * @param extending
     *            whether the plane is carrying on past its target after a pass
     * @param horizontal
     *            blocks to the target across the ground
     * @param extend
     *            blocks the plane carries on to before it turns back
     * @return whether it is carrying on past its target now: from the moment it passes over the target until it is far enough beyond
     */
    static boolean extending(boolean extending, double horizontal, double extend)
    {
        return extending ? horizontal < extend : horizontal < PASS_DISTANCE;
    }

    private static double loiterRadius(EntityNPCInterface npc)
    {
        return Math.max(MIN_LOITER_RADIUS, npc.ais.walkingRange);
    }

    /**
     * @param vertical
     *            the aircraft's vertical speed
     * @param error
     *            blocks it is below the height it should hold; negative above it
     * @return the vertical speed that brings it back: a firm climb when too low, a gentle descent only when far too
     *         high, and a helicopter with nowhere to go settles into a hover
     */
    static double climb(double vertical, double error, Flight type, boolean idle)
    {
        boolean plane = type == Flight.PLANE;
        if (error > 0.5D)
            return Math.max(vertical, Math.min(plane ? 0.24D : 0.30D, 0.06D + error * 0.035D));
        if (error < -5D)
            return Math.min(vertical, Math.max(plane ? -0.12D : -0.16D, error * 0.025D));
        return !plane && idle ? vertical * 0.55D : vertical;
    }

    /** @return blocks per tick a plane flies at: its set speed, or its own between stall and top speed */
    static double planeSpeed(int tenthsPerSecond, double current)
    {
        return tenthsPerSecond > 0 ? tenthsPerSecond / 200D : Mth.clamp(current, STALL_SPEED, TOP_SPEED);
    }

    /** @return degrees per tick a plane turns: what flying its loiter circle at its speed takes, so it never spins in place */
    static float planeTurn(double speed, double radius)
    {
        return (float) Mth.clamp(Math.toDegrees(speed / Math.max(1D, radius)), 0.25D, 2D);
    }

    /**
     * @param centre
     *            the point to circle
     * @param current
     *            the heading kept while the plane is right over the centre
     * @return the heading that flies a circle of the given radius: along the tangent, leaning in when outside the
     *         circle and out when inside
     */
    static float loiterYaw(Vec3 position, Vec3 centre, double radius, boolean clockwise, float current)
    {
        double dx = position.x - centre.x;
        double dz = position.z - centre.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1D)
            return current;
        double radialX = dx / distance;
        double radialZ = dz / distance;
        double tangentX = clockwise ? -radialZ : radialZ;
        double tangentZ = clockwise ? radialX : -radialX;
        double correction = Mth.clamp((distance - radius) / radius, -0.75D, 0.75D) * 1.5D;
        return MountMotion.yaw(new Vec3(tangentX - radialX * correction, 0D, tangentZ - radialZ * correction));
    }

    /** Measures the terrain every few ticks, at a moment of the NPC's own: under a helicopter and round it, under a plane and ahead of it. */
    private void sample(EntityNPCInterface npc, Flight type, int minimum)
    {
        if (sampledAt != Integer.MIN_VALUE && (npc.tickCount + npc.getId()) % SAMPLE_INTERVAL != 0 && npc.tickCount - sampledAt < SAMPLE_INTERVAL * 2)
            return;
        sampledAt = npc.tickCount;
        int x = npc.getBlockX();
        int z = npc.getBlockZ();
        int highest = surface(npc, x, z);
        if (type == Flight.HELICOPTER)
        {
            for (int[] offset : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}})
                highest = Math.max(highest, surface(npc, x + offset[0], z + offset[1]));
        }
        else
        {
            Vec3 forward = Vec3.directionFromRotation(0F, yaw);
            for (int distance = 8; distance <= 24; distance += 8)
                highest = Math.max(highest, surface(npc, Mth.floor(npc.getX() + forward.x * distance), Mth.floor(npc.getZ() + forward.z * distance)));
        }
        terrain = highest;
        clearance = npc.getY() - highest;
        attackAllowed = clearance >= minimum + (attackAllowed ? 0D : ATTACK_HYSTERESIS);
    }

    /** @return the height of the ground or water at a column; the aircraft's own height where the chunk is not loaded */
    private static int surface(EntityNPCInterface npc, int x, int z)
    {
        Level level = npc.level();
        return level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : npc.getBlockY();
    }
}
