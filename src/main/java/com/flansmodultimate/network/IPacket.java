package com.flansmodultimate.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IPacket
{
    /** Encode the packet into the buffer. */
    void encodeInto(RegistryFriendlyByteBuf data);

    /** Decode the packet from the buffer. */
    void decodeInto(RegistryFriendlyByteBuf data);
}
