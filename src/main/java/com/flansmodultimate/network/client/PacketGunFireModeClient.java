package com.flansmodultimate.network.client;

import com.flansmodultimate.common.guns.EnumFireMode;
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
public class PacketGunFireModeClient implements IClientPacket
{
    private InteractionHand hand;
    private EnumFireMode mode;

    public PacketGunFireModeClient(InteractionHand hand, EnumFireMode mode)
    {
        this.hand = hand;
        this.mode = mode;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
        data.writeEnum(mode);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        mode = data.readEnum(EnumFireMode.class);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            gunItem.getConfigType().setFireMode(stack, mode);
    }
}
