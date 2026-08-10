package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@NoArgsConstructor
public class PacketCancelGunReloadClient implements IClientPacket
{
    private InteractionHand hand;

    public PacketCancelGunReloadClient(InteractionHand hand)
    {
        this.hand = hand;
    }

    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        data.writeEnum(hand);
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        hand = data.readEnum(InteractionHand.class);
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.GUN.cancelReloadGunItem(player, hand);
    }
}
