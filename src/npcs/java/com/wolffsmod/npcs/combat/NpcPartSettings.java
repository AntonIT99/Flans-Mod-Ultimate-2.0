package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * What an NPC changes about the part hitboxes of its model: parts it leaves out, and parts whose box, hit points or
 * penetration resistance or armour it replaces. A part the settings do not mention is the model's.
 *
 * <p>
 * Parts are named as in type files, in any letter case. Settings for a part the selected model does not have are kept
 * and ignored, so that picking another model and coming back loses nothing.
 * </p>
 */
public final class NpcPartSettings
{
    public static final String NBT_KEY = "WolffsModParts";
    /** Blocks a box may reach from the model origin, and may measure along one axis. */
    public static final float MAX_EXTENT = 512F;
    public static final float MAX_HEALTH = 10_000_000F;
    public static final float MAX_RESISTANCE = 100_000F;
    public static final float MAX_ARMOR_MM = 100_000F;
    private static final String ENABLED_KEY = "Enabled";
    private static final String HEALTH_KEY = "Health";
    private static final String RESISTANCE_KEY = "Resistance";
    private static final String ARMOR_KEY = "ArmorMm";
    private static final String HEAT_ARMOR_KEY = "ArmorVsHeatMm";
    private static final String[] BOUNDS_KEYS = {"MinX", "MinY", "MinZ", "MaxX", "MaxY", "MaxZ"};
    private static final Entry MODEL = new Entry(true, null, null);

    private final Map<String, Entry> entries = new TreeMap<>();

    /**
     * @param enabled
     *            whether the part is hit and holds hit points at all
     * @param box
     *            the NPC's own box and hit points for the part, or null for the model's
     * @param armor
     *            the NPC's own uniform armour for the part, or null for the model's plates
     */
    public record Entry(boolean enabled, @Nullable PartBox box, @Nullable PartArmor armor)
    {}

    public Entry get(String part)
    {
        return entries.getOrDefault(key(part), MODEL);
    }

    public void setEnabled(String part, boolean enabled)
    {
        Entry entry = get(part);
        put(part, new Entry(enabled, entry.box(), entry.armor()));
    }

    /**
     * @param box
     *            the NPC's own box for the part, brought within the limits, or null to return to the model's
     */
    public void setBox(String part, @Nullable PartBox box)
    {
        Entry entry = get(part);
        put(part, new Entry(entry.enabled(), box == null ? null : limit(box), entry.armor()));
    }

    /** Sets uniform kinetic and HEAT armour in millimetres, or null to restore the model's; independent of the box. */
    public void setArmor(String part, @Nullable PartArmor armor)
    {
        Entry entry = get(part);
        PartArmor limited = armor == null ? null : new PartArmor(Math.min(MAX_ARMOR_MM, armor.thicknessMm()), Math.min(MAX_ARMOR_MM, armor.heatThicknessMm()));
        put(part, new Entry(entry.enabled(), entry.box(), limited));
    }

    private void put(String part, Entry entry)
    {
        if (MODEL.equals(entry))
            entries.remove(key(part));
        else
            entries.put(key(part), entry);
    }

    /** Returns every part to the model's. */
    public void clear()
    {
        entries.clear();
    }

    public boolean isEmpty()
    {
        return entries.isEmpty();
    }

    /** @return a number that changes whenever a setting does */
    public int signature()
    {
        return entries.hashCode();
    }

    /** Makes the body what the model defines with this NPC's changes on top. */
    public void apply(IPartBody body)
    {
        for (PartState part : body.getParts())
        {
            Entry entry = get(part.name());
            body.setEnabled(part.name(), entry.enabled());
            body.setBox(part.name(), entry.box());
            body.setArmor(part.name(), entry.armor());
        }
    }

    private static String key(String part)
    {
        return part == null ? "" : part.trim().toLowerCase(Locale.ROOT);
    }

    private static PartBox limit(PartBox box)
    {
        AABB bounds = box.bounds();
        double minX = clamp(bounds.minX);
        double minY = clamp(bounds.minY);
        double minZ = clamp(bounds.minZ);
        // AABB orders its corners, so the size is never negative.
        return new PartBox(new AABB(minX, minY, minZ, minX + size(bounds.getXsize()), minY + size(bounds.getYsize()), minZ + size(bounds.getZsize())), Math.min(MAX_HEALTH, box.health()),
            Math.min(MAX_RESISTANCE, box.penetrationResistance()));
    }

    private static double clamp(double coordinate)
    {
        return Double.isFinite(coordinate) ? Math.max(-MAX_EXTENT, Math.min(MAX_EXTENT, coordinate)) : 0D;
    }

    private static double size(double size)
    {
        return Double.isFinite(size) ? Math.max(0D, Math.min(MAX_EXTENT, size)) : 0D;
    }

    public void load(CompoundTag root)
    {
        entries.clear();
        CompoundTag tag = root.getCompound(NBT_KEY);
        for (String part : tag.getAllKeys())
        {
            CompoundTag saved = tag.getCompound(part);
            boolean enabled = !saved.contains(ENABLED_KEY, Tag.TAG_BYTE) || saved.getBoolean(ENABLED_KEY);
            PartArmor armor = saved.contains(ARMOR_KEY, Tag.TAG_ANY_NUMERIC) && saved.contains(HEAT_ARMOR_KEY, Tag.TAG_ANY_NUMERIC)
                ? new PartArmor(saved.getFloat(ARMOR_KEY), saved.getFloat(HEAT_ARMOR_KEY))
                : null;
            put(part, new Entry(enabled, readBox(saved), null));
            setArmor(part, armor);
        }
    }

    @Nullable
    private static PartBox readBox(CompoundTag saved)
    {
        double[] bounds = new double[BOUNDS_KEYS.length];
        for (int index = 0; index < bounds.length; index++)
        {
            if (!saved.contains(BOUNDS_KEYS[index], Tag.TAG_ANY_NUMERIC))
                return null;
            bounds[index] = saved.getDouble(BOUNDS_KEYS[index]);
        }
        return limit(new PartBox(new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]), saved.getFloat(HEALTH_KEY), saved.getFloat(RESISTANCE_KEY)));
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        entries.forEach((part, entry) ->
        {
            CompoundTag saved = new CompoundTag();
            saved.putBoolean(ENABLED_KEY, entry.enabled());
            PartArmor armor = entry.armor();
            if (armor != null)
            {
                saved.putFloat(ARMOR_KEY, armor.thicknessMm());
                saved.putFloat(HEAT_ARMOR_KEY, armor.heatThicknessMm());
            }
            PartBox box = entry.box();
            if (box != null)
            {
                AABB bounds = box.bounds();
                double[] values = {bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ};
                for (int index = 0; index < values.length; index++)
                    saved.putDouble(BOUNDS_KEYS[index], values[index]);
                saved.putFloat(HEALTH_KEY, box.health());
                saved.putFloat(RESISTANCE_KEY, box.penetrationResistance());
            }
            tag.put(part, saved);
        });
        root.put(NBT_KEY, tag);
    }
}
