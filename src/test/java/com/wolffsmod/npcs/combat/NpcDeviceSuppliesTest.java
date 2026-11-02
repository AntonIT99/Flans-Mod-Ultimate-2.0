package com.wolffsmod.npcs.combat;

import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcDeviceSuppliesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }

    private static EntityNPCInterface fullInventory()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        for (int slot = 0; slot < NpcAmmunition.INVENTORY_SLOTS; slot++)
            npc.inventory.drops.put(slot, wrapped(new ItemStack(Items.STONE, 64)));
        return npc;
    }

    @Test
    void fullInventoriesRefuseSuppliesWithoutLosingOrReplacingAnything()
    {
        EntityNPCInterface npc = fullInventory();
        var before = new java.util.HashMap<>(npc.inventory.drops);
        ItemStack offered = new ItemStack(Items.IRON_NUGGET, 4);
        assertFalse(NpcDeviceSupplies.store(npc, offered));
        assertEquals(before, npc.inventory.drops);
        assertEquals(4, offered.getCount());
    }

    @Test
    void supplyTransactionsFillPartialStacksAndUseEmptySlotsWithoutChangingDropChancesOfExistingItems()
    {
        EntityNPCInterface npc = fullInventory();
        ItemStack partial = new ItemStack(Items.IRON_NUGGET, 63);
        npc.inventory.drops.put(0, wrapped(partial));
        npc.inventory.dropchance.put(0, 0.5F);
        npc.inventory.drops.remove(1);
        ItemStack offered = new ItemStack(Items.IRON_NUGGET, 64);
        NpcAPI api = mock(NpcAPI.class);
        when(api.getIItemStack(any(ItemStack.class))).thenAnswer(call -> wrapped(call.getArgument(0)));
        try (var npcs = mockStatic(NpcAPI.class))
        {
            npcs.when(NpcAPI::Instance).thenReturn(api);
            assertTrue(NpcDeviceSupplies.store(npc, offered));
            assertEquals(64, partial.getCount());
            ItemStack stored = npc.inventory.drops.get(1).getMCItemStack();
            assertEquals(63, stored.getCount());
            assertEquals(0.5F, npc.inventory.dropchance.get(0));
            assertEquals(0F, npc.inventory.dropchance.get(1));
            assertEquals(64, offered.getCount());
            assertTrue(npc.updateClient);
        }
    }

    @Test
    void insufficientSpaceDoesNotPartiallyFillAMagazineStack()
    {
        EntityNPCInterface npc = fullInventory();
        ItemStack partial = new ItemStack(Items.IRON_NUGGET, 63);
        npc.inventory.drops.put(0, wrapped(partial));
        assertFalse(NpcDeviceSupplies.store(npc, new ItemStack(Items.IRON_NUGGET, 2)));
        assertEquals(63, partial.getCount());
    }
}
