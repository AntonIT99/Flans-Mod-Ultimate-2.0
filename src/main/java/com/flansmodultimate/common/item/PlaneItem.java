package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.types.PlaneType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PlaneItem extends DriveableItem<PlaneType, Plane>
{
    public PlaneItem(PlaneType configType)
    {
        super(configType);
    }

    @Override
    protected Plane createDriveable(Level level, double x, double y, double z, float yaw, @Nullable Player placer, ItemStack sourceStack)
    {
        return new Plane(level, configType, x, y, z, yaw, placer, sourceStack);
    }
}
