package com.flansmodultimate.network.server;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunFireModeClient;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunFireMode implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunFireMode(InteractionHand hand)
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
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();
        EnumFireMode previousMode = gunType.getFireMode(stack);
        EnumFireMode nextMode = gunType.cycleFireMode(stack);
        if (previousMode == nextMode)
            return;

        PlayerData.getInstance(player).setBurstRoundsRemaining(hand, 0);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunFireModeClient(hand, nextMode), player);
    }
}
