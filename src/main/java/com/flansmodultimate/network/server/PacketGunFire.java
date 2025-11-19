package com.flansmodultimate.network.server;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunFire implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunFire(InteractionHand hand)
    {
        this.hand = hand;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleServerSide(ServerPlayer player)
    {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!itemstack.isEmpty() && itemstack.getItem() instanceof GunItem gun)
        {
            gun.getBehavior().shootServer(hand, player, itemstack);
        }
        else
        {
            FlansMod.log.warn("Received invalid PacketGunFire. Item in hand is not an instance of {}", GunItem.class.getSimpleName());
        }
    }
}
