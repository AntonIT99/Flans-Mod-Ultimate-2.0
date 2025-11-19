package com.flansmodultimate.network.client;

import com.flansmodultimate.ModClient;
import com.flansmodultimate.network.IClientPacket;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

public class PacketAllowDebug implements IClientPacket
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
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        ModClient.setDebug(true);
    }
}
