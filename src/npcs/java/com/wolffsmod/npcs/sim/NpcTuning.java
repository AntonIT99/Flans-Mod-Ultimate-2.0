package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;

/**
 * Per-NPC numbers and choices behind the simulation switches: aim speeds, turn rates, terrain limits, line-of-sight
 * limits, search and path intervals. Every value is a whole number within fixed bounds; a value the save does not
 * mention keeps its default, so NPCs saved before a value existed behave as its default describes.
 */
public final class NpcTuning
{
    public static final String NBT_KEY = "WolffsModTuning";

    private final int[] values = new int[Value.values().length];

    /** One tunable number. A value of 0 for a speed or rate means the model decides. */
    public enum Value
    {
        /** Traverse speed in degrees per second; 0 takes the speed the model's seat states. */
        AIM_SPEED_YAW(0, 0, 3600),
        /** Elevation speed in degrees per second; 0 takes the speed the model's seat states. */
        AIM_SPEED_PITCH(0, 0, 3600),
        /** How far, in degrees, the gun may be off its solution and still fire. */
        ALIGN_TOLERANCE(4, 1, 45),
        /** Hull turn rate in degrees per second; 0 takes a rate fitting the model kind. */
        HULL_TURN_RATE(0, 0, 3600),
        /** Share of its speed, in percent, a vehicle keeps while its hull points away from where it wants to go. */
        THROTTLE_FLOOR(15, 0, 100),
        /** Index into {@link Terrain}. */
        TERRAIN(0, 0, Terrain.values().length - 1),
        /** Deepest water, in blocks, a ground profile wades through. */
        MAX_WADE_DEPTH(1, 0, 16),
        /** Shallowest water, in blocks, a watercraft floats in. */
        MIN_WATER_DEPTH(1, 1, 16),
        /** Speed on land in tenths of a block per second; 0 keeps the NPC's walking speed. */
        LAND_SPEED(0, 0, 600),
        /** Speed in water in tenths of a block per second; 0 keeps the default swimming speed. */
        WATER_SPEED(0, 0, 600),
        /** Index into {@link Sight}. */
        SIGHT(0, 0, Sight.values().length - 1),
        /** Transparent blocks the NPC sees through. */
        VISION_LIMIT(8, 0, 64),
        /** Transparent blocks the NPC fires through. */
        FIRE_LIMIT(4, 0, 64),
        /** Blocks beyond its aggro range at which the NPC keeps a target it already has; a tactical manoeuvre adds its own radius. */
        TARGET_MARGIN(0, 0, 256),
        /** Ticks between searches of the surroundings up to 32 blocks; 0 takes the interval set for the whole server. */
        SEARCH_NEAR_INTERVAL(0, 0, 200),
        /** Ticks between searches of the farthest band; the bands in between scale from the near interval to this. */
        SEARCH_FAR_INTERVAL(0, 0, 600),
        /** Ticks between path searches while pursuing a moving target. */
        PATH_RETRY_INTERVAL(10, 1, 100),
        /** Blocks of detour a path search may take beyond the distance to its goal. */
        PATH_MARGIN(24, 8, 128),
        /** Distance to the nearest player, in blocks, within which an NPC told to stay active keeps simulating. */
        ACTIVE_RANGE(128, 32, 512),
        /** Index into {@link DrivePreset}; choosing one fills in the driving values, which stay editable. */
        DRIVE_PRESET(0, 0, DrivePreset.values().length - 1),
        /** Index into {@link DriveType}. */
        DRIVE_TYPE(0, 0, DriveType.values().length - 1),
        /** Share of full throttle gained per second, in percent. */
        DRIVE_ACCELERATION(100, 5, 2000),
        /** Share of full throttle shed per second while braking, in percent. */
        DRIVE_BRAKING(250, 5, 2000),
        /** Share of full throttle shed per second while coasting, in percent. */
        DRIVE_COASTING(60, 1, 2000),
        /** Top speed in reverse as a share of the forward top speed, in percent. */
        DRIVE_REVERSE_SPEED(40, 5, 100),
        /** Smallest turning circle radius in tenths of a block; 0 takes the radius of the vehicle type. */
        TURN_RADIUS(0, 0, 640),
        /** Blocks of path ahead the vehicle steers for and looks for corners in. */
        DRIVE_LOOKAHEAD(4, 1, 32),
        /** Share of its speed, in percent, a vehicle keeps through a right-angle corner. */
        CORNER_SPEED(45, 5, 100),
        /** Blocks from the end of its path at which a vehicle stops; 0 drives all the way. */
        STOP_DISTANCE(0, 0, 32),
        /** Degrees off the hull beyond which a nearby goal is reached in reverse. */
        REVERSE_ANGLE(110, 45, 179),
        /** Blocks within which a goal behind the vehicle is reached in reverse rather than by turning round. */
        REVERSE_DISTANCE(10, 1, 64),
        /** Blocks kept to a vehicle ahead; 0 ignores other vehicles. */
        FOLLOW_DISTANCE(5, 0, 32),
        /** Index into {@link Flight}. */
        FLIGHT_TYPE(0, 0, Flight.values().length - 1),
        /** Blocks above the terrain an aircraft climbs to before it attacks. */
        ATTACK_ALTITUDE(20, 0, 256),
        /** Speed of a plane in tenths of a block per second; 0 keeps it between a stall and a top speed. */
        FLIGHT_SPEED(0, 0, 600),
        /** Blocks below the surface a watercraft runs at; 0 floats. */
        SUBMARINE_DEPTH(0, 0, 64);

        private final int fallback;
        private final int minimum;
        private final int maximum;

        Value(int fallback, int minimum, int maximum)
        {
            this.fallback = fallback;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public int fallback()
        {
            return fallback;
        }

        public int minimum()
        {
            return minimum;
        }

        public int maximum()
        {
            return maximum;
        }

        public int clamp(int value)
        {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }

    /** Where an NPC may travel. */
    public enum Terrain
    {
        /** Custom NPCs decides, as for any NPC. */
        DEFAULT,
        /** Land, wading through water no deeper than the wade depth. */
        GROUND,
        /** Water at least the minimum depth, never land. */
        WATERCRAFT,
        /** Land and water alike. */
        AMPHIBIOUS
    }

    /** How a ground vehicle steers. */
    public enum DriveType
    {
        /** Tracked when the model is a tank, wheeled otherwise. */
        AUTO,
        /** Has to move to turn, on a circle no tighter than its turn radius. */
        WHEELED,
        /** Turns on the spot. */
        TRACKED,
        /** Turns on the spot only while slow, and more slowly than a lighter vehicle. */
        HEAVY_TRACKED
    }

    /** Ready-made driving values. A preset only fills the values in; it does not lock them. */
    public enum DrivePreset
    {
        CUSTOM(DriveType.AUTO, 100, 250, 60, 0, 4, 45, 0), CAR(DriveType.WHEELED, 150, 320, 60, 40, 5, 45, 0), TRUCK(DriveType.WHEELED, 70, 200, 40, 70, 6, 35, 0), BUS(DriveType.WHEELED, 60, 180, 40,
            90, 7, 30,
            0), LIGHT_TANK(DriveType.TRACKED, 90, 260, 80, 0, 4, 55, 1200), HEAVY_TANK(DriveType.HEAVY_TRACKED, 45, 200, 90, 0, 5, 45, 700), APC(DriveType.WHEELED, 90, 260, 60, 55, 5, 40, 0);

        private final DriveType type;
        private final int acceleration;
        private final int braking;
        private final int coasting;
        private final int radius;
        private final int lookahead;
        private final int cornerSpeed;
        private final int turnRate;

        // One row per preset reads better than a builder here.
        @SuppressWarnings("java:S107")
        DrivePreset(DriveType type, int acceleration, int braking, int coasting, int radius, int lookahead, int cornerSpeed, int turnRate)
        {
            this.type = type;
            this.acceleration = acceleration;
            this.braking = braking;
            this.coasting = coasting;
            this.radius = radius;
            this.lookahead = lookahead;
            this.cornerSpeed = cornerSpeed;
            this.turnRate = turnRate;
        }
    }

    /** How a flying NPC flies. */
    public enum Flight
    {
        /** As any flying Custom NPC. */
        NORMAL,
        /** Never stops, turns in circles, climbs before it attacks. */
        PLANE,
        /** May hover, turns its hull toward its target, climbs before it attacks. */
        HELICOPTER
    }

    /** What an NPC sees and fires through. */
    public enum Sight
    {
        /** Only what vanilla line of sight lets through. */
        NORMAL,
        /** Sees through transparent blocks, fires only with a clear line. */
        SEE_THROUGH,
        /** Sees and fires through transparent blocks. */
        SEE_AND_FIRE_THROUGH
    }

    public NpcTuning()
    {
        reset();
    }

    public int get(Value value)
    {
        return values[value.ordinal()];
    }

    public void set(Value value, int number)
    {
        values[value.ordinal()] = value.clamp(number);
    }

    public Terrain terrain()
    {
        return Terrain.values()[get(Value.TERRAIN)];
    }

    public Sight sight()
    {
        return Sight.values()[get(Value.SIGHT)];
    }

    public DriveType driveType()
    {
        return DriveType.values()[get(Value.DRIVE_TYPE)];
    }

    public Flight flight()
    {
        return Flight.values()[get(Value.FLIGHT_TYPE)];
    }

    /** Selects a preset and, unless it is Custom, fills in its driving values. */
    public void applyPreset(DrivePreset preset)
    {
        set(Value.DRIVE_PRESET, preset.ordinal());
        if (preset == DrivePreset.CUSTOM)
            return;
        set(Value.DRIVE_TYPE, preset.type.ordinal());
        set(Value.DRIVE_ACCELERATION, preset.acceleration);
        set(Value.DRIVE_BRAKING, preset.braking);
        set(Value.DRIVE_COASTING, preset.coasting);
        set(Value.TURN_RADIUS, preset.radius);
        set(Value.DRIVE_LOOKAHEAD, preset.lookahead);
        set(Value.CORNER_SPEED, preset.cornerSpeed);
        set(Value.HULL_TURN_RATE, preset.turnRate);
    }

    /** @return a number that changes whenever any value does */
    public int signature()
    {
        return Arrays.hashCode(values);
    }

    public void load(CompoundTag root)
    {
        reset();
        CompoundTag tag = root.getCompound(NBT_KEY);
        for (Value value : Value.values())
        {
            if (tag.contains(value.name(), Tag.TAG_INT))
                set(value, tag.getInt(value.name()));
        }
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        for (Value value : Value.values())
        {
            if (get(value) != value.fallback)
                tag.putInt(value.name(), get(value));
        }
        root.put(NBT_KEY, tag);
    }

    private void reset()
    {
        for (Value value : Value.values())
            values[value.ordinal()] = value.fallback;
    }

    public static NpcTuning of(EntityNPCInterface npc)
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsTuning();
    }
}
