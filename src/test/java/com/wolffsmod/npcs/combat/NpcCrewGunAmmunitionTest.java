package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.wolffsmod.npcs.crew.NpcCrew;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcCrewGunAmmunitionTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void seatGunsPreferVehicleThenGunnerThenDefaultWithoutUsingUpInventory()
    {
        EntityNPCInterface vehicle = mock(EntityNPCInterface.class);
        EntityNPCInterface gunner = mock(EntityNPCInterface.class);
        vehicle.inventory = new DataInventory(vehicle);
        gunner.inventory = new DataInventory(gunner);
        IContentType gun = mock(IContentType.class);
        when(gun.getItem()).thenReturn(Items.STICK);
        ItemStack vehicleAmmo = new ItemStack(Items.IRON_NUGGET, 3);
        ItemStack gunnerAmmo = new ItemStack(Items.GOLD_NUGGET, 4);
        ItemStack fallback = new ItemStack(Items.DIAMOND);
        vehicle.inventory.drops.put(2, wrapped(vehicleAmmo));
        vehicle.inventory.weapons.put(1, wrapped(new ItemStack(Items.APPLE)));
        gunner.inventory.weapons.put(1, wrapped(gunnerAmmo));
        try (var equipment = mockStatic(FlansEquipment.class))
        {
            equipment.when(() -> FlansEquipment.acceptsAmmunition(any(), any())).thenAnswer(call ->
            {
                ItemStack ammo = call.getArgument(1);
                return ammo.is(Items.IRON_NUGGET) || ammo.is(Items.GOLD_NUGGET);
            });
            equipment.when(() -> FlansEquipment.createDefaultAmmunition(gun)).thenReturn(Optional.of(fallback));
            assertSame(vehicleAmmo, NpcAmmunition.crewGunTemplate(vehicle, gunner, gun));
            vehicle.inventory.drops.clear();
            assertSame(gunnerAmmo, NpcAmmunition.crewGunTemplate(vehicle, gunner, gun));
            gunner.inventory.weapons.clear();
            gunner.inventory.drops.put(4, wrapped(gunnerAmmo));
            assertSame(gunnerAmmo, NpcAmmunition.crewGunTemplate(vehicle, gunner, gun));
            gunner.inventory.drops.clear();
            assertSame(fallback, NpcAmmunition.crewGunTemplate(vehicle, gunner, gun));
            equipment.when(() -> FlansEquipment.createDefaultAmmunition(gun)).thenReturn(Optional.empty());
            assertTrue(NpcAmmunition.crewGunTemplate(vehicle, gunner, gun).isEmpty());
            assertEquals(3, vehicleAmmo.getCount());
            assertEquals(4, gunnerAmmo.getCount());
        }
    }

    @Test
    void crewHoldingFireCannotUseEquippedWeaponsOrThrowWithARetainedTarget()
    {
        EntityNPCInterface npc = mock(EntityNPCInterface.class);
        ServerLevel level = mock(ServerLevel.class);
        when(npc.level()).thenReturn(level);
        LivingEntity target = mock(LivingEntity.class);
        when(npc.getTarget()).thenReturn(target);
        try (var crew = mockStatic(NpcCrew.class))
        {
            crew.when(() -> NpcCrew.holdsFire(npc)).thenReturn(true);
            assertFalse(NpcEquipmentRangedGoal.engages(npc));
            assertFalse(NpcItemAttacks.ranged(npc, target));
            assertFalse(NpcItemAttacks.throwCarried(npc, target, new ItemStack(Items.SNOWBALL)));
        }
    }

    private static IItemStack wrapped(ItemStack stack)
    {
        IItemStack wrapper = mock(IItemStack.class);
        when(wrapper.getMCItemStack()).thenReturn(stack);
        return wrapper;
    }
}
