package com.flansmodultimate.network;

public interface IPacket
{
    /** Encode the packet into the buffer. */
    void encodeInto(PacketBuffer data);

    /** Decode the packet from the buffer. */
    void decodeInto(PacketBuffer data);
}
