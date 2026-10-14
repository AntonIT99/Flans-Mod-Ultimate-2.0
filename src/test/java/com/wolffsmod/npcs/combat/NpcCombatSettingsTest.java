package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.combat.NpcCombatSettings.EquipmentImport;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NpcCombatSettingsTest
{
    @Test
    void npcsSavedBeforeTheseSettingsInheritEverythingAndAcceptEveryHit()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.load(new CompoundTag());
        assertEquals(Knockback.MODEL, settings.getKnockback());
        for (NpcDamageCategory category : NpcDamageCategory.values())
            assertEquals(0, settings.minimumHit(category));
        for (NpcTypeProperty property : NpcTypeProperty.values())
            assertFalse(settings.isCustom(property));
        assertFalse(settings.hasRequests());
        assertEquals(0, settings.customSignature());
    }

    @Test
    void settingsAndPendingRequestsSurviveSavingAndCloning()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.setKnockback(Knockback.IMMUNE);
        settings.setMinimumHit(NpcDamageCategory.PROJECTILE, 12);
        settings.setMinimumHit(NpcDamageCategory.EXPLOSION, 40);
        settings.setCustom(NpcTypeProperty.HEALTH, true);
        settings.requestImport(NpcTypeProperty.DAMAGE);
        settings.requestImport(EquipmentImport.ARMOR);
        CompoundTag npc = new CompoundTag();
        npc.putInt("MaxHealth", 80);
        settings.save(npc);
        NpcCombatSettings clone = new NpcCombatSettings();
        clone.load(npc.copy());
        assertEquals(Knockback.IMMUNE, clone.getKnockback());
        assertEquals(12, clone.minimumHit(NpcDamageCategory.PROJECTILE));
        assertEquals(0, clone.minimumHit(NpcDamageCategory.MELEE));
        assertEquals(40, clone.minimumHit(NpcDamageCategory.EXPLOSION));
        assertEquals(Set.of(NpcTypeProperty.HEALTH, NpcTypeProperty.DAMAGE), clone.customProperties());
        assertTrue(clone.importRequested(NpcTypeProperty.DAMAGE));
        assertFalse(clone.importRequested(NpcTypeProperty.HEALTH));
        assertTrue(clone.importRequested(EquipmentImport.ARMOR));
        assertFalse(clone.importRequested(EquipmentImport.MELEE));
        assertEquals(settings.customSignature(), clone.customSignature());
        assertEquals(80, npc.getInt("MaxHealth"));
    }

    @Test
    void anImportMakesThePropertyCustomAndIsCarriedOutOnlyOnce()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.requestImport(NpcTypeProperty.ACCURACY);
        settings.requestImport(EquipmentImport.RANGED);
        assertTrue(settings.isCustom(NpcTypeProperty.ACCURACY));
        assertTrue(settings.hasRequests());
        assertEquals(Set.of(NpcTypeProperty.ACCURACY), settings.takePropertyImports());
        assertEquals(Set.of(EquipmentImport.RANGED), settings.takeEquipmentImports());
        assertFalse(settings.hasRequests());
        assertTrue(settings.takePropertyImports().isEmpty());
        // The imported value stays custom until inheritance is chosen again.
        assertTrue(settings.isCustom(NpcTypeProperty.ACCURACY));
    }

    @Test
    void returningToInheritanceCancelsAPendingImportAndChangesWhatIsInherited()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.requestImport(NpcTypeProperty.SPEED);
        assertNotEquals(0, settings.customSignature());
        settings.setCustom(NpcTypeProperty.SPEED, false);
        assertFalse(settings.importRequested(NpcTypeProperty.SPEED));
        assertFalse(settings.hasRequests());
        assertEquals(0, settings.customSignature());
        settings.requestImport(NpcTypeProperty.SPEED);
        settings.clearRequests();
        assertTrue(settings.isCustom(NpcTypeProperty.SPEED));
        assertFalse(settings.hasRequests());
    }

    @Test
    void minimumHitsAreClampedAndUnknownSavedNamesAreIgnored()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.setMinimumHit(NpcDamageCategory.MELEE, -5);
        assertEquals(0, settings.minimumHit(NpcDamageCategory.MELEE));
        settings.setMinimumHit(NpcDamageCategory.MELEE, Integer.MAX_VALUE);
        assertEquals(NpcCombatSettings.MAX_MINIMUM_HIT, settings.minimumHit(NpcDamageCategory.MELEE));
        settings.setKnockback(null);
        assertEquals(Knockback.MODEL, settings.getKnockback());
        CompoundTag root = new CompoundTag();
        settings.save(root);
        CompoundTag saved = root.getCompound(NpcCombatSettings.NBT_KEY);
        saved.putString("Knockback", "LAUNCHED");
        saved.getList("CustomProperties", Tag.TAG_STRING).add(StringTag.valueOf("REMOVED_PROPERTY"));
        settings.load(root);
        assertEquals(Knockback.MODEL, settings.getKnockback());
        assertTrue(settings.customProperties().isEmpty());
        assertEquals(NpcCombatSettings.MAX_MINIMUM_HIT, settings.minimumHit(NpcDamageCategory.MELEE));
    }
}
