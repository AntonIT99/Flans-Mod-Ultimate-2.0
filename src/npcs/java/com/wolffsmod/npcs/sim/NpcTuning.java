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
        /** Ticks between searches of the surroundings up to 32 blocks. */
        SEARCH_NEAR_INTERVAL(10, 1, 200),
        /** Ticks between searches of the farthest band; the bands in between scale from the near interval to this. */
        SEARCH_FAR_INTERVAL(60, 1, 600),
        /** Ticks between path searches while pursuing a moving target. */
        PATH_RETRY_INTERVAL(10, 1, 100),
        /** Blocks of detour a path search may take beyond the distance to its goal. */
        PATH_MARGIN(24, 8, 128),
        /** Distance to the nearest player, in blocks, within which an NPC told to stay active keeps simulating. */
        ACTIVE_RANGE(128, 32, 512);

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
