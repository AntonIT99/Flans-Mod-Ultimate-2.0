package com.flansmodultimate.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface IPacket extends CustomPacketPayload
{
    void encodeInto(FriendlyByteBuf buf);

    void decodeInto(FriendlyByteBuf buf);
}