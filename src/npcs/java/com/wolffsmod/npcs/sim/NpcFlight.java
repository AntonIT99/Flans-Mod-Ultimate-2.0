package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.FlightPerformance;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcTactics;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Makes a flying NPC fly like a plane or a helicopter instead of drifting like a ghast.
 *
 * <p>
 * Both keep a minimum altitude above the terrain under and ahead of them and hold their fire, without giving their
 * target up, while they are below it. A plane never stops: it flies along its heading and comes round in a circle,
 * toward its target or about its home while it has none, never tighter than it turns at its speed. Its tactic may instead
 * circle its target, turning in for a gun pass now and then, or carry it on past the target after each pass, holding its fire, before it comes round again. On a gun run it dives or climbs to put its
 * nose on the target, as its fixed guns need; a bomber flies over its target level. A helicopter may hover and turns its hull
 * toward its target.
 * Custom NPCs' flying movement still supplies the climb and descent toward its own goals in between. An aircraft model
 * flies at its definition's speeds, climb rate and control rates: a plane with nothing to fight cruises just above the speed
 * it needs to fly and builds up to its top speed to fight, gains speed in a dive and loses it in a climb, never below the
 * speed it needs to fly, and turns as tightly as its steepest bank allows at its speed. One instance per NPC, server side.
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
    /** Degrees per tick a plane turns at least and, without a turn rate or a model, at most. */
    static final float MIN_PLANE_TURN = 0.25F;
    static final float MAX_PLANE_TURN = 2F;
    /** How much more a plane can turn than the circle it flies takes, so that it can come back onto it when it drifts off. */
    static final double CIRCLE_MARGIN = 1.25D;
    private static final float HELICOPTER_TURN = 3F;
    /** Blocks across the ground within which a plane on a strafing run has passed over its target, and the least it carries on past it. */
    static final double PASS_DISTANCE = 10D;
    static final double MIN_EXTEND = 24D;
    /** Degrees per tick a plane raises or lowers its nose, and the steepest it dives or climbs at a target. */
    static final float PITCH_RATE = 2.5F;
    static final float MAX_DIVE = 45F;
    static final float MAX_CLIMB = 30F;
    /** Degrees off its heading within which a plane noses over toward its target rather than first turning to it. */
    static final float DIVE_CONE = 30F;
    /** Degrees a plane banks per degree a tick it turns, the steepest bank, and degrees a tick it rolls toward it. */
    static final float BANK_PER_TURN = 20F;
    static final float MAX_BANK = 45F;
    static final float ROLL_RATE = 3F;
    /** Ticks an orbiting plane circles its target before it turns in for a gun pass, and the longest a pass lasts. */
    static final int ORBIT_PASS_INTERVAL = 200;
    static final int MAX_PASS_TICKS = 300;
    /** Blocks per tick a plane climbs at most, and a helicopter, without a climb rate of their own. */
    static final double PLANE_CLIMB = 0.24D;
    static final double HELICOPTER_CLIMB = 0.30D;
    /** Share of the gap to the speed it is throttled to that a plane flying its model closes each tick. */
    static final double THRUST_RESPONSE = 0.02D;
    /** How far above the least speed its model flies at a plane with nothing to fight cruises, so that a climb does not hold it at that speed. */
    static final double CRUISE_MARGIN = 1.1D;
    /** Speed a plane gains per tick per unit of the sine of its dive, as gravity pulls it along its flight path. */
    static final double PATH_GRAVITY = 0.098D;
    /** Fastest a plane flying its model turns, in degrees per tick, however slow it is. */
    static final float MAX_COORDINATED_TURN = 6F;
    /** Least and most a model's control rates scale the default pitch and roll rates by. */
    private static final double MIN_AUTHORITY = 0.25D;
    private static final double MAX_AUTHORITY = 3D;

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
    /**
     * Degrees a plane's nose points below the horizon, negative above it, as Minecraft counts pitch: along its flight path,
     * or at its target during an attack run. Its fixed guns point there.
     */
    @Getter
    private float pitch;
    /** Degrees a plane is rolled to its right, the right wing down, negative to its left, as it banks into its turns. */
    @Getter
    private float bank;
    /** Whether an orbiting plane has turned in on its target for a gun pass. */
    private boolean passing;
    /** Ticks an orbiting plane has circled since its last pass, or spent on the pass it is making. */
    private int orbitTicks;
    /** Blocks per tick a plane flying its model flies at; 0 until it takes off from its measured speed. */
    private double airspeed;

    /** @return how the NPC flies: as its flight type says, Automatic resolved by its model; Normal unless it moves by flying */
    public static Flight type(EntityNPCInterface npc)
    {
        if (npc.ais.movementType != 1)
            return Flight.NORMAL;
        Flight flight = NpcTuning.of(npc).flight();
        return flight == Flight.AUTO ? automatic(model(npc)) : flight;
    }

    /** @return Helicopter for a rotorcraft model, Plane for any other aircraft model, Normal for anything else, a ship among them */
    static Flight automatic(@Nullable IContentType model)
    {
        if (!(model instanceof IDriveableType driveable) || driveable.getDriveableKind() != DriveableKind.PLANE || driveable.isShip())
            return Flight.NORMAL;
        boolean rotorcraft = FlansEntityTypes.getFlightPerformance(model).map(FlightPerformance::rotorcraft).orElse(driveable.isHelicopter());
        return rotorcraft ? Flight.HELICOPTER : Flight.PLANE;
    }

    /** @return how the aircraft model the NPC shows flies, when it flies as its model and takes the model's properties */
    public static Optional<FlightPerformance> performance(EntityNPCInterface npc)
    {
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        IContentType model = model(npc);
        if (model == null || !options.enabled(Feature.MODEL_FLIGHT) || !options.enabled(Feature.TYPE_PROPERTIES))
            return Optional.empty();
        return FlansEntityTypes.getFlightPerformance(model);
    }

    @Nullable
    private static IContentType model(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        return model == null ? null : model.getInfoType();
    }

    /** @return degrees per second the aircraft model turns at its top speed, as the editor shows it while the turn rate is inherited */
    public static double modelTurnRate(FlightPerformance model)
    {
        return (model.rotorcraft() ? HELICOPTER_TURN : coordinatedTurn(model.topSpeed())) * 20D;
    }

    /** @return blocks per tick the aircraft climbs at most: its climb rate setting, its model's, or the default of its type */
    public static double maxClimb(NpcTuning tuning, @Nullable FlightPerformance model, Flight type)
    {
        int tenths = tuning.get(Value.CLIMB_RATE);
        if (tenths > 0)
            return tenths / 200D;
        if (model != null && model.climbRate().isPresent())
            return model.climbRate().getAsDouble();
        return type == Flight.PLANE ? PLANE_CLIMB : HELICOPTER_CLIMB;
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
            return NpcTactics.of(npc) == Tactic.ORBIT && !passing ? "ORBIT" : "ATTACK_RUN";
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
            passing = false;
            orbitTicks = 0;
            pitch = 0F;
            bank = 0F;
            airspeed = 0D;
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
        FlightPerformance model = performance(npc).orElse(null);
        double vertical = climb(motion.y, terrain + minimum + (type == Flight.PLANE ? PLANE_MARGIN : HELICOPTER_MARGIN) - npc.getY(), type, npc.getNavigation().isDone(),
            maxClimb(tuning, model, type));
        LivingEntity target = npc.getTarget();
        boolean engaged = target != null && target.isAlive();
        if (type == Flight.PLANE)
            flyPlane(npc, tuning, model, engaged ? target : null, vertical, minimum);
        else
            flyHelicopter(npc, tuning, model, engaged ? target : null, vertical);
        npc.setYRot(yaw);
        npc.setYBodyRot(yaw);
    }

    /**
     * Turns a helicopter's hull toward its target, or along its travel, and holds its height; it flies no faster than its
     * model does.
     *
     * @param model
     *            how the helicopter's model flies, when it flies as its model; null for the defaults
     * @param target
     *            the living target the helicopter fights, or null
     * @param vertical
     *            the vertical speed that holds its altitude
     */
    private void flyHelicopter(EntityNPCInterface npc, NpcTuning tuning, @Nullable FlightPerformance model, @Nullable LivingEntity target, double vertical)
    {
        Vec3 motion = npc.getDeltaMovement();
        pitch = 0F;
        bank = 0F;
        float wanted = yaw;
        // Its guns fire along its hull, so it faces where their rounds have to go.
        if (target != null)
            wanted = MountMotion.yaw(NpcVehicleState.of(npc).aimDirection().orElse(target.position().subtract(npc.position())));
        else if (motion.horizontalDistanceSqr() > 4.0E-4D)
            wanted = MountMotion.yaw(motion);
        int turnRate = tuning.get(Value.FLIGHT_TURN_RATE);
        yaw = MountMotion.turn(yaw, wanted, turnRate > 0 ? turnRate / 20F : HELICOPTER_TURN);
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double cap = model != null && horizontal > model.topSpeed() ? model.topSpeed() / horizontal : 1D;
        npc.setDeltaMovement(motion.x * cap, vertical, motion.z * cap);
    }

    /**
     * Flies a plane along its heading, coming round toward its target or about its home. On an attack run with its guns it
     * puts its nose on where its rounds have to go, diving or climbing as steeply as the run allows; otherwise its nose
     * follows its flight path. It banks into its turns.
     *
     * @param model
     *            how the plane's model flies, when it flies as its model; null for the defaults
     * @param target
     *            the living target the plane fights, or null
     * @param vertical
     *            the vertical speed that holds the plane's altitude
     * @param minimum
     *            blocks above the terrain the plane must stay to attack
     */
    private void flyPlane(EntityNPCInterface npc, NpcTuning tuning, @Nullable FlightPerformance model, @Nullable LivingEntity target, double vertical, int minimum)
    {
        Vec3 motion = npc.getDeltaMovement();
        // Guns are led at where the target will be; a bomber flies over where its bomb meets the target.
        NpcVehicleState state = NpcVehicleState.of(npc);
        Vec3 toTarget = target == null ? Vec3.ZERO : state.bombPoint().orElse(target.position()).subtract(npc.position());
        Vec3 aim = target != null && !state.isBombing() ? state.aimDirection().orElse(toTarget) : toTarget;
        // The horizontal speed of a diving or climbing plane is only part of its airspeed.
        double speed = flightSpeed(tuning, model, target == null, motion.horizontalDistance() / Math.max(0.5D, Math.cos(Math.toRadians(pitch))));
        int turnRate = tuning.get(Value.FLIGHT_TURN_RATE);
        // No circle is tighter than the plane turns at its speed, with some turn to spare to stay on it.
        double tightest = turnRadius(speed, maxTurn(turnRate, speed, model != null)) * CIRCLE_MARGIN;
        float wanted;
        double circle;
        if (target != null && NpcTactics.of(npc) == Tactic.ORBIT)
            circle = Math.max(tightest, orbitRadius(npc));
        else
            circle = Math.max(tightest, loiterRadius(npc));
        if (target != null)
            wanted = attackYaw(npc, target, aim, circle);
        else
        {
            wanted = idleYaw(npc, motion, yaw, circle);
            extending = false;
            passing = false;
            orbitTicks = 0;
        }
        float before = yaw;
        yaw = MountMotion.turn(yaw, wanted, planeTurn(turnRate, speed, circle, model != null));
        float wantedBank = Mth.clamp(Mth.wrapDegrees(yaw - before) * BANK_PER_TURN, -MAX_BANK, MAX_BANK);
        float rollRate = ROLL_RATE * authority(model == null ? 1D : model.rollAuthority());
        float pitchRate = PITCH_RATE * authority(model == null ? 1D : model.pitchAuthority());
        bank += Mth.clamp(wantedBank - bank, -rollRate, rollRate);
        double climb = vertical;
        double horizontal = speed;
        if (target != null && attacks(npc, aim))
        {
            // The nose goes no lower than lets the plane fly level along its attack altitude.
            float dive = Mth.clamp(MountMotion.relative(aim, yaw).pitch(), -MAX_CLIMB, MAX_DIVE);
            if (npc.getY() - terrain <= minimum + 1D)
                dive = Math.min(dive, 0F);
            pitch += Mth.clamp(dive - pitch, -pitchRate, pitchRate);
            climb = -speed * Math.sin(Math.toRadians(pitch));
            horizontal = speed * Math.cos(Math.toRadians(pitch));
        }
        else
            pitch += Mth.clamp((float) -Math.toDegrees(Math.atan2(climb, speed)) - pitch, -pitchRate, pitchRate);
        Vec3 forward = Vec3.directionFromRotation(0F, yaw);
        npc.setDeltaMovement(forward.x * horizontal, climb, forward.z * horizontal);
    }

    /**
     * @param idle
     *            whether the plane has nothing to fight, and cruises
     * @param measured
     *            blocks per tick the plane flew along its flight path during the last tick
     * @return blocks per tick the plane flies at this tick: its set cruise or plane speed, the airspeed its model builds up
     *         toward its cruise or top speed, or its own between stall and top speed
     */
    private double flightSpeed(NpcTuning tuning, @Nullable FlightPerformance model, boolean idle, double measured)
    {
        int set = tuning.get(Value.FLIGHT_SPEED);
        int cruise = idle ? tuning.get(Value.CRUISE_SPEED) : 0;
        if (model == null || (set > 0 && cruise == 0))
            return planeSpeed(cruise > 0 ? cruise : set, measured);
        double throttle = idle ? cruiseSpeed(cruise, model) : model.topSpeed();
        airspeed = modelSpeed(airspeed > 0D ? airspeed : measured, model.minimumSpeed(), throttle, model.topSpeed(), pitch);
        return airspeed;
    }

    /**
     * @param tenthsPerSecond
     *            the cruise speed setting; 0 for none
     * @return blocks per tick a plane flying its model cruises at with nothing to fight: its cruise speed setting, or just above
     *         the least speed it flies at; never below that speed nor above its top speed
     */
    public static double cruiseSpeed(int tenthsPerSecond, FlightPerformance model)
    {
        double top = model.topSpeed();
        double cruise = tenthsPerSecond > 0 ? tenthsPerSecond / 200D : Math.max(STALL_SPEED, model.minimumSpeed() * CRUISE_MARGIN);
        return Mth.clamp(cruise, Math.min(model.minimumSpeed(), top), top);
    }

    /**
     * Unlimited roaming follows the local wander direction instead of steering back toward home.
     *
     * @param radius
     *            blocks the plane circles its home at
     */
    static float idleYaw(EntityNPCInterface npc, Vec3 motion, float heading, double radius)
    {
        if (npc.ais.walkingRange == 0 && !npc.ais.returnToStart)
            return motion.horizontalDistanceSqr() > 4.0E-4D ? MountMotion.yaw(motion) : heading;
        return loiterYaw(npc.position(), new Vec3(npc.getStartXPos(), npc.getY(), npc.getStartZPos()), radius, (npc.getId() & 1) == 0, heading);
    }

    /**
     * The heading of a plane with a target, by its tactic: at the target, round it, or straight on past it after a pass. An
     * orbiting plane turns in for a pass now and then. Custom NPCs' own tactics fly at it.
     *
     * @param radius
     *            blocks an orbiting plane circles its target at
     */
    private float attackYaw(EntityNPCInterface npc, LivingEntity target, Vec3 aim, double radius)
    {
        Tactic tactic = NpcTactics.of(npc);
        double horizontal = Math.sqrt(npc.distanceToSqr(target.getX(), npc.getY(), target.getZ()));
        if (tactic == Tactic.ORBIT)
        {
            if (!orbitPass(horizontal, radius))
                return loiterYaw(npc.position(), new Vec3(target.getX(), npc.getY(), target.getZ()), radius, (npc.getId() & 1) == 0, yaw);
        }
        else
        {
            passing = false;
            orbitTicks = 0;
            extending = tactic == Tactic.STRAFING_RUN && extending(extending, horizontal, Math.max(MIN_EXTEND, NpcTactics.engagement(npc).preferred()));
        }
        if (extending || horizontal <= 1D)
            return yaw;
        return MountMotion.yaw(aim);
    }

    /**
     * Counts an orbiting plane's time circling its target, and turns it in on the target for a gun pass every
     * {@link #ORBIT_PASS_INTERVAL} ticks, from far enough out to line up. Once it has passed over the target, or the pass has
     * lasted {@link #MAX_PASS_TICKS}, it carries on straight, its guns silent, until it is back out at its circle.
     *
     * @param horizontal
     *            blocks to the target across the ground
     * @param radius
     *            blocks the plane circles its target at
     * @return whether the plane is making a pass or carrying on after one, rather than circling
     */
    boolean orbitPass(double horizontal, double radius)
    {
        if (extending)
        {
            extending = horizontal < radius;
            return extending;
        }
        if (passing)
        {
            if (horizontal >= PASS_DISTANCE && ++orbitTicks <= MAX_PASS_TICKS)
                return true;
            passing = false;
            orbitTicks = 0;
            extending = horizontal < radius;
            return extending;
        }
        if (++orbitTicks < ORBIT_PASS_INTERVAL || horizontal < radius * 0.75D)
            return false;
        passing = true;
        orbitTicks = 0;
        return true;
    }

    /**
     * @param aim
     *            the world direction the plane's guns have to fire along
     * @return whether the plane makes an attack run with its guns: high enough to attack, neither carrying on past its target
     *         nor circling it outside a pass nor bombing it, and headed close enough at it to put its nose on it
     */
    private boolean attacks(EntityNPCInterface npc, Vec3 aim)
    {
        boolean circling = NpcTactics.of(npc) == Tactic.ORBIT && !passing;
        return attackAllowed && !extending && !circling && !NpcVehicleState.of(npc).isBombing() && Math.abs(MountMotion.relative(aim, yaw).yaw()) <= DIVE_CONE;
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

    /** @return blocks a plane is told to circle its home at, before its turn widens the circle */
    private static double loiterRadius(EntityNPCInterface npc)
    {
        return Math.max(MIN_LOITER_RADIUS, npc.ais.walkingRange);
    }

    /** @return blocks an orbiting plane is told to circle its target at, before its turn widens the circle */
    private static double orbitRadius(EntityNPCInterface npc)
    {
        return Math.max(MIN_LOITER_RADIUS, NpcTactics.engagement(npc).preferred());
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
        return climb(vertical, error, type, idle, type == Flight.PLANE ? PLANE_CLIMB : HELICOPTER_CLIMB);
    }

    /**
     * @param maxClimb
     *            blocks per tick the aircraft climbs at most
     */
    static double climb(double vertical, double error, Flight type, boolean idle, double maxClimb)
    {
        boolean plane = type == Flight.PLANE;
        if (error > 0.5D)
            return Math.max(vertical, Math.min(maxClimb, 0.06D + error * 0.035D));
        if (error < -5D)
            return Math.min(vertical, Math.max(plane ? -0.12D : -0.16D, error * 0.025D));
        return !plane && idle ? vertical * 0.55D : vertical;
    }

    /** @return blocks per tick a plane flies at: its set speed, or its own between stall and top speed */
    static double planeSpeed(int tenthsPerSecond, double current)
    {
        return tenthsPerSecond > 0 ? tenthsPerSecond / 200D : Mth.clamp(current, STALL_SPEED, TOP_SPEED);
    }

    /**
     * @param current
     *            blocks per tick it flies at now
     * @param minimum
     *            least it flies at, below which its wing no longer carries it
     * @param throttle
     *            speed it is throttled to fly level at: its top speed at full throttle, its cruise speed when idle
     * @param top
     *            fastest it flies level at full throttle
     * @param pitch
     *            degrees its nose points below the horizon, negative above it
     * @return blocks per tick a plane flying its model flies at next: building up or slowing down to the speed it is
     *         throttled to, faster in a dive and slower in a climb, never below the least it flies at nor above its top speed
     */
    static double modelSpeed(double current, double minimum, double throttle, double top, float pitch)
    {
        double next = current + (throttle - current) * THRUST_RESPONSE + PATH_GRAVITY * Math.sin(Math.toRadians(pitch));
        return Mth.clamp(next, Math.min(minimum, top), top);
    }

    /**
     * @return degrees per tick a plane turns: what flying its circle at its speed takes, with some turn to spare to stay on it,
     *         so it never spins in place
     */
    static float planeTurn(double speed, double radius)
    {
        return (float) Mth.clamp(Math.toDegrees(speed / Math.max(1D, radius)) * CIRCLE_MARGIN, MIN_PLANE_TURN, MAX_PLANE_TURN);
    }

    /**
     * @param degreesPerSecond
     *            the turn rate setting; 0 for none
     * @param modelled
     *            whether the plane flies as its model, and turns as tightly as its steepest bank allows
     * @return degrees per tick a plane turns at most at its speed
     */
    static float maxTurn(int degreesPerSecond, double speed, boolean modelled)
    {
        if (degreesPerSecond > 0)
            return degreesPerSecond / 20F;
        return modelled ? coordinatedTurn(speed) : MAX_PLANE_TURN;
    }

    /** @return blocks across from its centre the tightest circle is that a plane flies turning so many degrees a tick */
    static double turnRadius(double speed, float degreesPerTick)
    {
        return speed / Math.toRadians(Math.max(MIN_PLANE_TURN, degreesPerTick));
    }

    /**
     * @param degreesPerSecond
     *            the turn rate setting; 0 for none
     * @param modelled
     *            whether the plane flies as its model, and turns as tightly as its steepest bank allows
     * @return degrees per tick a plane turns
     */
    static float planeTurn(int degreesPerSecond, double speed, double radius, boolean modelled)
    {
        if (degreesPerSecond > 0)
            return degreesPerSecond / 20F;
        return modelled ? coordinatedTurn(speed) : planeTurn(speed, radius);
    }

    /** @return degrees per tick a plane turns banked at its steepest without losing height: the slower, the tighter */
    static float coordinatedTurn(double speed)
    {
        double rate = Math.toDegrees(PATH_GRAVITY * Math.tan(Math.toRadians(MAX_BANK)) / Math.max(1.0E-3D, speed));
        return (float) Mth.clamp(rate, 0.25D, MAX_COORDINATED_TURN);
    }

    /** @return the share a model's control rate scales a default rate by, within reasonable bounds */
    static float authority(double modelRate)
    {
        return (float) Mth.clamp(modelRate, MIN_AUTHORITY, MAX_AUTHORITY);
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
