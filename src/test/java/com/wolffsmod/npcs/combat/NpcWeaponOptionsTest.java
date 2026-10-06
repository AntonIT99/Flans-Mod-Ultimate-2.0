package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcWeaponOptionsTest
{
    @Test
    void oldNpcSavesEnableTheUsefulFeaturesAndSelectThePrimaryBank()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        for (Feature feature : Feature.values())
        {
            if (feature == Feature.SECONDARY_BANK)
                assertFalse(options.enabled(feature));
            else
                assertTrue(options.enabled(feature));
        }
    }

    @Test
    void disabledFeaturesAndSecondarySelectionSurviveSavingAndCloning()
    {
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.set(Feature.PROJECTILES, false);
        options.set(Feature.TYPE_PROPERTIES, false);
        options.set(Feature.FLAN_SOUNDS, false);
        options.set(Feature.ITEM_ARMOR, false);
        options.set(Feature.WEAPON_ANIMATIONS, false);
        options.set(Feature.SECONDARY_BANK, true);
        CompoundTag npc = new CompoundTag();
        npc.putInt("pDamage", 19);
        options.save(npc);
        NpcWeaponOptions clone = new NpcWeaponOptions();
        clone.load(npc.copy());
        assertFalse(clone.enabled(Feature.PROJECTILES));
        assertFalse(clone.enabled(Feature.TYPE_PROPERTIES));
        assertFalse(clone.enabled(Feature.FLAN_SOUNDS));
        assertFalse(clone.enabled(Feature.ITEM_ARMOR));
        assertFalse(clone.enabled(Feature.WEAPON_ANIMATIONS));
        assertTrue(clone.enabled(Feature.ITEM_WEAPONS));
        assertTrue(clone.enabled(Feature.ARMOR_ANIMATIONS));
        assertTrue(clone.enabled(Feature.SECONDARY_BANK));
        assertTrue(clone.enabled(Feature.BALLISTIC_AIM));
        assertEquals(19, npc.getInt("pDamage"));
    }

    @Test
    void partialSettingsFromOlderVersionsKeepDefaultsForNewFeatures()
    {
        CompoundTag root = new CompoundTag();
        CompoundTag old = new CompoundTag();
        old.putBoolean("PROJECTILES", false);
        old.putString("GRENADES", "invalid");
        root.put(NpcWeaponOptions.NBT_KEY, old);
        NpcWeaponOptions options = new NpcWeaponOptions();
        options.load(root);
        assertFalse(options.enabled(Feature.PROJECTILES));
        assertTrue(options.enabled(Feature.GRENADES));
        assertTrue(options.enabled(Feature.LEAD_TARGET));
        assertTrue(options.enabled(Feature.TYPE_PROPERTIES));
        options.load(new CompoundTag());
        assertTrue(options.enabled(Feature.PROJECTILES));
    }
}
