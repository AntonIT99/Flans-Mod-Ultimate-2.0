package com.flansmodultimate.platform.network;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;

/** Packet buffers for plain JUnit round trips: 1.20.1 packets use the plain buffer type. */
public final class TestPacketBuffers
{
    private TestPacketBuffers()
    {}

    /** An empty, growable buffer. */
    public static FriendlyByteBuf buffer()
    {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    /** A buffer reading the given bytes. */
    public static FriendlyByteBuf wrap(byte[] bytes)
    {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
    }
}
