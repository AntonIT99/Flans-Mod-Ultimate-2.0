package com.flansmodultimate.network.server;

import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketGunPreferredAmmoClient;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor
public class PacketGunPreferredAmmo implements IServerPacket
{
    private InteractionHand hand;
    private String ammoName;

    public PacketGunPreferredAmmo(InteractionHand hand, String ammoName)
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
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
            return;

        boolean allowed = gunItem.getConfigType().getAmmoTypes().stream()
            .anyMatch(type -> type.getOriginalShortName().equals(ammoName));
        if (!allowed)
            return;

        gunItem.setPreferredAmmo(stack, ammoName);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketHandler.sendTo(new PacketGunPreferredAmmoClient(hand, ammoName), player);
    }
}
