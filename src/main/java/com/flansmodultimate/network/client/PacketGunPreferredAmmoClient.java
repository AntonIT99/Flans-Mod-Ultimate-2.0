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
public class PacketGunPreferredAmmoClient implements IClientPacket
{
    private InteractionHand hand;
    private String ammoName;

    public PacketGunPreferredAmmoClient(InteractionHand hand, String ammoName)
    {
        this.hand = hand;
        this.ammoName = ammoName;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeEnum(hand);
        data.writeUtf(ammoName, 128);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
        ammoName = data.readUtf(128);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem)
            gunItem.setPreferredAmmo(stack, ammoName);
    }
}
