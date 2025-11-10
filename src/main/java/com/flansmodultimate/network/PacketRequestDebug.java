package com.flansmodultimate.network;

import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
public class PacketRequestDebug implements IServerPacket
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
            PacketHandler.sendTo(new PacketAllowDebug(), player);
    }
}
