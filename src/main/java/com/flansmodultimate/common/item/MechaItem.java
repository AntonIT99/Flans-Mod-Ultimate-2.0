package com.flansmodultimate.common.item;

import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.types.MechaType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MechaItem extends DriveableItem<MechaType, Mecha>
{
    public MechaItem(MechaType configType, Properties properties)
    {
        super(configType, properties);
    }

    @Override
    protected Mecha createDriveable(Level level, double x, double y, double z, float yaw, @Nullable Player placer, ItemStack sourceStack)
    {
        return new Mecha(level, configType, x, y, z, yaw, placer, sourceStack);
    }
}
