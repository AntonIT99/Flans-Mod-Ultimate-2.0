package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.weapons.DriveableAmmoLoader;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.ItemStack;

/** Supplies a copied spawner template before entity creation, so fuel and inventories initialize together. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamsVehicleSupplies
{
    public static void provision(DriveableType type, DriveableData data, boolean fuel, int spareItems)
    {
        if (fuel)
            data.setFuelInTank(type.getFuelTankSize());
        if (spareItems <= 0)
            return;
        DriveableAmmoLoader.load(type, data, null);
        for (ShootableType ammo : DriveableAmmoLoader.defaultAmmo(type, data))
            supplyCargo(data, ammo, spareItems);
    }

    private static void supplyCargo(DriveableData data, ShootableType ammo, int spareItems)
    {
        ItemStack template = ModUtils.getItemStack(ammo, 1).orElse(ItemStack.EMPTY);
        if (template.isEmpty())
            return;
        if (ammo.getRoundsPerItem() > 1)
            ShootableItem.setRoundsRemaining(template, ammo.getRoundsPerItem());
        int remaining = spareItems;
        for (int slot = 0; slot < data.getNumCargoSlots() && remaining > 0; slot++)
        {
            if (!data.getCargo(slot).isEmpty())
                continue;
            ItemStack supply = template.copyWithCount(Math.min(remaining, Math.min(template.getMaxStackSize(), data.getMaxStackSize())));
            if (data.canPlaceItem(data.getCargoInventoryStart() + slot, supply))
            {
                data.setCargo(slot, supply);
                remaining -= supply.getCount();
            }
        }
    }
}
