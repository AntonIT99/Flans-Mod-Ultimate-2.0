package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcArmorPreviewTest
{
    @Test
    void appliesFlanDefenseBeforeNonlinearVanillaArmor()
    {
        assertEquals(0.875F, NpcArmorPreview.protection(10F, 0.5F, 20F, 8F, 0), 1E-6F);
    }

    @Test
    void previewReflectsDamageStrengthAndEnchantmentProtection()
    {
        float ordinary = NpcArmorPreview.protection(10F, 0F, 20F, 8F, 0);
        float stronger = NpcArmorPreview.protection(40F, 0F, 20F, 8F, 0);
        assertTrue(ordinary > stronger);
        assertTrue(NpcArmorPreview.protection(10F, 0F, 20F, 8F, 10) > ordinary);
    }

    @Test
    void emptyEquipmentProtectsNothingAndDefenseCannotInvertDamage()
    {
        assertEquals(0F, NpcArmorPreview.protection(10F, 0F, 0F, 0F, 0));
        assertEquals(1F, NpcArmorPreview.protection(10F, 2F, 20F, 8F, 0));
        assertEquals(0F, NpcArmorPreview.protection(0F, 0F, 0F, 0F, 0));
    }
}
