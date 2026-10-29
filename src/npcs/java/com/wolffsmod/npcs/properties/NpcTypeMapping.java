package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.driveables.ReloadCycle;
import com.flansmodultimate.api.entity.EntityTypeProperties;
import com.flansmodultimate.api.entity.EntityTypeProperties.Sound;
import com.flansmodultimate.api.entity.EntityTypeProperties.Weapon;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcMobility;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.function.DoubleUnaryOperator;
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
        return create(type, options, nativeProjectile, maxHealth, null);
    }

    /**
     * @param cycle
     *            the reload cycle of the model with the ammunition of the NPC, or with the model's default ammunition
     *            when the NPC carries none it fires; null when the NPC follows none. With one, the reload becomes the
     *            delay and the magazine the burst, as Custom NPCs understands them
     */
    public static Map<NpcTypeProperty, Tag> create(EntityTypeProperties type, NpcWeaponOptions options, boolean nativeProjectile, double maxHealth, @Nullable ReloadCycle cycle)
    {
        Map<NpcTypeProperty, Tag> values = new EnumMap<>(NpcTypeProperty.class);
        if (!options.enabled(Feature.TYPE_PROPERTIES))
            return values;
        integer(values, NpcTypeProperty.HEALTH, type.health(), 1D, 1, (int) Math.min(Integer.MAX_VALUE, maxHealth));
        // A walking speed in blocks a tick becomes the speed value whose square a mob walks at.
        integer(values, NpcTypeProperty.WALKING_SPEED, map(type.walkingSpeed(), NpcMobility::walkingSpeedFor), 1D, 0, 100);
        type.meleeReach().ifPresent(reach -> values.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf(CustomNpcsPlatform.meleeRange(Math.max(0D, reach)))));
        // A machine has no breath to lose, takes no potions, rolls through cobwebs and does not heal by itself; a flooded
        // engine is the model's own concern.
        values.put(NpcTypeProperty.CAN_DROWN, ByteTag.valueOf(false));
        values.put(NpcTypeProperty.POTION_IMMUNE, ByteTag.valueOf(true));
        values.put(NpcTypeProperty.IGNORE_COBWEB, ByteTag.valueOf(true));
        values.put(NpcTypeProperty.HEALTH_REGEN, IntTag.valueOf(0));
        values.put(NpcTypeProperty.COMBAT_REGEN, IntTag.valueOf(0));
        type.takesFallDamage().ifPresent(takes -> values.put(NpcTypeProperty.NO_FALL_DAMAGE, ByteTag.valueOf(!takes)));
        timing(values, type, nativeProjectile, cycle);
        if (options.enabled(Feature.MODEL_WEAPON_STATS))
            weaponStats(values, type, nativeProjectile);
        sounds(values, type, options);
        return Map.copyOf(values);
    }

    private static void timing(Map<NpcTypeProperty, Tag> values, EntityTypeProperties type, boolean nativeProjectile, @Nullable ReloadCycle cycle)
    {
        if (type.shootDelay().isEmpty())
            return;
        integer(values, NpcTypeProperty.BURST_DELAY, type.shootDelay(), 1D, 1, 9999);
        if (cycle != null)
        {
            // The NPC fires its magazine one trigger at a time, then waits out the reload.
            integer(values, NpcTypeProperty.DELAY_MIN, OptionalDouble.of(cycle.reloadTicks()), 1D, 1, 9999);
            integer(values, NpcTypeProperty.DELAY_MAX, OptionalDouble.of(cycle.reloadTicks()), 1D, 1, 9999);
            values.put(NpcTypeProperty.BURST, IntTag.valueOf(Math.min(9999, cycle.volleys())));
            if (nativeProjectile)
                values.put(NpcTypeProperty.SHOT_COUNT, IntTag.valueOf(1));
            return;
        }
        integer(values, NpcTypeProperty.DELAY_MIN, type.shootDelay(), 1D, 1, 9999);
        integer(values, NpcTypeProperty.DELAY_MAX, type.shootDelay(), 1D, 1, 9999);
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
        spread.ifPresent(value -> values.put(NpcTypeProperty.ACCURACY, IntTag.valueOf((int) Mth.clamp(Math.round(100D - value * 5D), 1L, 100L))));
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

    private static OptionalDouble map(OptionalDouble source, DoubleUnaryOperator mapping)
    {
        return source.isPresent() ? OptionalDouble.of(mapping.applyAsDouble(source.getAsDouble())) : source;
    }

    private static void integer(Map<NpcTypeProperty, Tag> values, NpcTypeProperty property, OptionalDouble source, double scale, int min, int max)
    {
        source.ifPresent(value -> values.put(property, IntTag.valueOf((int) Mth.clamp(Math.ceil(value * scale), min, max))));
    }
}
