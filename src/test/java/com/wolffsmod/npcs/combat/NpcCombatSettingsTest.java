package com.wolffsmod.npcs.combat;

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
        for (EquipmentProperty property : EquipmentProperty.values())
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
        settings.requestImport(EquipmentProperty.ARMOR_MELEE);
        settings.setCustom(EquipmentProperty.MELEE_REACH, true);
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
        assertTrue(clone.importRequested(EquipmentProperty.ARMOR_MELEE));
        assertFalse(clone.importRequested(EquipmentProperty.MELEE_DAMAGE));
        assertEquals(Set.of(EquipmentProperty.ARMOR_MELEE, EquipmentProperty.MELEE_REACH), clone.customEquipment());
        assertEquals(settings.customSignature(), clone.customSignature());
        assertEquals(80, npc.getInt("MaxHealth"));
    }

    @Test
    void anImportMakesThePropertyCustomAndIsCarriedOutOnlyOnce()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.requestImport(NpcTypeProperty.ACCURACY);
        settings.requestImport(EquipmentProperty.RANGED_SPEED);
        assertTrue(settings.isCustom(NpcTypeProperty.ACCURACY));
        assertTrue(settings.hasRequests());
        assertEquals(Set.of(NpcTypeProperty.ACCURACY), settings.takePropertyImports());
        assertEquals(Set.of(EquipmentProperty.RANGED_SPEED), settings.takeEquipmentImports());
        assertTrue(settings.isCustom(EquipmentProperty.RANGED_SPEED));
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
    void anEquipmentCopyLeavesTheModeAndOnlySwitchableStatisticsBecomeCustom()
    {
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.requestCopy(EquipmentProperty.MELEE_DAMAGE);
        assertTrue(settings.importRequested(EquipmentProperty.MELEE_DAMAGE));
        assertFalse(settings.isCustom(EquipmentProperty.MELEE_DAMAGE));
        // The item always times its own shots, so its rhythm can be imported but never kept instead.
        settings.requestImport(EquipmentProperty.FIRING_DELAY);
        assertTrue(settings.importRequested(EquipmentProperty.FIRING_DELAY));
        assertFalse(settings.isCustom(EquipmentProperty.FIRING_DELAY));
        settings.setCustom(EquipmentProperty.MELEE_DAMAGE, true);
        settings.setCustom(EquipmentProperty.MELEE_DAMAGE, false);
        assertFalse(settings.isCustom(EquipmentProperty.MELEE_DAMAGE));
    }

    @Test
    void anEarlierWholeKindImportStillCopiesEveryStatisticOfThatKind()
    {
        CompoundTag root = new CompoundTag();
        CompoundTag tag = new CompoundTag();
        ListTag imports = new ListTag();
        imports.add(StringTag.valueOf("RANGED"));
        imports.add(StringTag.valueOf("ARMOR"));
        tag.put("EquipmentImports", imports);
        root.put(NpcCombatSettings.NBT_KEY, tag);
        NpcCombatSettings settings = new NpcCombatSettings();
        settings.load(root);
        for (EquipmentProperty property : EquipmentProperty.values())
            // The former kinds were the main-hand weapon and the armor; the off hand had none.
            assertEquals(property.getGroup() != EquipmentProperty.Group.MELEE && property.getGroup().getSlot() != EquipmentProperty.Slot.OFF_HAND
                && property.getGroup().getSlot() != EquipmentProperty.Slot.PROJECTILE_SLOT, settings.importRequested(property), property.name());
        // Those imports were copies; they do not make the NPC keep its own values.
        assertTrue(settings.customEquipment().isEmpty());
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
