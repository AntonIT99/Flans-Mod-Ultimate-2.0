package com.flansmodultimate.network;

import com.flansmodultimate.platform.network.PacketBuffer;

public interface IPacket
{
    /** Encode the packet into the buffer. */
    void encodeInto(PacketBuffer data);

    /** Decode the packet from the buffer. */
    void decodeInto(PacketBuffer data);
}
