package com.wolffsarmormod.network;

import net.minecraft.network.FriendlyByteBuf;

public interface IPacket
{
    /** Encode the packet into the buffer. */
    void encodeInto(FriendlyByteBuf data);

    /** Decode the packet from the buffer. */
    void decodeInto(FriendlyByteBuf data);
}
