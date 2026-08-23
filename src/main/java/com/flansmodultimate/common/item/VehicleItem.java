package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.types.VehicleType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class VehicleItem extends DriveableItem<VehicleType, Vehicle>
{
    public VehicleItem(VehicleType configType, Properties properties)
    {
        super(configType, properties);
    }

    @Override
    protected Vehicle createDriveable(Level level, double x, double y, double z, float yaw, @Nullable Player placer, ItemStack sourceStack)
    {
        return new Vehicle(level, configType, x, y, z, yaw, placer, sourceStack);
    }
}
