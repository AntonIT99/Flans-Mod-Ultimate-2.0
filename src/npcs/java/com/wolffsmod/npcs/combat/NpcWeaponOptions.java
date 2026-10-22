package com.wolffsmod.npcs.combat;

import lombok.Getter;

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
            true), MODEL_WEAPON_STATS(true), FIRE_SOUNDS(true), SHOOT_PARTICLES(true), LEAD_TARGET(true), BALLISTIC_AIM(true), SECONDARY_BANK(false), RELOAD_SOUNDS(true), THROW_SOUNDS(
                true), MODEL_SOUNDS(true), MELEE_SOUNDS(true), VEHICLE_DAMAGE(true), MODEL_ARMOUR(true), INVULNERABLE(false), FLAN_DAMAGE_TYPES(true), RELOAD_CYCLE(true), DUAL_WIELD(
                    true), GUN_SHOT_ANIMATION(true), GUN_RELOAD_ANIMATION(true), GUN_MELEE_ANIMATION(true), NATIVE_THROWABLES(true), GRADUAL_AIM(true), FIRE_WHEN_ALIGNED(true), TURRET_LOCKED(
                        false), PASSENGER_GUNS(true), TURRET_MUZZLES(true), ADVANCED_LEAD(true), HULL_FACES_TRAVEL(true), MISALIGNED_THROTTLE(true), RIDER_AT_SEAT(
                            true), EYE_AT_MUZZLE(true), ANIMATED_MODEL(true), DOORS_OPEN(false), GEAR_UP(false), WINGS_FOLDED(false), ENGINE_RUNNING(true), FULL_THROTTLE(false), MODEL_CULLING(
                                true), PROGRESSIVE_SEARCH(true), ADAPTIVE_PATH_RANGE(true), STAY_ACTIVE(false), HULL_ASSISTS_AIM(true), DRIVING(true), DRIVE_REVERSE(true), PATH_DEBUG(
                                    false), CONSUME_AMMO(false), PART_HITBOXES(true), PART_HEALTH(true), PART_EFFECTS(true), BROKEN_PARTS_PASS(true), PART_REPAIR(true), HITBOX_OUTLINES(
                                        false), SOLID_HITBOXES(true), ARMOR_POTION_EFFECTS(true), MANNED_GUNS_FIRE(true), MELEE_DUAL_WIELD(false), DRIVE_DRIVEABLES(false), RETURN_HOME_TELEPORT(true);

        /** Whether the switch is on for an NPC whose save does not mention it. */
        @Getter
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
        {
            boolean fallback = feature.defaultEnabled;
            // Split the former switch without changing saved NPC audio preferences.
            if (isSoundFeature(feature) && tag.contains("FLAN_SOUNDS", Tag.TAG_BYTE))
                fallback = tag.getBoolean("FLAN_SOUNDS");
            // The model weapon switch was saved under its earlier name.
            if (feature == Feature.MODEL_WEAPON_STATS && tag.contains("WEAPON_STATS", Tag.TAG_BYTE))
                fallback = tag.getBoolean("WEAPON_STATS");
            set(feature, tag.contains(feature.name(), Tag.TAG_BYTE) ? tag.getBoolean(feature.name()) : fallback);
        }
    }

    private static boolean isSoundFeature(Feature feature)
    {
        return feature == Feature.FIRE_SOUNDS || feature == Feature.RELOAD_SOUNDS || feature == Feature.THROW_SOUNDS || feature == Feature.MODEL_SOUNDS || feature == Feature.MELEE_SOUNDS;
    }

    public void save(CompoundTag root)
    {
        CompoundTag tag = new CompoundTag();
        for (Feature feature : Feature.values())
            tag.putBoolean(feature.name(), enabled(feature));
        root.put(NBT_KEY, tag);
    }
}
