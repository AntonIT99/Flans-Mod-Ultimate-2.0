package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcEmplacementAmmoTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void placedGunsAndAaUseDefaultsOnlyWithInfiniteAmmunitionAndFallbackEnabled()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        IContentType gun = mock(IContentType.class);
        IAAGunType aa = mock(IAAGunType.class);
        ItemStack round = new ItemStack(Items.IRON_NUGGET);
        try (var equipment = mockStatic(NpcEquipment.class); var flans = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(true);
            flans.when(() -> FlansEquipment.createDefaultAmmunitionChoices(gun)).thenReturn(List.of(round));
            flans.when(() -> FlansEquipment.createDefaultAmmunitionChoices(aa, false)).thenReturn(List.of(round));
            for (IContentType type : List.of(gun, aa))
                assertSame(round, NpcAmmunition.emplacementTemplate(npc, type));
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(true);
            for (IContentType type : List.of(gun, aa))
                assertTrue(NpcAmmunition.emplacementTemplate(npc, type).isEmpty());
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(false);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO)).thenReturn(false);
            for (IContentType type : List.of(gun, aa))
                assertTrue(NpcAmmunition.emplacementTemplate(npc, type).isEmpty());
        }
    }

    @Test
    void aFiniteMagazineTransfersOnceAndAnInfiniteMagazineLeavesInventoryUntouched()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        IAAGunType aa = mock(IAAGunType.class);
        ItemStack carried = new ItemStack(Items.IRON_NUGGET, 2);
        IItemStack entry = mock(IItemStack.class);
        when(entry.getMCItemStack()).thenReturn(carried);
        npc.inventory.drops.put(0, entry);
        try (var equipment = mockStatic(NpcEquipment.class); var flans = mockStatic(FlansEquipment.class))
        {
            flans.when(() -> FlansEquipment.acceptsAmmunition(aa, false, carried)).thenReturn(true);
            ItemStack infinite = NpcAmmunition.takeEmplacementAmmo(npc, aa);
            assertEquals(1, infinite.getCount());
            assertEquals(2, carried.getCount());
            assertNotSame(carried, infinite);
            equipment.when(() -> NpcEquipment.enabled(npc, Feature.CONSUME_AMMO)).thenReturn(true);
            assertEquals(1, NpcAmmunition.takeEmplacementAmmo(npc, aa).getCount());
            assertEquals(1, carried.getCount());
            assertEquals(1, NpcAmmunition.takeEmplacementAmmo(npc, aa).getCount());
            assertTrue(npc.inventory.drops.isEmpty());
        }
    }
}
