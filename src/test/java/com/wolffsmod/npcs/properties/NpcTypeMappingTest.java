package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EntityTypeProperties;
import com.flansmodultimate.api.EntityTypeProperties.Sound;
import com.flansmodultimate.api.EntityTypeProperties.Weapon;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcTypeMappingTest
{
    @Test
    void mapsTypePropertiesToNpcUnitsAndKeepsNativePelletsOutOfShotCount()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        EntityTypeProperties type = type(List.of(weapon(12, 2, 3, "flansmod:fire")));
        var mapped = NpcTypeMapping.create(type, options, true);
        assertEquals(IntTag.valueOf(200), mapped.get(NpcTypeProperty.HEALTH));
        assertEquals(IntTag.valueOf(30), mapped.get(NpcTypeProperty.SPEED));
        assertEquals(IntTag.valueOf(90), mapped.get(NpcTypeProperty.ACCURACY));
        assertEquals(IntTag.valueOf(5), mapped.get(NpcTypeProperty.DELAY_MIN));
        assertEquals(IntTag.valueOf(1), mapped.get(NpcTypeProperty.SHOT_COUNT));
        assertEquals(IntTag.valueOf(9), mapped.get(NpcTypeProperty.WALKING_SPEED));
        assertEquals(FloatTag.valueOf(10), mapped.get(NpcTypeProperty.MELEE_REACH));
        assertEquals(ByteTag.valueOf(false), mapped.get(NpcTypeProperty.CAN_DROWN));
        assertEquals(ByteTag.valueOf(true), mapped.get(NpcTypeProperty.NO_FALL_DAMAGE));
        assertEquals(IntTag.valueOf(8), NpcTypeMapping.create(type, options, false).get(NpcTypeProperty.SHOT_COUNT));
    }

    @Test
    void inheritanceAndExistingWeaponAudioSwitchesAreIndependent()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        EntityTypeProperties type = type(List.of(weapon(12, 2, 3, "flansmod:fire")));
        options.set(Feature.WEAPON_STATS, false);
        options.set(Feature.FLAN_SOUNDS, false);
        var mapped = NpcTypeMapping.create(type, options, true);
        assertTrue(mapped.containsKey(NpcTypeProperty.HEALTH));
        assertTrue(mapped.containsKey(NpcTypeProperty.DELAY_MIN));
        assertFalse(mapped.containsKey(NpcTypeProperty.DAMAGE));
        assertFalse(mapped.containsKey(NpcTypeProperty.SPEED));
        assertFalse(mapped.containsKey(NpcTypeProperty.FIRING_SOUND));
        assertFalse(mapped.containsKey(NpcTypeProperty.IDLE_SOUND));
        options.set(Feature.TYPE_PROPERTIES, false);
        assertTrue(NpcTypeMapping.create(type, options, true).isEmpty());
    }

    @Test
    void mixedMountsDisplayTheirOwnValuesWithoutReplacingUsedFallbacks()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        var type = type(List.of(weapon(12, 2, 3, "flansmod:fire"), weapon(24, 4, 5, "flansmod:other")));
        var mapped = NpcTypeMapping.create(type, options, true);
        assertFalse(mapped.containsKey(NpcTypeProperty.DAMAGE));
        var readouts = NpcTypeReadouts.create(type, mapped, options, true);
        assertEquals("12 / 24", readouts.get(NpcTypeProperty.DAMAGE));
        assertEquals("90 / 80", readouts.get(NpcTypeProperty.ACCURACY));
        assertEquals("30 / 50", readouts.get(NpcTypeProperty.SPEED));
        assertEquals("flansmod:fire / flansmod:other", readouts.get(NpcTypeProperty.FIRING_SOUND));
        var withFallback = type(List.of(weapon(12, 2, 3, "flansmod:fire"), new Weapon("aa", 24, 4, OptionalDouble.empty(), 1, "")));
        var mixed = NpcTypeMapping.create(withFallback, options, true);
        var mixedReadout = NpcTypeReadouts.create(withFallback, mixed, options, true);
        assertFalse(mixedReadout.containsKey(NpcTypeProperty.SPEED));
        assertFalse(mixedReadout.containsKey(NpcTypeProperty.FIRING_SOUND));
    }

    @Test
    void effectiveHealthRespectsTheLiveAttributeLimit()
    {
        var type = new EntityTypeProperties(OptionalDouble.of(50000), OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), Optional.empty(),
            OptionalDouble.empty(), Map.of(), List.of());
        assertEquals(IntTag.valueOf(1024), NpcTypeMapping.create(type, new NpcWeaponOptions(), true).get(NpcTypeProperty.HEALTH));
        assertEquals(IntTag.valueOf(50000), NpcTypeMapping.create(type, new NpcWeaponOptions(), true, 100000).get(NpcTypeProperty.HEALTH));
    }

    @Test
    void fractionalReachMatchesTheIntegerNpcFieldAndStopsRefreshing()
    {
        var type = new EntityTypeProperties(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.of(2.6), Optional.empty(), Optional.empty(), OptionalDouble.empty(),
            Map.of(), List.of());
        var mapped = NpcTypeMapping.create(type, new NpcWeaponOptions(), true);
        assertEquals(FloatTag.valueOf(3F), mapped.get(NpcTypeProperty.MELEE_REACH));
        Map<NpcTypeProperty, net.minecraft.nbt.Tag> current = new java.util.EnumMap<>(NpcTypeProperty.class);
        current.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf(1F));
        NpcPropertyOverrides overrides = new NpcPropertyOverrides();
        assertTrue(overrides.refresh(mapped, current::get, current::put));
        assertFalse(overrides.refresh(mapped, current::get, current::put));
    }

    private static Weapon weapon(double damage, double spread, double speed, String sound)
    {
        return new Weapon("test", damage, spread, OptionalDouble.of(speed), 8, sound);
    }

    private static EntityTypeProperties type(List<Weapon> weapons)
    {
        return new EntityTypeProperties(OptionalDouble.of(200), OptionalDouble.of(80), OptionalDouble.of(0.43), OptionalDouble.of(10), Optional.of(true), Optional.of(false), OptionalDouble.of(4.5),
            Map.of(Sound.IDLE, "flansmod:idle", Sound.STEP, "flansmod:step"), weapons);
    }
}
