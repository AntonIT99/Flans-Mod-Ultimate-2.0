package com.wolffsmod.npcs.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;

/** Per-NPC switches; absent keys in existing saves keep the useful defaults enabled. */
public final class NpcWeaponOptions
{
    public static final String NBT_KEY = "WolffsModWeapons";
    private final EnumSet<Feature> enabled = EnumSet.noneOf(Feature.class);

    public enum Feature
    {
        ITEM_ARMOR(true), ITEM_WEAPONS(true), ARMOR_ANIMATIONS(true), WEAPON_ANIMATIONS(true), TYPE_PROPERTIES(true), PROJECTILES(true), GRENADES(true), MODEL_MUZZLES(true), ALTERNATE_BARRELS(
            true), WEAPON_STATS(true), FLAN_SOUNDS(true), SHOOT_PARTICLES(true), LEAD_TARGET(true), BALLISTIC_AIM(true), SECONDARY_BANK(false);

        private final boolean defaultEnabled;

        Feature(boolean defaultEnabled)
        {
            this.defaultEnabled = defaultEnabled;
        }
    }

    public NpcWeaponOptions()
    {
        load(new CompoundTag());
    }

    public boolean enabled(Feature feature)
    {
        return enabled.contains(feature);
    }

    public void set(Feature feature, boolean value)
    {
        if (value)
            enabled.add(feature);
        else
            enabled.remove(feature);
    }

    public void load(CompoundTag root)
    {
        CompoundTag tag = root.getCompound(NBT_KEY);
        for (Feature feature : Feature.values())
            set(feature, tag.contains(feature.name(), Tag.TAG_BYTE) ? tag.getBoolean(feature.name()) : feature.defaultEnabled);
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        for (Feature feature : Feature.values())
            tag.putBoolean(feature.name(), enabled(feature));
        root.put(NBT_KEY, tag);
    }
}
