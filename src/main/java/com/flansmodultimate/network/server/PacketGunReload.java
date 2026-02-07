package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * This packet is send by the client to request a reload. The server checks if the player can reload and in this case actually reloads and sends a GunAnimationPacket as response.
 * The GunAnimationPacket plays the reload animation and sets the pumpDelay & pumpTime times to prevent the client from shooting while reloading
 */
@NoArgsConstructor
public class PacketGunReload implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunReload(InteractionHand hand)
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
        {
            PlayerData data = PlayerData.getInstance(player, LogicalSide.SERVER);
            gunItem.getGunItemHandler().doPlayerReload(level, player, data, stack, hand, true);
        }
    }
}