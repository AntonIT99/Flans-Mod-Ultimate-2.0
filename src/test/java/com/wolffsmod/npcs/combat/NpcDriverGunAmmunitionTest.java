package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import noppes.npcs.entity.data.DataStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcDriverGunAmmunitionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void eachGunPrefersCompatibleProjectileThenInventoryOrderThenItsOwnDefault()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        npc.inventory = new DataInventory(npc);
        npc.stats = mock(DataStats.class, withSettings().extraInterfaces(NpcWeaponSettings.class));
        when(((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions()).thenReturn(new NpcWeaponOptions());
        IContentType firstGun = mock(IContentType.class);
        IContentType secondGun = mock(IContentType.class);
        when(firstGun.getItem()).thenReturn(Items.STICK);
        when(secondGun.getItem()).thenReturn(Items.BLAZE_ROD);
        ItemStack projectile = new ItemStack(Items.IRON_NUGGET, 3);
        ItemStack firstAmmo = new ItemStack(Items.GOLD_NUGGET, 4);
        ItemStack laterAmmo = new ItemStack(Items.GOLD_NUGGET, 5);
        ItemStack fallback = new ItemStack(Items.DIAMOND);
        npc.inventory.weapons.put(1, wrapped(projectile));
        npc.inventory.drops.put(5, wrapped(laterAmmo));
        npc.inventory.drops.put(2, wrapped(firstAmmo));
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.acceptsAmmunition(any(), any())).thenAnswer(NpcDriverGunAmmunitionTest::fits);
            equipment.when(() -> FlansEquipment.acceptsAmmunition(any(), any(), anyBoolean())).thenAnswer(NpcDriverGunAmmunitionTest::fits);
            equipment.when(() -> FlansEquipment.createDefaultAmmunitionChoices(firstGun)).thenReturn(List.of(fallback));
            equipment.when(() -> FlansEquipment.createDefaultAmmunitionChoices(secondGun)).thenReturn(List.of());
            assertSame(projectile, NpcAmmunition.driverGunTemplate(npc, firstGun));
            assertSame(firstAmmo, NpcAmmunition.driverGunTemplate(npc, secondGun));
            npc.inventory.weapons.clear();
            assertSame(fallback, NpcAmmunition.driverGunTemplate(npc, firstGun));
            npc.inventory.drops.clear();
            assertTrue(NpcAmmunition.driverGunTemplate(npc, secondGun).isEmpty());
            assertEquals(3, projectile.getCount());
            assertEquals(4, firstAmmo.getCount());
            assertEquals(5, laterAmmo.getCount());
        }
    }

    /** @return whether the mocked weapon of the call takes its ammunition, whichever overload asked */
    private static boolean fits(InvocationOnMock call)
    {
        ItemStack weapon = call.getArgument(0);
        ItemStack ammo = call.getArgument(1);
        return weapon.is(Items.STICK) && ammo.is(Items.IRON_NUGGET) || weapon.is(Items.BLAZE_ROD) && ammo.is(Items.GOLD_NUGGET);
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }
}
