package com.flansmodultimate.network.client;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketGunVariableZoomClient implements IClientPacket
{
    private InteractionHand hand;
    private float zoom;

    public PacketGunVariableZoomClient(InteractionHand hand, float zoom)
    {
        this.hand = hand;
        this.zoom = zoom;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
        data.writeFloat(zoom);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        zoom = data.readFloat();
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            gunItem.setCurrentVariableZoom(stack, zoom);
    }
}
