package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.IDriveableType;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcTuning.DriveType;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Drives a ground vehicle NPC along the path Custom NPCs gave it, the way a vehicle moves rather than the way a mob
 * walks: forward or in reverse along its hull, gaining and losing speed gradually, steering for a point ahead on the
 * path, slowing before corners and behind other vehicles, and backing up when it is stuck or keeps circling.
 *
 * <p>
 * Custom NPCs still decides where to go and the path search still decides how; this only replaces the last step, from
 * path to motion. One instance per NPC, server side. {@link #drive} holds the arithmetic and touches no world.
 * </p>
 */
public final class NpcDriving
{
    /** Heading change, in degrees, up to which path nodes count as one straight. */
    private static final float STRAIGHT = 25F;
    /** A wheeled vehicle has to keep rolling to turn at all. */
    private static final float WHEELED_FLOOR = 0.5F;
    private static final float DEFAULT_WHEELED_RADIUS = 3F;
    private static final float HEAVY_TURN_SHARE = 0.6F;
    private static final int STUCK_INTERVAL = 20;
    private static final double STUCK_DISTANCE = 0.15D;
    /** Ticks a vehicle may keep steering hard without getting any closer to its heading before it backs up. */
    private static final int CIRCLING_TICKS = 80;
    private static final int RECOVERY_TICKS = 30;
    private static final float RECOVERY_THROTTLE = 0.6F;
    private static final int FOLLOW_INTERVAL = 10;
    private static final float FOLLOW_CONE = 35F;
    /** Share of full throttle a vehicle keeps up to the end of its path, so that it gets there. */
    private static final float ARRIVAL_THROTTLE = 0.2F;
    /** Blocks from a position it is pushed toward without a path at which a vehicle has arrived. */
    private static final double ARRIVED = 0.3D;

    /** Signed share of the top speed the vehicle is commanded to: negative in reverse. */
    @Getter
    private float throttle;
    @Getter
    private boolean reversing;
    private int recovery;
    private float recoveryThrottle;
    private int circling;
    @Nullable
    private Vec3 anchor;
    private int anchorTick;
    private float followLimit = 1F;
    /** Whether the vehicle was last slowing down for the end of its path, which it then brakes at rather than rolls past. */
    private boolean arriving;
    /** The point the vehicle currently steers for, for the path preview. */
    @Getter @Nullable
    private Vec3 steerPoint;

    /**
     * How the vehicle handles.
     *
     * @param pivot
     *            whether it turns on the spot
     * @param turnStep
     *            most degrees the hull turns per tick
     * @param radius
     *            tightest turning circle of a vehicle that cannot pivot, in blocks
     * @param accelerate
     *            throttle gained per tick
     * @param brake
     *            throttle shed per tick while braking
     * @param coast
     *            throttle shed per tick with nowhere to go
     * @param reverseShare
     *            top speed in reverse as a share of the forward one
     * @param cornerShare
     *            share of its speed kept through a right-angle corner
     * @param floor
     *            share of its speed kept while the hull points away from where it wants to go
     * @param reverse
     *            whether it may drive in reverse to a goal behind it
     * @param reverseAngle
     *            degrees off the hull beyond which a goal counts as behind
     * @param reverseDistance
     *            blocks within which a goal behind is reached in reverse
     */
    public record Settings(boolean pivot, float turnStep, float radius, float accelerate, float brake, float coast, float reverseShare, float cornerShare, float floor, boolean reverse,
        float reverseAngle, double reverseDistance)
    {
        static Settings of(NpcTuning tuning, NpcWeaponOptions options, boolean tracked)
        {
            DriveType type = tuning.driveType();
            if (type == DriveType.AUTO)
                type = tracked ? DriveType.TRACKED : DriveType.WHEELED;
            boolean pivot = type != DriveType.WHEELED;
            int rate = tuning.get(Value.HULL_TURN_RATE);
            float turnStep = (rate > 0 ? rate : 90F) / 20F * (type == DriveType.HEAVY_TRACKED ? HEAVY_TURN_SHARE : 1F);
            int tenths = tuning.get(Value.TURN_RADIUS);
            float radius = tenths > 0 ? tenths / 10F : DEFAULT_WHEELED_RADIUS;
            float floor = options.enabled(Feature.MISALIGNED_THROTTLE) ? tuning.get(Value.THROTTLE_FLOOR) / 100F : 1F;
            if (type == DriveType.HEAVY_TRACKED)
                floor = 0F;
            return new Settings(pivot, turnStep, radius, tuning.get(Value.DRIVE_ACCELERATION) / 2000F, tuning.get(Value.DRIVE_BRAKING) / 2000F, tuning.get(Value.DRIVE_COASTING) / 2000F,
                tuning.get(Value.DRIVE_REVERSE_SPEED) / 100F, tuning.get(Value.CORNER_SPEED) / 100F, floor, options.enabled(Feature.DRIVE_REVERSE), tuning.get(Value.REVERSE_ANGLE),
                tuning.get(Value.REVERSE_DISTANCE));
        }
    }

    /**
     * What the vehicle faces this tick.
     *
     * @param hullYaw
     *            where the hull points
     * @param wantedYaw
     *            the bearing of the point it steers for
     * @param goalDistance
     *            blocks to the end of its path
     * @param cornerAngle
     *            degrees the path turns at the next corner, 0 without one in sight
     * @param cornerDistance
     *            blocks to that corner
     * @param lookahead
     *            blocks ahead within which a corner matters
     * @param speed
     *            blocks the vehicle actually moved per tick
     * @param limit
     *            share of its speed traffic ahead allows, from 0 to 1
     * @param haltDistance
     *            blocks from the end of its path at which it comes to a halt
     * @param topSpeed
     *            blocks per tick it moves at full throttle; 0 when unknown, which leaves it at speed up to its goal
     */
    public record Situation(float hullYaw, float wantedYaw, double goalDistance, float cornerAngle, double cornerDistance, double lookahead, double speed, float limit, double haltDistance,
        double topSpeed)
    {}

    /**
     * The decision for this tick.
     *
     * @param hullYaw
     *            where the hull points afterwards
     * @param throttle
     *            signed share of the top speed
     */
    public record Command(float hullYaw, float throttle)
    {}

    /** Where the path leads from here. */
    private record Route(Vec3 steer, double goalDistance, float cornerAngle, double cornerDistance, double haltDistance)
    {}

    /** Steers and sets the throttle for one tick with somewhere to go. */
    public Command drive(Situation situation, Settings settings)
    {
        float off = Mth.wrapDegrees(situation.wantedYaw() - situation.hullYaw());
        // Once in reverse, the vehicle stays in reverse until the goal is clearly ahead again.
        boolean behind = Math.abs(off) > (reversing ? 90F : settings.reverseAngle());
        reversing = settings.reverse() && behind && situation.goalDistance() <= settings.reverseDistance();
        // In reverse it is the rear that has to point at the goal.
        float steer = reversing ? Mth.wrapDegrees(off + 180F) : off;
        float step = settings.turnStep();
        if (!settings.pivot())
        {
            // A wheeled vehicle turns only as fast as it rolls round its turning circle, which widens with speed.
            double widened = Math.max(0.5D, settings.radius()) * (1D + Math.abs(throttle));
            step = (float) Math.min(step, Math.toDegrees(situation.speed() / widened));
        }
        float hull = MountMotion.turn(situation.hullYaw(), situation.hullYaw() + steer, step);
        float remaining = Math.abs(Mth.wrapDegrees(situation.hullYaw() + steer - hull));
        float share = MountMotion.throttle(remaining, settings.pivot() ? settings.floor() : Math.max(settings.floor(), WHEELED_FLOOR));
        if (situation.cornerAngle() > 0F && situation.lookahead() > 0D)
        {
            float corner = Mth.lerp(Mth.clamp(situation.cornerAngle() / 90F, 0F, 1F), 1F, settings.cornerShare());
            float near = (float) Mth.clamp(1D - (situation.cornerDistance() - 1D) / situation.lookahead(), 0D, 1D);
            share *= Mth.lerp(near, 1F, corner);
        }
        share *= Mth.clamp(situation.limit(), 0F, 1F);
        // It slows down in time to halt where its path ends, rather than rolling past.
        float free = reversing ? share * settings.reverseShare() : share;
        float wanted = Math.min(free, arrival(situation.goalDistance() - situation.haltDistance(), situation.topSpeed(), settings.brake()));
        arriving = wanted < free;
        throttle = approach(reversing ? -wanted : wanted, settings, false);
        return new Command(hull, throttle);
    }

    /**
     * @param remaining
     *            blocks left to where the vehicle halts
     * @param topSpeed
     *            blocks per tick it moves at full throttle
     * @param brake
     *            throttle it sheds per tick while braking
     * @return the highest throttle from which braking still stops it within that distance, never so low that it does not arrive
     */
    static float arrival(double remaining, double topSpeed, float brake)
    {
        if (topSpeed <= 0D)
            return 1F;
        return (float) Mth.clamp(Math.sqrt(2D * brake * Math.max(0D, remaining) / topSpeed), ARRIVAL_THROTTLE, 1D);
    }

    /** Lets the vehicle roll out when it has nowhere to go, or brakes it when it has just got there. */
    public Command idle(float hullYaw, Settings settings)
    {
        reversing = false;
        throttle = approach(0F, settings, !arriving);
        return new Command(hullYaw, throttle);
    }

    /**
     * @param coasting
     *            whether a halt is merely the lack of anywhere to go, rather than something to stop for
     */
    private float approach(float target, Settings settings, boolean coasting)
    {
        if (target == 0F)
            return toward(0F, coasting ? settings.coast() : settings.brake());
        // Changing direction means braking to a halt first.
        if (throttle != 0F && Math.signum(throttle) != Math.signum(target))
            return toward(0F, settings.brake());
        return toward(target, Math.abs(target) > Math.abs(throttle) ? settings.accelerate() : settings.brake());
    }

    private float toward(float target, float step)
    {
        return throttle < target ? Math.min(throttle + step, target) : Math.max(throttle - step, target);
    }

    /** Forgets speed and recovery, for a vehicle that stopped being driven. */
    public void reset()
    {
        throttle = 0F;
        reversing = false;
        recovery = 0;
        circling = 0;
        anchor = null;
        followLimit = 1F;
        steerPoint = null;
        arriving = false;
    }

    /**
     * Drives the NPC for one tick, before it moves.
     *
     * @param hullYaw
     *            where its hull points now
     * @param speed
     *            blocks it actually moved during the last tick
     * @return the decision, whose throttle the caller turns into the NPC's forward movement
     */
    public Command travel(EntityNPCInterface npc, float hullYaw, double speed, NpcTuning tuning, NpcWeaponOptions options)
    {
        Settings settings = Settings.of(tuning, options, tracked(npc));
        Route route = route(npc, tuning.get(Value.DRIVE_LOOKAHEAD), tuning.get(Value.STOP_DISTANCE));
        steerPoint = route == null ? null : route.steer();
        if (route != null && stopsShort(npc, route, tuning.get(Value.STOP_DISTANCE)))
            route = null;
        if (route == null)
        {
            recovery = 0;
            circling = 0;
            anchor = null;
            return idle(hullYaw, settings);
        }
        if (recovery > 0)
        {
            // Backing up while the hull keeps turning toward the path is a three-point turn.
            recovery--;
            float step = settings.pivot() ? settings.turnStep() : settings.turnStep() * 0.5F;
            throttle = recoveryThrottle;
            if (recovery == 0)
                npc.getNavigation().recomputePath();
            return new Command(MountMotion.turn(hullYaw, MountMotion.yaw(route.steer().subtract(npc.position())), step), throttle);
        }
        updateTraffic(npc, hullYaw, tuning.get(Value.FOLLOW_DISTANCE));
        float wanted = MountMotion.yaw(route.steer().subtract(npc.position()));
        // The NPC's own motion is no measure of its speed here: before it moves, ground friction has already been taken off it.
        Command command = drive(new Situation(hullYaw, wanted, route.goalDistance(), route.cornerAngle(), route.cornerDistance(), tuning.get(Value.DRIVE_LOOKAHEAD), speed, followLimit,
            route.haltDistance(), NpcMobility.topSpeed(npc)), settings);
        watchProgress(npc, Mth.wrapDegrees(wanted - command.hullYaw()));
        return command;
    }

    /** @return the forward movement input that makes the NPC move at the commanded share of its speed */
    public static float input(EntityNPCInterface npc, float throttle)
    {
        // The move control scales its input by the pace a goal asks for; without one the plain speed applies.
        float base = npc.zza != 0F ? Math.abs(npc.zza) : (float) npc.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return base * throttle;
    }

    private static boolean tracked(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return model != null && model.getInfoType() instanceof IDriveableType type && type.isTracked();
    }

    private static boolean stopsShort(EntityNPCInterface npc, Route route, int stopDistance)
    {
        if (stopDistance <= 0 || route.goalDistance() > stopDistance || npc.getNavigation().isDone())
            return false;
        npc.getNavigation().stop();
        return true;
    }

    /** Backs the vehicle up when it pushes without moving, or steers hard without coming round. */
    private void watchProgress(EntityNPCInterface npc, float off)
    {
        circling = Math.abs(off) > 45F && Math.abs(throttle) > 0.1F ? circling + 1 : 0;
        boolean stuck = false;
        if (anchor == null || Math.abs(throttle) < 0.3F)
        {
            anchor = npc.position();
            anchorTick = npc.tickCount;
        }
        else if (npc.tickCount - anchorTick >= STUCK_INTERVAL)
        {
            stuck = anchor.distanceToSqr(npc.position()) < STUCK_DISTANCE * STUCK_DISTANCE;
            anchor = npc.position();
            anchorTick = npc.tickCount;
        }
        if (stuck || circling > CIRCLING_TICKS)
        {
            recovery = RECOVERY_TICKS;
            recoveryThrottle = throttle > 0F ? -RECOVERY_THROTTLE : RECOVERY_THROTTLE;
            circling = 0;
            anchor = null;
        }
    }

    /** Looks, every few ticks and at a moment of the NPC's own, for a vehicle ahead to keep a distance from. */
    private void updateTraffic(EntityNPCInterface npc, float hullYaw, int distance)
    {
        if (distance <= 0)
        {
            followLimit = 1F;
            return;
        }
        if ((npc.tickCount + npc.getId()) % FOLLOW_INTERVAL != 0)
            return;
        float heading = reversing ? hullYaw + 180F : hullYaw;
        double nearest = Double.MAX_VALUE;
        AABB area = npc.getBoundingBox().inflate(distance * 2D, 2D, distance * 2D);
        for (EntityNPCInterface other : npc.level().getEntitiesOfClass(EntityNPCInterface.class, area, other -> other != npc && NpcVehicleState.of(other).kind() == FlanModelKind.VEHICLE))
        {
            Vec3 offset = other.position().subtract(npc.position());
            if (Math.abs(Mth.wrapDegrees(MountMotion.yaw(offset) - heading)) <= FOLLOW_CONE)
                nearest = Math.min(nearest, offset.horizontalDistance() - (npc.getBbWidth() + other.getBbWidth()) * 0.5D);
        }
        followLimit = followLimit(nearest, distance);
    }

    /** @return the share of its speed a vehicle keeps with another one that many blocks ahead: none at half the distance or less */
    static float followLimit(double gap, int distance)
    {
        double minimum = distance * 0.5D;
        return (float) Mth.clamp((gap - minimum) / Math.max(0.1D, distance - minimum), 0D, 1D);
    }

    /** Reads the point to steer for, the next corner and the distance to the goal off the NPC's path or its movement input. */
    @Nullable
    private static Route route(EntityNPCInterface npc, int lookahead, int stopDistance)
    {
        Vec3 position = npc.position();
        Path path = npc.getNavigation().getPath();
        if (path == null || path.isDone() || path.getNextNodeIndex() >= path.getNodeCount())
            return inputRoute(npc, position, lookahead);
        int index = path.getNextNodeIndex();
        Vec3 steer = path.getEntityPosAtNode(npc, index);
        float heading = MountMotion.yaw(steer.subtract(position));
        double travelled = horizontal(position, steer);
        float cornerAngle = 0F;
        double cornerDistance = Double.MAX_VALUE;
        Vec3 from = steer;
        for (int node = index + 1; node < path.getNodeCount() && travelled < lookahead * 2D; node++)
        {
            Vec3 to = path.getEntityPosAtNode(npc, node);
            double length = horizontal(from, to);
            float change = Math.abs(Mth.wrapDegrees(MountMotion.yaw(to.subtract(from)) - heading));
            if (cornerAngle == 0F && change > STRAIGHT)
            {
                cornerAngle = change;
                cornerDistance = horizontal(position, from);
            }
            // Steering for a node further along is only safe while the path runs straight and level up to it.
            if (cornerAngle == 0F && Math.abs(to.y - from.y) < 0.6D && travelled + length <= lookahead)
                steer = to;
            travelled += length;
            from = to;
        }
        Vec3 end = path.getEntityPosAtNode(npc, path.getNodeCount() - 1);
        // The navigator calls the path done as soon as the NPC's body reaches its last node.
        float width = npc.getBbWidth();
        return new Route(steer, horizontal(position, end), cornerAngle, cornerDistance, Math.max(stopDistance, width > 0.75F ? width / 2F : 0.75F - width / 2F));
    }

    /** Without a path, a goal may still push the NPC directly: toward a wanted position, or sideways round a target. */
    @Nullable
    private static Route inputRoute(EntityNPCInterface npc, Vec3 position, int lookahead)
    {
        if (npc.getMoveControl().hasWanted())
        {
            Vec3 wanted = new Vec3(npc.getMoveControl().getWantedX(), npc.getMoveControl().getWantedY(), npc.getMoveControl().getWantedZ());
            double distance = horizontal(position, wanted);
            return distance < ARRIVED ? null : new Route(wanted, distance, 0F, Double.MAX_VALUE, ARRIVED);
        }
        if (npc.zza == 0F && npc.xxa == 0F)
            return null;
        Vec3 direction = new Vec3(npc.xxa, 0D, npc.zza).normalize().yRot((float) Math.toRadians(-npc.getYRot()));
        return new Route(position.add(direction.scale(lookahead)), Double.MAX_VALUE, 0F, Double.MAX_VALUE, 0D);
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }
}
