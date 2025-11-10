package com.flansmodultimate.network;

import com.flansmodultimate.ModClient;
import lombok.NoArgsConstructor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

@NoArgsConstructor
public class PacketHitMarker implements IClientPacket
{

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        //no data
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        //no data
    }

    @Override
    public void handleClientSide(LocalPlayer player, ClientLevel level)
    {
        ModClient.setHitMarkerTime(20);
    }
}
