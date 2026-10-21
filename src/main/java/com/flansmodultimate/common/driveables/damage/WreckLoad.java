package com.flansmodultimate.common.driveables.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * What a machine had aboard when it was destroyed, as far as its death and its wreck show it: how much of its
 * ammunition and how much of its fuel was left. A tank with an empty rack does not cook off, and an aircraft with dry
 * tanks smoulders rather than blazes.
 *
 * @param ammunition
 *            share of a full ammunition load still aboard, from 0 to 1
 * @param fuel
 *            share of its tank still full, from 0 to 1
 */
public record WreckLoad(float ammunition, float fuel)
{
    /** What is assumed of a machine whose load is not known, such as an NPC drawn as a vehicle. */
    public static final WreckLoad UNKNOWN = new WreckLoad(0.6F, 0.6F);
    /** Rounds aboard that count as a full load for the look of a cook-off. */
    public static final int FULL_ROUNDS = 20;
    private static final String NBT_AMMUNITION = "wreck_ammunition";
    private static final String NBT_FUEL = "wreck_fuel";
    private static final int STEPS = 100;

    public WreckLoad
    {
        ammunition = share(ammunition);
        fuel = share(fuel);
    }

    private static float share(float value)
    {
        return Float.isFinite(value) ? Mth.clamp(value, 0F, 1F) : 0F;
    }

    /** @return the load of a machine carrying this many rounds, with this much of its tank left */
    public static WreckLoad of(int rounds, float fuelShare)
    {
        return new WreckLoad(rounds / (float) FULL_ROUNDS, fuelShare);
    }

    /** @return the load packed into one number for the entity's synchronised data */
    public int pack()
    {
        return Math.round(ammunition * STEPS) | Math.round(fuel * STEPS) << 8;
    }

    public static WreckLoad unpack(int packed)
    {
        return new WreckLoad((packed & 0xFF) / (float) STEPS, (packed >> 8 & 0xFF) / (float) STEPS);
    }

    public void save(CompoundTag tag)
    {
        tag.putFloat(NBT_AMMUNITION, ammunition);
        tag.putFloat(NBT_FUEL, fuel);
    }

    /** @return the load saved with a wreck, or the unknown load for a wreck saved before loads were */
    public static WreckLoad load(CompoundTag tag)
    {
        return tag.contains(NBT_AMMUNITION) ? new WreckLoad(tag.getFloat(NBT_AMMUNITION), tag.getFloat(NBT_FUEL)) : UNKNOWN;
    }

    /** @return how hard the wreck's fire burns for the fuel it had, from a smoulder to a blaze */
    public float fireStrength()
    {
        return 0.35F + 0.65F * fuel;
    }

    /** @return whether there is enough ammunition aboard to cook off */
    public boolean cooksOff()
    {
        return ammunition >= 0.05F;
    }
}
