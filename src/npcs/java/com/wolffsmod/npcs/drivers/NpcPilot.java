package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.AircraftMode;
import com.flansmodultimate.api.DriveControl;
import com.flansmodultimate.api.DriveableAttitude;
import com.flansmodultimate.api.FlansEntityTypes;
import com.wolffsmod.npcs.sim.MountMotion;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import lombok.AccessLevel;
import lombok.Getter;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Flies the aircraft an NPC sits at the controls of, wherever the NPC would walk. A plane takes off along its heading,
 * climbs to a cruising height above the terrain ahead, banks to turn, makes strafing runs on the NPC's target and circles
 * where it has nowhere further to go; parked with nowhere to go, it stays parked. A rotorcraft lifts off on its collective,
 * tilts to fly to its goal, faces the NPC's target and hovers, and lands once it has stayed idle for a while. Either
 * pulls up whenever the ground comes too close. One per NPC, kept by its {@link NpcDriver}. Server thread.
 */
final class NpcPilot
{
    /** Blocks above the terrain below which an aircraft in the air pulls up, wings level, whatever else it wants. */
    private static final double FLOOR = 7D;
    private static final int TICKS_PER_SECOND = 20;
    /** Ticks ahead the ground is looked for, at the aircraft's speed and down its path, and the sink rate projected. */
    private static final int[] TERRAIN_LOOKAHEAD = {0, 20, 40, 60};
    private static final int SINK_LOOKAHEAD = 30;
    /** Degrees of nose-up a plane rolls for take-off with. */
    private static final float TAKEOFF_PITCH = 10F;
    /** Degrees of nose-up with which an aircraft pulls away from the ground. */
    private static final float PULL_UP_PITCH = 15F;
    /** Steepest climb and dive a plane flies to change its height, in degrees. */
    private static final float MAX_CLIMB = 15F;
    private static final float MAX_DESCENT = 12F;
    /** Degrees of nose-up per block below the height wanted, and down per block a tick of climb. */
    private static final float HEIGHT_GAIN = 0.5F;
    private static final float CLIMB_DAMPING = 20F;
    /** Steepest bank a plane turns in, and degrees of bank per degree off its heading. */
    private static final float MAX_BANK = 40F;
    private static final float BANK_PER_DEGREE = 1.2F;
    /** Speed, in blocks a tick, below which a plane in the air lowers its nose to keep flying. */
    private static final double STALL_GUARD = 0.35D;
    /** Horizontal blocks from its target within which a plane dives on it, and within which it breaks off. */
    private static final double DIVE_DISTANCE = 160D;
    private static final double BREAK_DISTANCE = 24D;
    /** Blocks above the terrain below which a diving plane breaks off. */
    private static final double BREAK_HEIGHT = 10D;
    /** Ticks a plane that broke off flies on before it turns back, and blocks away it must be by then. */
    private static final int EXTEND_TICKS = 60;
    private static final double REENGAGE_DISTANCE = 80D;
    /** Steepest a plane dives on its target, and climbs at it, in degrees. */
    private static final float MAX_DIVE = 40F;
    private static final float MAX_ATTACK_CLIMB = 15F;
    /** Degrees off its goal beyond which a plane on the ground turns before it rolls for take-off, and the throttle it taxis at. */
    private static final float TAXI_ANGLE = 30F;
    private static final float TAXI_THROTTLE = 0.35F;

    /** Fastest a rotorcraft flies to its goal, in blocks a tick, and how much faster per block it is still away. */
    private static final double ROTOR_SPEED = 0.5D;
    private static final double ROTOR_SPEED_PER_BLOCK = 0.03D;
    /** Blocks from its goal at which a rotorcraft has arrived, and degrees off it within which it flies on rather than turns. */
    private static final double ROTOR_ARRIVAL = 4D;
    private static final float ROTOR_TURN_ANGLE = 30F;
    /** Degrees of tilt per block a tick of speed off what is wanted, and the most it tilts forward, back and sideways. */
    private static final float TILT_PER_SPEED = 40F;
    private static final float MAX_FORWARD_TILT = 15F;
    private static final float MAX_BACK_TILT = 12F;
    private static final float MAX_SIDE_TILT = 10F;
    /** Climb wanted per block of height off the hover height, and the fastest climb and descent, in blocks a tick. */
    private static final double CLIMB_PER_BLOCK = 0.04D;
    private static final double MAX_ROTOR_CLIMB = 0.25D;
    private static final double MAX_ROTOR_DESCENT = 0.2D;
    private static final double LANDING_DESCENT = 0.1D;
    /** Collective that holds a hover, and how much more per block a tick of climb missing. */
    private static final float HOVER_COLLECTIVE = 0.5F;
    private static final float COLLECTIVE_PER_CLIMB = 3F;
    /** A plane glides in at this share of the speed it cruises at, and never slower than the least approach speed. */
    private static final double APPROACH_SHARE = 0.7D;
    private static final double MIN_APPROACH_SPEED = 0.45D;
    /** Share of each tick's speed taken into the cruising speed a plane learns. */
    private static final double CRUISE_LEARNING = 0.02D;

    private final FlightControl control = new FlightControl();
    /** How the NPC wants its aircraft flown, as read this tick. */
    private Settings settings = Settings.DEFAULTS;
    private final PlaneLanding landing = new PlaneLanding();
    /** The speed a plane cruises at, learnt as it cruises; 0 until it has. */
    private double cruiseSpeed;
    /** Where the aircraft was last tick. */
    @Nullable
    private Vec3 lastPosition;
    /** The heading a plane rolls along for take-off, set as the roll begins. */
    @Nullable
    private Float runway;
    /** Whether a plane is flying on past its target after a strafing run, until when, and on which heading. */
    private boolean extending;
    private long extendUntil;
    private float extendHeading;
    /** Ticks a rotorcraft has hovered with nothing to do. */
    private int idleTicks;
    /** Whether the aircraft, still on the ground, is meant to be moving: taxiing, rolling for take-off or lifting off. */
    @Getter(AccessLevel.PACKAGE)
    private boolean rolling;
    /** What the pilot did last tick and the point it flew for, for the movement debug. */
    @Getter(AccessLevel.PACKAGE)
    private Phase phase = Phase.PARKED;
    @Getter(AccessLevel.PACKAGE) @Nullable
    private Vec3 point;
    @Nullable
    private FlightControl.State lastState;
    @Nullable
    private FlightControl.Command lastCommand;
    private double lastHeight;
    private double lastSpeed;

    /** What a pilot does. */
    enum Phase
    {
        PARKED, TAXI, TAKEOFF, CRUISE, CIRCLE, ATTACK, DIVE, EXTEND, PULL_UP, APPROACH, FINAL, FLARE, ROLLOUT, TRAVEL, HOVER, LANDING
    }

    /**
     * How an NPC wants its aircraft flown, from its Wolff's Mod settings.
     *
     * @param cruiseHeight
     *            blocks above the terrain a plane cruises and circles at
     * @param attackHeight
     *            blocks above the terrain a plane makes for its target at, before it dives on it
     * @param circleRadius
     *            blocks round where it has nowhere further to go that a plane circles at
     * @param planeIdle
     *            ticks a plane circles with nothing to do before it lands; 0 never
     * @param hoverHeight
     *            blocks above the terrain a rotorcraft flies and hovers at
     * @param rotorIdle
     *            ticks a rotorcraft hovers with nothing to do before it lands; 0 never
     * @param lowFuel
     *            share of its tank under which an aircraft heads home to land, and stays on the ground
     */
    record Settings(double cruiseHeight, double attackHeight, double circleRadius, int planeIdle, double hoverHeight, int rotorIdle, double lowFuel)
    {
        static final Settings DEFAULTS = new Settings(Value.CRUISE_HEIGHT.fallback(), Value.ATTACK_HEIGHT.fallback(), Value.CIRCLE_RADIUS.fallback(),
            Value.PLANE_LANDING_DELAY.fallback() * TICKS_PER_SECOND, Value.HOVER_HEIGHT.fallback(), Value.HELICOPTER_LANDING_DELAY.fallback() * TICKS_PER_SECOND, Value.LOW_FUEL.fallback() / 100D);

        static Settings of(EntityNPCInterface npc)
        {
            NpcTuning tuning = NpcTuning.of(npc);
            return new Settings(tuning.get(Value.CRUISE_HEIGHT), tuning.get(Value.ATTACK_HEIGHT), tuning.get(Value.CIRCLE_RADIUS), tuning.get(Value.PLANE_LANDING_DELAY) * TICKS_PER_SECOND,
                tuning.get(Value.HOVER_HEIGHT), tuning.get(Value.HELICOPTER_LANDING_DELAY) * TICKS_PER_SECOND, tuning.get(Value.LOW_FUEL) / 100D);
        }

        /** Whether an aircraft with so much of its tank left is short of fuel. */
        boolean shortOfFuel(double fuel)
        {
            return fuel < lowFuel;
        }

        /** Whether an aircraft idle for so many ticks lands, after the ticks given; never after 0. */
        static boolean landsAfter(int idleTicks, int delay)
        {
            return delay > 0 && idleTicks > delay;
        }
    }

    /** Where a plane is to head, and the pitch it is to fly at. */
    private record Course(float heading, float pitch)
    {}

    /** How far a rotorcraft is to pitch and bank. */
    private record Tilt(float pitch, float bank)
    {}

    /**
     * What the pilot sees this tick.
     *
     * @param lowOnFuel
     *            whether it is short of fuel, as the NPC's settings say
     * @param resting
     *            blocks its origin is above level ground when it stands on its wheels
     */
    private record Flight(DriveableAttitude attitude, float throttle, boolean grounded, Vec3 position, Vec3 velocity, double ground, double terrain, boolean lowOnFuel, double resting)
    {
        double height()
        {
            return position.y - ground;
        }
    }

    /** Forgets the flight, once the NPC no longer flies. */
    void reset()
    {
        control.reset();
        lastPosition = null;
        runway = null;
        extending = false;
        idleTicks = 0;
        rolling = false;
        landing.abort();
        cruiseSpeed = 0D;
        phase = Phase.PARKED;
        point = null;
        lastState = null;
        lastCommand = null;
        settings = Settings.DEFAULTS;
    }

    /** @return what the pilot wanted of the aircraft last tick against what it got, for the movement debug log */
    String describe()
    {
        FlightControl.State state = lastState;
        FlightControl.Command command = lastCommand;
        if (state == null || command == null)
            return "";
        return String.format(Locale.ROOT, "pitch=%.0f/%.0f bank=%.0f/%.0f heading=%.0f/%s throttle=%.2f/%.2f height=%.1f speed=%.2f", state.pitch(), command.pitch(), state.bank(), command.bank(),
            state.heading(), command.heading() == null ? "-" : String.format(Locale.ROOT, "%.0f", command.heading()), state.throttle(), command.throttle(), lastHeight, lastSpeed);
    }

    /**
     * Flies for one tick.
     *
     * @param goal
     *            where the NPC would walk, or null when it would stay
     * @return the controls to hold, or none when the aircraft cannot be read
     */
    Set<DriveControl> fly(EntityNPCInterface npc, Entity aircraft, AircraftMode mode, @Nullable Vec3 goal)
    {
        DriveableAttitude attitude = FlansEntityTypes.getAttitude(aircraft).orElse(null);
        if (attitude == null)
            return EnumSet.noneOf(DriveControl.class);
        settings = Settings.of(npc);
        Vec3 position = aircraft.position();
        Vec3 velocity = lastPosition == null ? Vec3.ZERO : position.subtract(lastPosition);
        lastPosition = position;
        Level level = aircraft.level();
        double ground = groundAt(level, position.x, position.z, position.y);
        double terrain = ground;
        for (int ticks : TERRAIN_LOOKAHEAD)
            terrain = Math.max(terrain, groundAt(level, position.x + velocity.x * ticks, position.z + velocity.z * ticks, ground));
        double resting = FlansEntityTypes.getDriveableType(aircraft).map(type -> (double) type.getRestingHeight()).orElse(0D);
        Flight flight = new Flight(attitude, (float) FlansEntityTypes.getThrottle(aircraft).orElse(0D), FlansEntityTypes.isGrounded(aircraft), position, velocity, ground, terrain,
            settings.shortOfFuel(FlansEntityTypes.getFuelShare(aircraft).orElse(1D)), resting);
        Vec3 home = new Vec3(npc.getStartXPos(), npc.getStartYPos(), npc.getStartZPos());
        // Short of fuel, an aircraft gives up its target and its goal and heads home to land.
        LivingEntity target = !flight.lowOnFuel() && npc.getTarget() != null && npc.getTarget().isAlive() ? npc.getTarget() : null;
        Vec3 destination = flight.lowOnFuel() ? home : goal;
        point = target != null ? target.getBoundingBox().getCenter() : destination;
        FlightControl.Command command = mode == AircraftMode.ROTORCRAFT ? rotor(flight, destination, target) : wings(level, flight, destination, target, home);
        rolling = flight.grounded() && command.throttle() > 0F;
        FlightControl.State state = new FlightControl.State(attitude.pitch(), attitude.bank(), attitude.heading(), flight.throttle());
        lastState = state;
        lastCommand = command;
        lastHeight = flight.height();
        lastSpeed = velocity.length();
        return control.hold(state, command);
    }

    // Planes

    private FlightControl.Command wings(Level level, Flight flight, @Nullable Vec3 goal, @Nullable LivingEntity target, Vec3 home)
    {
        Vec3 place = goal != null ? goal : home;
        // With nothing to do for a while, or short of fuel, a plane lands, where it is going or at home.
        if (wantsDown(flight, goal, target) && (!flight.grounded() || landing.active()))
            return land(level, flight, place);
        if (flight.grounded())
        {
            extending = false;
            return ground(flight, flight.lowOnFuel() ? null : goal, target);
        }
        return airborne(level, flight, place, target);
    }

    /** @return whether a plane is to land: it is short of fuel, or has had nothing to do for a while; otherwise the landing is given up */
    private boolean wantsDown(Flight flight, @Nullable Vec3 goal, @Nullable LivingEntity target)
    {
        boolean idle = target == null && (goal == null || horizontal(flight.position(), goal) <= settings.circleRadius());
        idleTicks = idle ? idleTicks + 1 : 0;
        boolean down = flight.lowOnFuel() || Settings.landsAfter(idleTicks, settings.planeIdle());
        if (!down)
            landing.abort();
        return down;
    }

    /** Flies a plane in the air: on its target, or to a place and round it, keeping clear of the ground and above a stall. */
    private FlightControl.Command airborne(Level level, Flight flight, Vec3 place, @Nullable LivingEntity target)
    {
        runway = null;
        float heading = flight.attitude().heading();
        Course course = target != null ? attack(level, flight, target) : travel(flight, place);
        if (tooLow(flight))
        {
            phase = Phase.PULL_UP;
            return new FlightControl.Command(PULL_UP_PITCH, 0F, heading, 1F);
        }
        float pitch = course.pitch();
        if (flight.velocity().length() < STALL_GUARD && flight.height() > settings.cruiseHeight() * 0.5D)
            pitch = Math.min(pitch, -5F);
        float bank = Mth.clamp(Mth.wrapDegrees(course.heading() - heading) * BANK_PER_DEGREE, -MAX_BANK, MAX_BANK);
        if (phase == Phase.CRUISE || phase == Phase.CIRCLE)
            cruiseSpeed = cruiseSpeed <= 0D ? flight.velocity().length() : Mth.lerp(CRUISE_LEARNING, cruiseSpeed, flight.velocity().length());
        return new FlightControl.Command(pitch, bank, course.heading(), 1F);
    }

    /** Brings the plane down at the site, unless the ground ahead rises into its way, when it climbs away to try again. */
    private FlightControl.Command land(Level level, Flight flight, Vec3 site)
    {
        double siteGround = groundAt(level, site.x, site.z, site.y);
        if (tooLow(flight) && flight.terrain() > siteGround + 3D)
        {
            landing.abort();
            phase = Phase.PULL_UP;
            return new FlightControl.Command(PULL_UP_PITCH, 0F, flight.attitude().heading(), 1F);
        }
        double approachSpeed = Math.max(MIN_APPROACH_SPEED, cruiseSpeed * APPROACH_SHARE);
        PlaneLanding.Step step = landing
            .land(new PlaneLanding.View(flight.position(), flight.velocity(), flight.attitude().heading(), flight.grounded(), flight.height(), siteGround, flight.resting(), approachSpeed), site);
        phase = switch (step.stage())
        {
            case APPROACH -> Phase.APPROACH;
            case FINAL -> Phase.FINAL;
            case FLARE -> Phase.FLARE;
            case ROLLOUT -> Phase.ROLLOUT;
            case LANDED -> Phase.PARKED;
        };
        point = site;
        return new FlightControl.Command(step.pitch(), step.bank(), step.heading(), step.throttle(), step.brake());
    }

    /** On the ground: parked with nowhere further to go, otherwise taxiing round toward its goal and rolling for take-off. */
    private FlightControl.Command ground(Flight flight, @Nullable Vec3 goal, @Nullable LivingEntity target)
    {
        float heading = flight.attitude().heading();
        Vec3 destination = target != null ? target.position() : goal;
        if (destination == null || target == null && horizontal(flight.position(), destination) <= settings.circleRadius())
        {
            runway = null;
            phase = Phase.PARKED;
            return new FlightControl.Command(0F, 0F, heading, 0F);
        }
        float off = Mth.wrapDegrees(bearing(flight.position(), destination) - heading);
        if (runway == null && Math.abs(off) > TAXI_ANGLE && flight.velocity().horizontalDistance() < STALL_GUARD)
        {
            phase = Phase.TAXI;
            return new FlightControl.Command(0F, 0F, heading + off, TAXI_THROTTLE);
        }
        if (runway == null)
            runway = heading;
        phase = Phase.TAKEOFF;
        return new FlightControl.Command(TAKEOFF_PITCH, 0F, runway, 1F);
    }

    /**
     * A strafing run: the plane flies at its target, dives on it from close enough, breaks off short of it or of the
     * ground, flies on for a while and comes round for another pass.
     */
    private Course attack(Level level, Flight flight, LivingEntity target)
    {
        Vec3 aim = target.getBoundingBox().getCenter();
        double distance = horizontal(flight.position(), aim);
        long now = level.getGameTime();
        if (extending && now >= extendUntil && distance > REENGAGE_DISTANCE)
            extending = false;
        boolean diving = distance < DIVE_DISTANCE;
        if (!extending && (distance < BREAK_DISTANCE || diving && flight.height() < BREAK_HEIGHT && flight.velocity().y < 0D))
        {
            extending = true;
            extendUntil = now + EXTEND_TICKS;
            extendHeading = flight.attitude().heading();
        }
        if (extending)
        {
            phase = Phase.EXTEND;
            return new Course(extendHeading, holdHeight(flight, flight.terrain() + settings.cruiseHeight()));
        }
        if (!diving)
        {
            phase = Phase.ATTACK;
            return new Course(bearing(flight.position(), aim), holdHeight(flight, flight.terrain() + settings.attackHeight()));
        }
        phase = Phase.DIVE;
        float elevation = (float) Math.toDegrees(Math.atan2(aim.y - flight.position().y, Math.max(1.0E-3D, distance)));
        return new Course(bearing(flight.position(), aim), Mth.clamp(elevation, -MAX_DIVE, MAX_ATTACK_CLIMB));
    }

    /** Flies to a place at cruising height and circles there. */
    private Course travel(Flight flight, Vec3 anchor)
    {
        extending = false;
        boolean away = horizontal(flight.position(), anchor) > settings.circleRadius();
        phase = away ? Phase.CRUISE : Phase.CIRCLE;
        point = anchor;
        float heading = away ? bearing(flight.position(), anchor) : circle(flight.position(), anchor, settings.circleRadius());
        return new Course(heading, holdHeight(flight, flight.terrain() + settings.cruiseHeight()));
    }

    /** @return the pitch that brings a plane to a height and holds it there */
    private static float holdHeight(Flight flight, double height)
    {
        return holdHeight(flight.position().y, flight.velocity().y, height);
    }

    /**
     * @param y
     *            the height the plane is at
     * @param climb
     *            blocks it climbs per tick
     * @return the pitch that brings a plane to a height and holds it there
     */
    static float holdHeight(double y, double climb, double height)
    {
        return Mth.clamp((float) (height - y) * HEIGHT_GAIN - (float) climb * CLIMB_DAMPING, -MAX_DESCENT, MAX_CLIMB);
    }

    /**
     * @return the heading that circles a point: straight round it at the radius, turning in from outside and out from
     *         inside
     */
    static float circle(Vec3 position, Vec3 centre, double radius)
    {
        double distance = horizontal(position, centre);
        float inward = (float) Mth.clamp((distance - radius) * 2D, -60D, 60D);
        return Mth.wrapDegrees(bearing(position, centre) + 90F - inward);
    }

    // Rotorcraft

    private FlightControl.Command rotor(Flight flight, @Nullable Vec3 goal, @Nullable LivingEntity target)
    {
        float heading = flight.attitude().heading();
        double distance = goal == null ? 0D : horizontal(flight.position(), goal);
        boolean travelling = goal != null && distance > ROTOR_ARRIVAL;
        idleTicks = travelling || target != null ? 0 : idleTicks + 1;
        phase = rotorPhase(flight, travelling, target != null);
        if (phase == Phase.PARKED)
            return new FlightControl.Command(0F, 0F, heading, 0F);
        boolean settling = phase == Phase.LANDING;
        // It flies nose first to its goal, and faces its target once it is there; either way it is not landing.
        if (travelling)
        {
            float wantedHeading = bearing(flight.position(), goal);
            boolean onCourse = Math.abs(Mth.wrapDegrees(wantedHeading - heading)) < ROTOR_TURN_ANGLE;
            Tilt tilt = tilt(flight, onCourse ? Math.min(ROTOR_SPEED, distance * ROTOR_SPEED_PER_BLOCK) : 0D);
            return new FlightControl.Command(tilt.pitch(), tilt.bank(), wantedHeading, collective(flight, false));
        }
        Tilt tilt = tilt(flight, 0D);
        if (target == null)
            return new FlightControl.Command(tilt.pitch(), tilt.bank(), heading, collective(flight, settling));
        // Hovering on its target, it points its nose at it for its guns.
        Vec3 line = target.getBoundingBox().getCenter().subtract(flight.position());
        float pitch = Mth.clamp((float) Math.toDegrees(Math.atan2(line.y, Math.max(1.0E-3D, line.horizontalDistance()))), -MAX_FORWARD_TILT, 5F);
        return new FlightControl.Command(pitch, tilt.bank(), bearing(flight.position(), target.position()), collective(flight, false));
    }

    /**
     * @return what a rotorcraft does: stays parked on the ground with nothing to do or short of fuel, lands once it has hovered
     *         idle a while or is home short of fuel, or else travels or hovers
     */
    private Phase rotorPhase(Flight flight, boolean travelling, boolean fighting)
    {
        if (flight.grounded() && (!travelling && !fighting || flight.lowOnFuel()))
            return Phase.PARKED;
        if (Settings.landsAfter(idleTicks, settings.rotorIdle()) || flight.lowOnFuel() && !travelling)
            return Phase.LANDING;
        return travelling ? Phase.TRAVEL : Phase.HOVER;
    }

    /**
     * @return the tilt that brings a rotorcraft to a speed along its nose and holds it from drifting sideways: nose down to
     *         speed up and up to slow down, rolled against a drift
     */
    private static Tilt tilt(Flight flight, double wantedSpeed)
    {
        Vec3 front = horizontalUnit(flight.attitude().front());
        Vec3 right = new Vec3(-front.z, 0D, front.x);
        double ahead = flight.velocity().dot(front);
        double aside = flight.velocity().dot(right);
        return new Tilt(Mth.clamp((float) (ahead - wantedSpeed) * TILT_PER_SPEED, -MAX_FORWARD_TILT, MAX_BACK_TILT), Mth.clamp((float) -aside * TILT_PER_SPEED, -MAX_SIDE_TILT, MAX_SIDE_TILT));
    }

    /** @return the collective that climbs a rotorcraft to its hover height and holds it there, or lets it down to land */
    private float collective(Flight flight, boolean settling)
    {
        double climb = settling ? -LANDING_DESCENT : Mth.clamp((flight.terrain() + settings.hoverHeight() - flight.position().y) * CLIMB_PER_BLOCK, -MAX_ROTOR_DESCENT, MAX_ROTOR_CLIMB);
        if (tooLow(flight) && !settling)
        {
            phase = Phase.PULL_UP;
            climb = MAX_ROTOR_CLIMB;
        }
        return HOVER_COLLECTIVE + Mth.clamp((float) (climb - flight.velocity().y) * COLLECTIVE_PER_CLIMB, -HOVER_COLLECTIVE, 1F - HOVER_COLLECTIVE);
    }

    // Terrain

    /** Whether the aircraft is in the air and about to come too close to the ground. */
    private static boolean tooLow(Flight flight)
    {
        if (flight.grounded())
            return false;
        double sinking = Math.min(0D, flight.velocity().y) * SINK_LOOKAHEAD;
        return flight.position().y - flight.terrain() + sinking < FLOOR;
    }

    /** @return the height of the ground at a place, or the fallback where it is not loaded */
    private static double groundAt(Level level, double x, double z, double fallback)
    {
        BlockPos pos = BlockPos.containing(x, 0D, z);
        return level.isLoaded(pos) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) : fallback;
    }

    private static float bearing(Vec3 from, Vec3 to)
    {
        return MountMotion.yaw(to.subtract(from));
    }

    private static Vec3 horizontalUnit(Vec3 direction)
    {
        Vec3 flat = new Vec3(direction.x, 0D, direction.z);
        return flat.lengthSqr() < 1.0E-8D ? new Vec3(0D, 0D, 1D) : flat.normalize();
    }

    private static double horizontal(Vec3 from, Vec3 to)
    {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }
}
