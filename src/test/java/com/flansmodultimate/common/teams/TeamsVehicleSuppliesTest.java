package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.weapons.DriveableAmmoLoader;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TeamsVehicleSuppliesTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void fuelAndAmmunitionCanBeDisabledIndependently()
    {
        DriveableType type = mock(DriveableType.class);
        DriveableData data = mock(DriveableData.class);
        when(type.getFuelTankSize()).thenReturn(1000);
        try (var loader = mockStatic(DriveableAmmoLoader.class))
        {
            TeamsVehicleSupplies.provision(type, data, false, 0);
            verifyNoInteractions(data);
            loader.verifyNoInteractions();
            TeamsVehicleSupplies.provision(type, data, true, 0);
            verify(data).setFuelInTank(1000F);
            loader.verifyNoInteractions();
        }
    }

    @Test
    void loadsWeaponBanksAndSplitsReservesWithoutReplacingCargo()
    {
        DriveableType type = mock(DriveableType.class);
        DriveableData data = mock(DriveableData.class);
        ShootableType ammo = mock(ShootableType.class);
        List<ItemStack> cargo = new java.util.ArrayList<>(List.of(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, ItemStack.EMPTY));
        when(data.getNumCargoSlots()).thenReturn(cargo.size());
        when(data.getMaxStackSize()).thenReturn(16);
        when(data.getCargo(anyInt())).thenAnswer(call -> cargo.get(call.getArgument(0)));
        when(data.canPlaceItem(anyInt(), any())).thenReturn(true);
        doAnswer(call -> cargo.set(call.getArgument(0), call.getArgument(1))).when(data).setCargo(anyInt(), any());
        try (var loader = mockStatic(DriveableAmmoLoader.class); var items = mockStatic(ModUtils.class))
        {
            loader.when(() -> DriveableAmmoLoader.defaultAmmo(type, data)).thenReturn(Set.of(ammo));
            items.when(() -> ModUtils.getItemStack(ammo, 1)).thenReturn(Optional.of(new ItemStack(Items.ARROW)));
            TeamsVehicleSupplies.provision(type, data, false, 32);
            loader.verify(() -> DriveableAmmoLoader.load(type, data, null));
            verify(data, never()).setFuelInTank(anyFloat());
            assertEquals(Items.DIAMOND, cargo.get(0).getItem());
            assertEquals(16, cargo.get(1).getCount());
            assertEquals(16, cargo.get(2).getCount());
        }
    }

    @Test
    void fullCargoStillAllowsWeaponLoading()
    {
        DriveableType type = mock(DriveableType.class);
        DriveableData data = mock(DriveableData.class);
        ShootableType ammo = mock(ShootableType.class);
        when(data.getNumCargoSlots()).thenReturn(1);
        when(data.getCargo(0)).thenReturn(new ItemStack(Items.DIAMOND));
        try (var loader = mockStatic(DriveableAmmoLoader.class); var items = mockStatic(ModUtils.class))
        {
            loader.when(() -> DriveableAmmoLoader.defaultAmmo(type, data)).thenReturn(Set.of(ammo));
            items.when(() -> ModUtils.getItemStack(ammo, 1)).thenReturn(Optional.of(new ItemStack(Items.ARROW)));
            TeamsVehicleSupplies.provision(type, data, false, 32);
            loader.verify(() -> DriveableAmmoLoader.load(type, data, null));
            verify(data, never()).setCargo(anyInt(), any());
        }
    }
}
