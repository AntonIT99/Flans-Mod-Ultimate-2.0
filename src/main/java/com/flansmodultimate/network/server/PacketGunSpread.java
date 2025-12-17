package com.flansmodultimate.network.server;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunSpread implements IServerPacket
{
    private float spread;

    public PacketGunSpread(ItemStack stack, float amount)
    {
        if (stack != null && stack.getItem() instanceof GunItem)
            spread = amount;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeFloat(spread);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        spread = data.readFloat();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getInventory().getSelected();
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            GunType type = gunItem.getConfigType();

            if (type.getGrip(stack) != null && type.getSecondaryFire(stack))
                type.getGrip(stack).setSecondarySpread(spread);
            else
                type.setBulletSpread(spread);
        }
    }
}
