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
 * Sent when the client detects a weapon switch, so that the switch delay blocking the shot is applied server side too.
 * The delay itself is read from the held gun server side, the client only reports which hand switched.
 */
@NoArgsConstructor
public class PacketGunSwitchDelay implements IServerPacket
{
    private InteractionHand hand;

    public PacketGunSwitchDelay(InteractionHand hand)
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
        ItemStack gunStack = player.getItemInHand(hand);
        if (!(gunStack.getItem() instanceof GunItem gunItem))
            return;

        float switchDelay = gunItem.getConfigType().getSwitchDelay();
        if (switchDelay <= 0F)
            return;

        PlayerData data = PlayerData.getInstance(player, LogicalSide.SERVER);
        data.setShootTime(hand, Math.max(data.getShootTime(hand), switchDelay));
    }
}
