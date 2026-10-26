package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.GroundHandling;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
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

import java.util.Optional;

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
    /** Most ticks a recovery is lengthened by while the vehicle picks up reverse speed. */
    private static final int MAX_RECOVERY_RAMP = 80;
    private static final int FOLLOW_INTERVAL = 10;
    private static final float FOLLOW_CONE = 35F;
    /** Share of full throttle a vehicle keeps up to the end of its path, so that it gets there. */
    private static final float ARRIVAL_THROTTLE = 0.2F;
    /** Blocks from a position it is pushed toward without a path at which a vehicle has arrived. */
    private static final double ARRIVED = 0.3D;
    /** Ticks of travel at its current speed a vehicle steers ahead for on a straight, when that reaches beyond its lookahead. */
    private static final double STEER_TICKS = 8D;
    /** Farthest, in blocks, a vehicle looks along its path for a corner to brake for. */
    private static final double MAX_CORNER_SCAN = 64D;
    /** Throttle response without a model to take it from, per tick. */
    private static final float DEFAULT_ACCELERATE = 100F / 2000F;
    private static final float DEFAULT_BRAKE = 250F / 2000F;
    private static final float DEFAULT_COAST = 60F / 2000F;

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
    private float steeringRate;
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
     * @param widenRadius
     *            whether the custom/fallback radius widens with throttle; false for the definition-derived rolling radius
     * @param propulsion
     *            the model's engine and brakes, for the responses taken from them; null when every response is a fixed rate
     */
    public record Settings(boolean pivot, float turnStep, float radius, float accelerate, float brake, float coast, float reverseShare, float cornerShare, float floor, boolean reverse,
        float reverseAngle, double reverseDistance, boolean widenRadius, @Nullable Propulsion propulsion)
    {
        public Settings(boolean pivot, float turnStep, float radius, float accelerate, float brake, float coast, float reverseShare, float cornerShare, float floor, boolean reverse,
            float reverseAngle, double reverseDistance)
        {
            this(pivot, turnStep, radius, accelerate, brake, coast, reverseShare, cornerShare, floor, reverse, reverseAngle, reverseDistance, true);
        }

        public Settings(boolean pivot, float turnStep, float radius, float accelerate, float brake, float coast, float reverseShare, float cornerShare, float floor, boolean reverse,
            float reverseAngle, double reverseDistance, boolean widenRadius)
        {
            this(pivot, turnStep, radius, accelerate, brake, coast, reverseShare, cornerShare, floor, reverse, reverseAngle, reverseDistance, widenRadius, null);
        }

        /**
         * @param speed
         *            blocks the vehicle moves per tick
         * @param topSpeed
         *            blocks per tick it moves at full throttle; 0 when unknown, which takes the model's top speed
         * @return throttle gained per tick at that speed
         */
        public float accelerationAt(double speed, double topSpeed)
        {
            return propulsion != null && propulsion.acceleration() ? propulsion.acceleration(speed, topSpeed) : accelerate;
        }

        /** @return throttle shed per tick at that speed while braking */
        public float brakingAt(double speed, double topSpeed)
        {
            return propulsion != null && propulsion.braking() ? propulsion.deceleration(speed, topSpeed, 1D) : brake;
        }

        /** @return throttle shed per tick at that speed with nowhere to go */
        public float coastingAt(double speed, double topSpeed)
        {
            return propulsion != null && propulsion.coasting() ? propulsion.deceleration(speed, topSpeed, 0D) : coast;
        }

        /** @return blocks the vehicle needs to come to a halt from the given speed while braking, as far as the arithmetic of {@link #arrival} goes */
        public double stoppingDistance(double speed, double topSpeed)
        {
            double top = topSpeed;
            if (top <= 0D && propulsion != null)
                top = propulsion.model().topSpeed();
            float step = brakingAt(speed, topSpeed);
            return top <= 0D || step <= 0F ? 0D : speed * speed / (2D * step * top);
        }

        static Settings of(NpcTuning tuning, NpcWeaponOptions options, boolean tracked, GroundHandling model)
        {
            return of(tuning, options, tracked, model, null);
        }

        static Settings of(NpcTuning tuning, NpcWeaponOptions options, boolean tracked, GroundHandling model, @Nullable GroundPropulsion engine)
        {
            DriveType type = tuning.driveType();
            if (type == DriveType.AUTO)
                type = tracked ? DriveType.TRACKED : DriveType.WHEELED;
            boolean pivot = type != DriveType.WHEELED;
            int rate = tuning.get(Value.HULL_TURN_RATE);
            boolean inheritedTurn = rate == 0 && model.turnRate().isPresent();
            float fallbackRate = rate > 0 ? rate : 90F;
            float fallbackStep = fallbackRate / 20F * (type == DriveType.HEAVY_TRACKED ? HEAVY_TURN_SHARE : 1F);
            float turnStep = inheritedTurn ? (float) model.turnRate().getAsDouble() / 20F : fallbackStep;
            int tenths = tuning.get(Value.TURN_RADIUS);
            boolean inheritedRadius = tenths == 0 && model.turningRadius().isPresent();
            float radius = tenths > 0 ? tenths / 10F : (float) model.turningRadius().orElse(DEFAULT_WHEELED_RADIUS);
            float reverseShare = reverseShare(tuning.get(Value.DRIVE_REVERSE_SPEED), model, engine);
            float floor = options.enabled(Feature.MISALIGNED_THROTTLE) ? tuning.get(Value.THROTTLE_FLOOR) / 100F : 1F;
            if (type == DriveType.HEAVY_TRACKED)
                floor = 0F;
            int acceleration = tuning.get(Value.DRIVE_ACCELERATION);
            int braking = tuning.get(Value.DRIVE_BRAKING);
            int coasting = tuning.get(Value.DRIVE_COASTING);
            return new Settings(pivot, turnStep, radius, rate(acceleration, DEFAULT_ACCELERATE), rate(braking, DEFAULT_BRAKE), rate(coasting, DEFAULT_COAST), reverseShare,
                tuning.get(Value.CORNER_SPEED) / 100F, floor, options.enabled(Feature.DRIVE_REVERSE) && reverseShare > 0F, tuning.get(Value.REVERSE_ANGLE), tuning.get(Value.REVERSE_DISTANCE),
                !inheritedRadius, Propulsion.of(engine, acceleration == 0, braking == 0, coasting == 0));
        }

        /** Without a stated reverse speed the vehicle reverses as its model says, or as its engine does at the share its throttle allows. */
        private static float reverseShare(int percent, GroundHandling model, @Nullable GroundPropulsion engine)
        {
            if (percent > 0)
                return percent / 100F;
            if (model.reverseShare().isPresent())
                return (float) model.reverseShare().getAsDouble();
            return engine == null ? 0.4F : (float) Mth.clamp(engine.reverseSpeed() / engine.topSpeed(), 0D, 1D);
        }

        /** @return throttle per tick for a rate in percent per second, the fallback for 0 */
        private static float rate(int percentPerSecond, float fallback)
        {
            return percentPerSecond > 0 ? percentPerSecond / 2000F : fallback;
        }
    }

    /**
     * The engine and brakes of the vehicle model, for the responses left to inherit them.
     *
     * @param acceleration
     *            whether the throttle rises as the engine accelerates the vehicle
     * @param braking
     *            whether it falls as the brakes slow it down
     * @param coasting
     *            whether it falls as rolling resistance slows it down
     */
    public record Propulsion(GroundPropulsion model, boolean acceleration, boolean braking, boolean coasting)
    {
        /** @return the engine for the responses that inherit it, or null when none does or there is no engine */
        @Nullable
        static Propulsion of(@Nullable GroundPropulsion engine, boolean acceleration, boolean braking, boolean coasting)
        {
            return engine != null && (acceleration || braking || coasting) ? new Propulsion(engine, acceleration, braking, coasting) : null;
        }

        /**
         * The response follows the throttle, not the blocks: a vehicle given its own land speed picks it up and sheds it
         * in the same time the model takes for its top speed.
         *
         * @return throttle gained per tick at that speed, under full demand
         */
        float acceleration(double speed, double topSpeed)
        {
            return share(model.acceleration(modelSpeed(speed, topSpeed)));
        }

        /** @return throttle shed per tick at that speed, under the given share of the brake */
        float deceleration(double speed, double topSpeed, double brake)
        {
            return share(model.deceleration(modelSpeed(speed, topSpeed), brake));
        }

        private double modelSpeed(double speed, double topSpeed)
        {
            return topSpeed > 0D ? speed / topSpeed * model.topSpeed() : speed;
        }

        private float share(double perTick)
        {
            return Double.isFinite(perTick) ? (float) Mth.clamp(perTick / model.topSpeed(), 0D, 1D) : 0F;
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
        return drive(situation, settings, 0, true);
    }

    /** Optional response time in ticks and tracked stationary-turn permission; defaults retain immediate steering. */
    public Command drive(Situation situation, Settings settings, int responseTicks, boolean stationaryTurn)
    {
        float off = Mth.wrapDegrees(situation.wantedYaw() - situation.hullYaw());
        // Once in reverse, the vehicle stays in reverse until the goal is clearly ahead again.
        boolean behind = Math.abs(off) > (reversing ? 90F : settings.reverseAngle());
        reversing = settings.reverse() && behind && situation.goalDistance() <= settings.reverseDistance();
        // In reverse it is the rear that has to point at the goal.
        float steer = reversing ? Mth.wrapDegrees(off + 180F) : off;
        float hull = steer(situation.hullYaw(), situation.hullYaw() + steer, situation.speed(), settings, responseTicks, stationaryTurn, settings.turnStep());
        float remaining = Math.abs(Mth.wrapDegrees(situation.hullYaw() + steer - hull));
        float share = MountMotion.throttle(remaining, settings.pivot() && stationaryTurn ? settings.floor() : Math.max(settings.floor(), WHEELED_FLOOR));
        float brake = settings.brakingAt(situation.speed(), situation.topSpeed());
        if (situation.cornerAngle() > 0F && situation.lookahead() > 0D)
        {
            float corner = Mth.lerp(Mth.clamp(situation.cornerAngle() / 90F, 0F, 1F), 1F, settings.cornerShare());
            float near = (float) Mth.clamp(1D - (situation.cornerDistance() - 1D) / situation.lookahead(), 0D, 1D);
            // A vehicle that brakes slowly has to start slowing for the corner before it is in sight of the lookahead.
            share *= Math.min(Mth.lerp(near, 1F, corner), approachLimit(situation.cornerDistance() - 1D, situation.topSpeed(), brake, corner));
        }
        share *= Mth.clamp(situation.limit(), 0F, 1F);
        // It slows down in time to halt where its path ends, rather than rolling past.
        float free = reversing ? share * settings.reverseShare() : share;
        float wanted = Math.min(free, arrival(situation.goalDistance() - situation.haltDistance(), situation.topSpeed(), brake));
        arriving = wanted < free;
        throttle = approach(reversing ? -wanted : wanted, settings, false, situation.speed(), situation.topSpeed());
        return new Command(hull, throttle);
    }

    /** Smooths yaw demand while enforcing the current rolling/yaw limits, including during recovery. */
    private float steer(float hull, float wanted, double speed, Settings settings, int responseTicks, boolean stationaryTurn, float maximum)
    {
        float limit = maximum;
        if (!settings.pivot() || !stationaryTurn)
        {
            double radius = settings.widenRadius() ? Math.max(0.5D, settings.radius()) * (1D + Math.abs(throttle)) : Math.max(0.01D, settings.radius());
            limit = (float) Math.min(limit, Math.toDegrees(Math.abs(speed) / radius));
        }
        float error = Mth.wrapDegrees(wanted - hull);
        float requested = Mth.clamp(error, -limit, limit);
        float response = responseTicks <= 0 ? Float.MAX_VALUE : settings.turnStep() / responseTicks;
        steeringRate += Mth.clamp(requested - steeringRate, -response, response);
        // New speed/permission limits apply immediately, even while a previous turn is still unwinding.
        steeringRate = Mth.clamp(steeringRate, -limit, limit);
        steeringRate = Mth.clamp(steeringRate, -Math.abs(error), Math.abs(error));
        return Mth.wrapDegrees(hull + steeringRate);
    }

    /**
     * @param remaining
     *            blocks left to where the vehicle has to be down to the given throttle
     * @param end
     *            the throttle it has to be down to there
     * @return the highest throttle from which braking still brings it down to that throttle in time; 1 with an unknown top speed
     */
    static float approachLimit(double remaining, double topSpeed, float brake, float end)
    {
        if (topSpeed <= 0D)
            return 1F;
        return (float) Mth.clamp(Math.sqrt(end * end + 2D * brake * Math.max(0D, remaining) / topSpeed), end, 1D);
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
        return idle(hullYaw, 0D, 0D, settings);
    }

    /**
     * @param speed
     *            blocks the vehicle actually moved per tick
     * @param topSpeed
     *            blocks per tick it moves at full throttle; 0 when unknown
     */
    public Command idle(float hullYaw, double speed, double topSpeed, Settings settings)
    {
        reversing = false;
        steeringRate = 0F;
        throttle = approach(0F, settings, !arriving, speed, topSpeed);
        return new Command(hullYaw, throttle);
    }

    /**
     * @param coasting
     *            whether a halt is merely the lack of anywhere to go, rather than something to stop for
     */
    private float approach(float target, Settings settings, boolean coasting, double speed, double topSpeed)
    {
        if (target == 0F)
            return toward(0F, coasting ? settings.coastingAt(speed, topSpeed) : settings.brakingAt(speed, topSpeed));
        // Changing direction means braking to a halt first.
        if (throttle != 0F && Math.signum(throttle) != Math.signum(target))
            return toward(0F, settings.brakingAt(speed, topSpeed));
        return toward(target, Math.abs(target) > Math.abs(throttle) ? settings.accelerationAt(speed, topSpeed) : settings.brakingAt(speed, topSpeed));
    }

    private float toward(float target, float step)
    {
        return throttle < target ? Math.min(throttle + step, target) : Math.max(throttle - step, target);
    }

    /** Forgets speed and recovery, for a vehicle that stopped being driven. */
    public void reset()
    {
        throttle = 0F;
        steeringRate = 0F;
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
        Settings settings = Settings.of(tuning, options, tracked(npc), handling(npc, options), propulsion(npc, options).orElse(null));
        double topSpeed = NpcMobility.topSpeed(npc);
        int lookahead = tuning.get(Value.DRIVE_LOOKAHEAD);
        // A fast vehicle steers further along a straight, and looks far enough ahead to brake for the next corner in time.
        double steerAhead = Math.min(Value.DRIVE_LOOKAHEAD.maximum(), Math.max(lookahead, speed * STEER_TICKS));
        double scan = Math.min(MAX_CORNER_SCAN, Math.max(lookahead * 2D, steerAhead + settings.stoppingDistance(speed, topSpeed)));
        Route route = route(npc, steerAhead, scan, tuning.get(Value.STOP_DISTANCE));
        steerPoint = route == null ? null : route.steer();
        if (route != null && stopsShort(npc, route, tuning.get(Value.STOP_DISTANCE)))
            route = null;
        if (route == null)
        {
            recovery = 0;
            circling = 0;
            anchor = null;
            return idle(hullYaw, speed, topSpeed, settings);
        }
        if (recovery > 0)
        {
            // Backing up while the hull keeps turning toward the path is a three-point turn.
            recovery--;
            float step = settings.pivot() ? settings.turnStep() : settings.turnStep() * 0.5F;
            throttle = approach(recoveryThrottle, settings, false, speed, topSpeed);
            if (recovery == 0)
                npc.getNavigation().recomputePath();
            return new Command(
                steer(hullYaw, MountMotion.yaw(route.steer().subtract(npc.position())), speed, settings, tuning.get(Value.STEERING_RESPONSE), tuning.get(Value.STATIONARY_TURN) != 0, step), throttle);
        }
        updateTraffic(npc, hullYaw, tuning.get(Value.FOLLOW_DISTANCE), tuning.get(Value.TRAFFIC_STOP_GAP));
        float wanted = MountMotion.yaw(route.steer().subtract(npc.position()));
        // The NPC's own motion is no measure of its speed here: before it moves, ground friction has already been taken off it.
        Command command = drive(new Situation(hullYaw, wanted, route.goalDistance(), route.cornerAngle(), route.cornerDistance(), lookahead, speed, followLimit, route.haltDistance(), topSpeed),
            settings, tuning.get(Value.STEERING_RESPONSE), tuning.get(Value.STATIONARY_TURN) != 0);
        watchProgress(npc, Mth.wrapDegrees(wanted - command.hullYaw()), settings.accelerationAt(speed, topSpeed));
        return command;
    }

    /** @return the forward movement input that makes the NPC move at the commanded share of its speed */
    public static float input(EntityNPCInterface npc, float throttle)
    {
        // The move control scales its input by the pace a goal asks for; without one the plain speed applies.
        float base = npc.zza != 0F ? Math.abs(npc.zza) : (float) npc.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return base * throttle;
    }

    /** Effective model handling for runtime movement and editor readouts; does not mutate the NPC. */
    public static Settings settings(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        return Settings.of(NpcTuning.of(npc), options, tracked(npc), handling(npc, options), propulsion(npc, options).orElse(null));
    }

    static GroundHandling handling(EntityNPCInterface npc, NpcWeaponOptions options)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || !options.enabled(Feature.TYPE_PROPERTIES))
            return GroundHandling.EMPTY;
        return NpcMobility.afloat(npc) ? FlansEntityTypes.getWaterHandling(model.getInfoType()) : FlansEntityTypes.getGroundHandling(model.getInfoType());
    }

    /** @return how the vehicle model gains and sheds speed, on land or in water, when the NPC takes properties from its model */
    public static Optional<GroundPropulsion> propulsion(EntityNPCInterface npc, NpcWeaponOptions options)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        if (model == null || !options.enabled(Feature.TYPE_PROPERTIES))
            return Optional.empty();
        return NpcMobility.afloat(npc) ? FlansEntityTypes.getWaterPropulsion(model.getInfoType()) : FlansEntityTypes.getGroundPropulsion(model.getInfoType());
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

    /**
     * Backs the vehicle up when it pushes without moving, or steers hard without coming round.
     *
     * @param accelerate
     *            throttle the vehicle gains per tick, which decides how long it needs to back up
     */
    private void watchProgress(EntityNPCInterface npc, float off, float accelerate)
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
            // A vehicle that picks up speed slowly backs up for longer, so that it gets anywhere at all.
            recovery = RECOVERY_TICKS + Math.min(MAX_RECOVERY_RAMP, Mth.ceil(RECOVERY_THROTTLE / Math.max(1.0E-3F, accelerate)));
            recoveryThrottle = throttle > 0F ? -RECOVERY_THROTTLE : RECOVERY_THROTTLE;
            circling = 0;
            anchor = null;
        }
    }

    /** Looks, every few ticks and at a moment of the NPC's own, for a vehicle ahead to keep a distance from. */
    private void updateTraffic(EntityNPCInterface npc, float hullYaw, int distance, int stopGapTenths)
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
        followLimit = followLimit(nearest, distance, stopGapTenths);
    }

    /** @return the share of its speed a vehicle keeps with another one that many blocks ahead: none at half the distance or less */
    static float followLimit(double gap, int distance)
    {
        return followLimit(gap, distance, 0);
    }

    /** Zero inherits half the following distance; a custom threshold cannot exceed the following distance. */
    static float followLimit(double gap, int distance, int stopGapTenths)
    {
        if (distance <= 0)
            return 1F;
        double minimum = stopGapTenths <= 0 ? distance * 0.5D : Math.min(distance, stopGapTenths / 10D);
        if (gap <= minimum)
            return 0F;
        return (float) Mth.clamp((gap - minimum) / Math.max(0.1D, distance - minimum), 0D, 1D);
    }

    /** Reads the point to steer for, the next corner and the distance to the goal off the NPC's path or its movement input. */
    @Nullable
    private static Route route(EntityNPCInterface npc, double lookahead, double scan, int stopDistance)
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
        for (int node = index + 1; node < path.getNodeCount() && travelled < scan; node++)
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
    private static Route inputRoute(EntityNPCInterface npc, Vec3 position, double lookahead)
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
