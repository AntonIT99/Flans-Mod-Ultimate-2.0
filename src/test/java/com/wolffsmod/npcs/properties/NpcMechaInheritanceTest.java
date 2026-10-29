package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.equipment.MechaMeleeProperties;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.*;

import java.util.EnumMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NpcMechaInheritanceTest
{
    @Test
    void bareMechaDoesNotInheritItsUnmultipliedReachOrMeleeDamage()
    {
        var values = new EnumMap<NpcTypeProperty, Tag>(NpcTypeProperty.class);
        values.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf(10F));
        NpcTypeProperties.offerMeleeValues(values, Optional.empty());
        assertEquals(IntTag.valueOf(0), values.get(NpcTypeProperty.MELEE_DAMAGE));
        assertFalse(values.containsKey(NpcTypeProperty.MELEE_REACH));
        assertFalse(values.containsKey(NpcTypeProperty.MELEE_DELAY));
    }

    @Test
    void swordSuppliesAllThreeFieldsAndUsesPlayerLimits()
    {
        var values = new EnumMap<NpcTypeProperty, Tag>(NpcTypeProperty.class);
        NpcTypeProperties.offerMeleeValues(values, Optional.of(MechaMeleeProperties.of(4F, 20F, 10F)));
        assertEquals(IntTag.valueOf(24), values.get(NpcTypeProperty.MELEE_DAMAGE));
        assertEquals(IntTag.valueOf(4), values.get(NpcTypeProperty.MELEE_DELAY));
        assertEquals(FloatTag.valueOf(32F), values.get(NpcTypeProperty.MELEE_REACH));
        var slow = MechaMeleeProperties.of(0F, 0F, 1F);
        assertEquals(1F, slow.damage());
        assertEquals(100, slow.delay());
        assertEquals(1D, slow.reach());
    }

    @Test
    void removingSwordRestoresSavedCadenceAndReachButKeepsZeroInheritedDamage()
    {
        var own = new EnumMap<NpcTypeProperty, Tag>(NpcTypeProperty.class);
        own.put(NpcTypeProperty.MELEE_DAMAGE, IntTag.valueOf(5));
        own.put(NpcTypeProperty.MELEE_DELAY, IntTag.valueOf(20));
        own.put(NpcTypeProperty.MELEE_REACH, FloatTag.valueOf(2F));
        var overlay = new NpcPropertyOverrides();
        var offered = new EnumMap<NpcTypeProperty, Tag>(NpcTypeProperty.class);
        NpcTypeProperties.offerMeleeValues(offered, Optional.of(MechaMeleeProperties.of(2F, 3F, 2F)));
        overlay.refresh(offered, own::get, own::put);
        assertEquals(IntTag.valueOf(12), own.get(NpcTypeProperty.MELEE_DAMAGE));
        assertEquals(IntTag.valueOf(5), own.get(NpcTypeProperty.MELEE_DELAY));
        NpcTypeProperties.offerMeleeValues(offered, Optional.empty());
        overlay.refresh(offered, own::get, own::put);
        assertEquals(IntTag.valueOf(0), own.get(NpcTypeProperty.MELEE_DAMAGE));
        assertEquals(IntTag.valueOf(20), own.get(NpcTypeProperty.MELEE_DELAY));
        assertEquals(FloatTag.valueOf(2F), own.get(NpcTypeProperty.MELEE_REACH));
    }
}
