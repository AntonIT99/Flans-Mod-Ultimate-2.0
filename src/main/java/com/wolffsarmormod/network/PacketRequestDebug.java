package com.wolffsarmormod.network;

import com.wolffsarmormod.ModClient;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDebug extends PacketBase
{
    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void handleServerSide(ServerPlayer player)
    {
        if (player.hasPermissions(2))
            PacketHandler.sendTo(new PacketRequestDebug(), player);
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        ModClient.setDebug(true);
    }
}
