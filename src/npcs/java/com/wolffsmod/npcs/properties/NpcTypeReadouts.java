package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EntityTypeProperties;
import com.flansmodultimate.api.EntityTypeProperties.Weapon;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;

import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Type readouts can show distinct mount values even when no single NPC fallback can represent them. */
public final class NpcTypeReadouts
{
    private NpcTypeReadouts()
    {}

    public static Map<NpcTypeProperty, String> create(EntityTypeProperties type, Map<NpcTypeProperty, Tag> mapped, NpcWeaponOptions options, boolean nativeProjectile)
    {
        Map<NpcTypeProperty, String> values = new EnumMap<>(NpcTypeProperty.class);
        mapped.forEach((property, value) -> values.put(property, value instanceof NumericTag number ? number.getAsNumber().toString() : value.getAsString()));
        if (!nativeProjectile || !options.enabled(Feature.TYPE_PROPERTIES))
            return values;
        if (options.enabled(Feature.WEAPON_STATS))
        {
            numbers(values, type, NpcTypeProperty.DAMAGE, weapon -> Math.ceil(weapon.damage()));
            numbers(values, type, NpcTypeProperty.ACCURACY, weapon -> Math.max(1D, Math.min(100D, Math.round(100D - weapon.spread() * 5D))));
            if (type.weapons().stream().allMatch(weapon -> weapon.speed().isPresent()))
                numbers(values, type, NpcTypeProperty.SPEED, weapon -> Math.max(0D, Math.min(100D, Math.ceil(weapon.speed().getAsDouble() * 10D))));
        }
        if (options.enabled(Feature.FLAN_SOUNDS) && !type.weapons().isEmpty() && type.weapons().stream().noneMatch(weapon -> weapon.firingSound().isBlank()))
            values.put(NpcTypeProperty.FIRING_SOUND, type.weapons().stream().map(Weapon::firingSound).distinct().collect(Collectors.joining(" / ")));
        return Map.copyOf(values);
    }

    private static void numbers(Map<NpcTypeProperty, String> values, EntityTypeProperties type, NpcTypeProperty property, ToDoubleFunction<Weapon> setting)
    {
        String readout = type.weapons().stream().mapToDouble(setting).filter(Double::isFinite).mapToLong(value -> (long) value).distinct().mapToObj(Long::toString).collect(Collectors.joining(" / "));
        if (!readout.isEmpty())
            values.put(property, readout);
    }
}
