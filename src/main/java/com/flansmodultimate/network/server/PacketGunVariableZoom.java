package com.flansmodultimate.network.server;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunVariableZoomClient;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunVariableZoom implements IServerPacket
{
    private InteractionHand hand;
    private boolean increase;

    public PacketGunVariableZoom(InteractionHand hand, boolean increase)
    {
        this.hand = hand;
        this.increase = increase;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
        data.writeBoolean(increase);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        increase = data.readBoolean();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem)
            || !gunItem.hasVariableZoom(stack))
            return;

        float zoom = gunItem.changeVariableZoom(stack, increase);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunVariableZoomClient(hand, zoom), player);
    }
}
