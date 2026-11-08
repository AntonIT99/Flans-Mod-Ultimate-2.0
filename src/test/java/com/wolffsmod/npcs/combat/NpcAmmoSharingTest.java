package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcAmmoSharingTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void transfersRealTaggedAmmunitionForAStowedWeaponWithoutCreatingOrDuplicatingIt()
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface donor = npc(level);
        EntityNPCInterface ally = npc(level);
        ItemStack gun = new ItemStack(Items.BOW);
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET, 2);
        ammo.getOrCreateTag().putInt("remaining", 7);
        donor.inventory.drops.put(0, wrapped(ammo));
        donor.inventory.dropchance.put(0, 0F);
        ally.inventory.drops.put(0, wrapped(gun));
        NpcAPI api = mock(NpcAPI.class);
        when(api.getIItemStack(any(ItemStack.class))).thenAnswer(call -> wrapped(call.getArgument(0)));
        try (var equipment = mockStatic(NpcEquipment.class); var flans = mockStatic(FlansEquipment.class); var rounds = mockStatic(FlansProjectiles.class); var npcs = mockStatic(NpcAPI.class))
        {
            equipment.when(() -> NpcEquipment.enabled(any(), eq(Feature.CONSUME_AMMO))).thenReturn(true);
            flans.when(() -> FlansEquipment.acceptsAmmunition(any(), any())).thenAnswer(call ->
                ((ItemStack)call.getArgument(0)).is(Items.BOW) && ((ItemStack)call.getArgument(1)).is(Items.IRON_NUGGET));
            rounds.when(() -> FlansProjectiles.hasRoundsLeft(any())).thenAnswer(call -> ((ItemStack)call.getArgument(0)).is(Items.IRON_NUGGET));
            npcs.when(NpcAPI::Instance).thenReturn(api);
            assertTrue(NpcAmmoSharing.transfer(donor, ally));
            ItemStack received = ally.inventory.drops.get(1).getMCItemStack();
            assertEquals(1, ammo.getCount());
            assertEquals(1, received.getCount());
            assertEquals(7, received.getTag().getInt("remaining"));
            assertNotSame(ammo, received);
            assertTrue(NpcAmmoSharing.transfer(donor, ally));
            assertFalse(donor.inventory.drops.containsKey(0));
            assertFalse(donor.inventory.dropchance.containsKey(0));
            assertEquals(2, received.getCount());
            assertFalse(NpcAmmoSharing.transfer(donor, ally));
            assertTrue(donor.updateClient);
            assertTrue(ally.updateClient);
        }
    }

    @Test
    void keepsAmmunitionForAnOffhandOrStowedWeaponAndRejectsInfiniteOrFullyStockedRecipients()
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface donor = npc(level);
        EntityNPCInterface ally = npc(level);
        ItemStack gun = new ItemStack(Items.BOW);
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET, 3);
        donor.inventory.drops.put(0, wrapped(ammo));
        when(ally.getMainHandItem()).thenReturn(gun);
        try (var equipment = mockStatic(NpcEquipment.class); var flans = mockStatic(FlansEquipment.class); var rounds = mockStatic(FlansProjectiles.class))
        {
            equipment.when(() -> NpcEquipment.enabled(any(), eq(Feature.CONSUME_AMMO))).thenReturn(true);
            flans.when(() -> FlansEquipment.acceptsAmmunition(any(), any())).thenAnswer(call ->
                ((ItemStack)call.getArgument(0)).is(Items.BOW) && ((ItemStack)call.getArgument(1)).is(Items.IRON_NUGGET));
            rounds.when(() -> FlansProjectiles.hasRoundsLeft(any())).thenAnswer(call -> ((ItemStack)call.getArgument(0)).is(Items.IRON_NUGGET));
            assertSame(ammo, NpcAmmoSharing.spare(donor, ally));
            when(donor.getOffhandItem()).thenReturn(gun);
            assertTrue(NpcAmmoSharing.spare(donor, ally).isEmpty());
            when(donor.getOffhandItem()).thenReturn(ItemStack.EMPTY);
            donor.inventory.drops.put(1, wrapped(gun));
            assertTrue(NpcAmmoSharing.spare(donor, ally).isEmpty());
            donor.inventory.drops.remove(1);
            equipment.when(() -> NpcEquipment.enabled(donor, Feature.CONSUME_AMMO)).thenReturn(false);
            assertTrue(NpcAmmoSharing.spare(donor, ally).isEmpty());
            equipment.when(() -> NpcEquipment.enabled(donor, Feature.CONSUME_AMMO)).thenReturn(true);
            equipment.when(() -> NpcEquipment.enabled(ally, Feature.CONSUME_AMMO)).thenReturn(false);
            assertTrue(NpcAmmoSharing.spare(donor, ally).isEmpty());
            equipment.when(() -> NpcEquipment.enabled(ally, Feature.CONSUME_AMMO)).thenReturn(true);
            ally.inventory.drops.put(0, wrapped(ammo.copyWithCount(2)));
            assertTrue(NpcAmmoSharing.spare(donor, ally).isEmpty());
            assertEquals(3, ammo.getCount());
        }
    }

    @Test
    void fullInventoriesAndSpentMagazinesLeaveBothInventoriesUntouched()
    {
        ServerLevel level = mock(ServerLevel.class);
        EntityNPCInterface donor = npc(level);
        EntityNPCInterface ally = npc(level);
        ItemStack gun = new ItemStack(Items.BOW);
        ItemStack ammo = new ItemStack(Items.IRON_NUGGET);
        donor.inventory.drops.put(0, wrapped(ammo));
        when(ally.getMainHandItem()).thenReturn(gun);
        for (int slot = 0; slot < NpcAmmunition.INVENTORY_SLOTS; slot++)
            ally.inventory.drops.put(slot, wrapped(new ItemStack(Items.STONE, 64)));
        try (var equipment = mockStatic(NpcEquipment.class); var flans = mockStatic(FlansEquipment.class); var rounds = mockStatic(FlansProjectiles.class))
        {
            equipment.when(() -> NpcEquipment.enabled(any(), eq(Feature.CONSUME_AMMO))).thenReturn(true);
            flans.when(() -> FlansEquipment.acceptsAmmunition(gun, ammo)).thenReturn(true);
            rounds.when(() -> FlansProjectiles.hasRoundsLeft(ammo)).thenReturn(true);
            assertFalse(NpcAmmoSharing.transfer(donor, ally));
            assertEquals(1, ammo.getCount());
            ally.inventory.drops.remove(0);
            rounds.when(() -> FlansProjectiles.hasRoundsLeft(ammo)).thenReturn(false);
            assertFalse(NpcAmmoSharing.transfer(donor, ally));
            assertEquals(1, ammo.getCount());
            assertFalse(ally.inventory.drops.containsKey(0));
        }
    }

    private static EntityNPCInterface npc(ServerLevel level)
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        when(npc.level()).thenReturn(level);
        when(npc.isAlive()).thenReturn(true);
        when(npc.getMainHandItem()).thenReturn(ItemStack.EMPTY);
        when(npc.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        return npc;
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }
}
