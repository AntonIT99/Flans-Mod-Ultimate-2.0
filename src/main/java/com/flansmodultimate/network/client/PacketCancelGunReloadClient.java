package com.flansmodultimate.network.client;

import com.flansmod.client.model.GunAnimations;
import com.flansmodultimate.ModClient;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

@NoArgsConstructor
public class PacketCancelGunReloadClient implements IClientPacket
{
    private InteractionHand hand;

    public PacketCancelGunReloadClient(InteractionHand hand)
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
    public void handleClientSide(@NotNull LocalPlayer player, @NotNull ClientLevel level)
    {
        PlayerData data = PlayerData.getInstance(player, LogicalSide.CLIENT);
        data.setShootTimeRight(0);
        data.setShootTimeLeft(0);
        data.setReloading(hand, false);

        GunAnimations animations = ModClient.getGunAnimations(player, hand);
        animations.cancelReload();
    }
}
