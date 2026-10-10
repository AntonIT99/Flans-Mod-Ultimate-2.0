package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EntityTypeProperties;
import com.flansmodultimate.api.EntityTypeProperties.Sound;
import com.flansmodultimate.api.EntityTypeProperties.Weapon;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;

import net.minecraft.nbt.*;

import java.util.*;
import java.util.function.ToDoubleFunction;

/** Converts type units to the equivalents Custom NPCs can represent, without guessing unrelated fields. */
public final class NpcTypeMapping
{
    private NpcTypeMapping()
    {}

    public static Map<NpcTypeProperty, Tag> create(EntityTypeProperties type, NpcWeaponOptions options, boolean nativeProjectile)
    {
        return create(type, options, nativeProjectile, 1024D);
    }

    public static Map<NpcTypeProperty, Tag> create(EntityTypeProperties type, NpcWeaponOptions options, boolean nativeProjectile, double maxHealth)
    {
        Map<NpcTypeProperty, Tag> values = new EnumMap<>(NpcTypeProperty.class);
        if (!options.enabled(Feature.TYPE_PROPERTIES))
            return values;
        integer(values, NpcTypeProperty.HEALTH, type.health(), 1D, 1, (int) Math.min(Integer.MAX_VALUE, maxHealth));
        integer(values, NpcTypeProperty.AGGRO_RANGE, type.targetRange(), 1D, 1, 512);
        integer(values, NpcTypeProperty.RANGE, type.targetRange(), 1D, 1, 256);
        integer(values, NpcTypeProperty.WALKING_SPEED, type.walkingSpeed(), 20D, 0, 100);
        type.meleeReach().ifPresent(reach -> values.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf((float) Math.max(0D, reach))));
        type.worksUnderWater().ifPresent(works -> values.put(NpcTypeProperty.CAN_DROWN, ByteTag.valueOf(!works)));
        type.takesFallDamage().ifPresent(takes -> values.put(NpcTypeProperty.NO_FALL_DAMAGE, ByteTag.valueOf(!takes)));
        timing(values, type, nativeProjectile);
        if (options.enabled(Feature.WEAPON_STATS))
            weaponStats(values, type, nativeProjectile);
        sounds(values, type, options);
        return Map.copyOf(values);
    }

    private static void timing(Map<NpcTypeProperty, Tag> values, EntityTypeProperties type, boolean nativeProjectile)
    {
        if (type.shootDelay().isEmpty())
            return;
        for (NpcTypeProperty property : new NpcTypeProperty[]{NpcTypeProperty.DELAY_MIN, NpcTypeProperty.DELAY_MAX, NpcTypeProperty.BURST_DELAY})
            integer(values, property, type.shootDelay(), 1D, 1, 9999);
        // Each native trigger already emits the bank's barrels and configured pellets.
        if (nativeProjectile)
        {
            values.put(NpcTypeProperty.BURST, IntTag.valueOf(1));
            values.put(NpcTypeProperty.SHOT_COUNT, IntTag.valueOf(1));
        }
    }

    private static void weaponStats(Map<NpcTypeProperty, Tag> values, EntityTypeProperties type, boolean nativeProjectile)
    {
        integer(values, NpcTypeProperty.DAMAGE, common(type, Weapon::damage), 1D, 0, Integer.MAX_VALUE);
        OptionalDouble spread = common(type, Weapon::spread);
        spread.ifPresent(value -> values.put(NpcTypeProperty.ACCURACY, IntTag.valueOf((int) Math.max(1D, Math.min(100D, Math.round(100D - value * 5D))))));
        if (type.weapons().stream().allMatch(weapon -> weapon.speed().isPresent()))
            integer(values, NpcTypeProperty.SPEED, common(type, weapon -> weapon.speed().getAsDouble()), 10D, 0, 100);
        if (!nativeProjectile)
            integer(values, NpcTypeProperty.SHOT_COUNT, common(type, Weapon::projectiles), 1D, 1, 10);
    }

    private static void sounds(Map<NpcTypeProperty, Tag> values, EntityTypeProperties type, NpcWeaponOptions options)
    {
        if (options.enabled(Feature.MODEL_SOUNDS))
            type.sounds().forEach((sound, value) -> values.put(sound == Sound.IDLE ? NpcTypeProperty.IDLE_SOUND : NpcTypeProperty.STEP_SOUND, StringTag.valueOf(value)));
        if (!options.enabled(Feature.FIRE_SOUNDS))
            return;
        var firingSounds = type.weapons().stream().map(Weapon::firingSound).filter(sound -> !sound.isBlank()).distinct().toList();
        if (firingSounds.size() == 1 && type.weapons().stream().allMatch(weapon -> !weapon.firingSound().isBlank()))
            values.put(NpcTypeProperty.FIRING_SOUND, StringTag.valueOf(firingSounds.get(0)));
    }

    private static OptionalDouble common(EntityTypeProperties type, ToDoubleFunction<Weapon> setting)
    {
        double[] values = type.weapons().stream().mapToDouble(setting).distinct().toArray();
        return values.length == 1 && Double.isFinite(values[0]) ? OptionalDouble.of(values[0]) : OptionalDouble.empty();
    }

    private static void integer(Map<NpcTypeProperty, Tag> values, NpcTypeProperty property, OptionalDouble source, double scale, int min, int max)
    {
        source.ifPresent(value -> values.put(property, IntTag.valueOf((int) Math.max(min, Math.min(max, Math.ceil(value * scale))))));
    }
}
