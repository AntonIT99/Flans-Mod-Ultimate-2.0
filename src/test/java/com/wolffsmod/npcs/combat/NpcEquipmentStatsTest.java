package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.EquippedWeaponProperties;
import com.wolffsmod.npcs.combat.NpcEquipmentStats.Shot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEquipmentStatsTest
{
    @Test
    void aWeaponWithoutAShootSoundSupportsImportAndEditorBlankChecks()
    {
        Shot shot = new Shot(5D, 3D, 1D, 1, null, EquippedWeaponProperties.Impact.NONE);

        assertTrue(shot.firingSound().isBlank());
        assertEquals("", shot.firingSound());
        assertEquals(5D, shot.damage());
    }

    @Test
    void aConfiguredFiringSoundIsPreserved()
    {
        Shot shot = new Shot(5D, 3D, 1D, 1, "flansmod:rifle", EquippedWeaponProperties.Impact.NONE);

        assertEquals("flansmod:rifle", shot.firingSound());
    }
}
