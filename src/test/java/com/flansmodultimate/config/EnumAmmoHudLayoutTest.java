package com.flansmodultimate.config;

import org.junit.jupiter.api.Test;

import net.minecraft.world.InteractionHand;

import static org.junit.jupiter.api.Assertions.*;

class EnumAmmoHudLayoutTest
{
    @Test
    void currentLayoutRemainsDistinctFromOptInLegacyLayouts()
    {
        assertFalse(EnumAmmoHudLayout.CURRENT.isLegacy());
        assertTrue(EnumAmmoHudLayout.LEGACY_FANCY.isLegacy());
        assertTrue(EnumAmmoHudLayout.LEGACY_DEFAULT.isLegacy());
    }

    @Test
    void legacyOffsetsMatchUltimate1710()
    {
        assertEquals(new EnumAmmoHudLayout.Placement(92, 8, 90, 25),
            EnumAmmoHudLayout.LEGACY_FANCY.placement(InteractionHand.MAIN_HAND));
        assertEquals(new EnumAmmoHudLayout.Placement(75, 8, 107, 25),
            EnumAmmoHudLayout.LEGACY_FANCY.placement(InteractionHand.OFF_HAND));
        assertEquals(new EnumAmmoHudLayout.Placement(32, 59, 16, 65),
            EnumAmmoHudLayout.LEGACY_DEFAULT.placement(InteractionHand.MAIN_HAND));
        assertEquals(new EnumAmmoHudLayout.Placement(16, 59, 32, 65),
            EnumAmmoHudLayout.LEGACY_DEFAULT.placement(InteractionHand.OFF_HAND));
    }
}
