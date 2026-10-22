package com.flansmodultimate.platform.network;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Packet buffers for plain JUnit round trips: 1.21.1 packets use registry-aware buffers, here with empty registries. */
public final class TestPacketBuffers
{
    private TestPacketBuffers()
    {}

    /** An empty, growable buffer. */
    // Plain JUnit has no connection-specific registry decorator; empty registries cover these payloads.
    @SuppressWarnings("deprecation")
    public static RegistryFriendlyByteBuf buffer()
    {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    /** A buffer reading the given bytes. */
    // Plain JUnit has no connection-specific registry decorator; empty registries cover these payloads.
    @SuppressWarnings("deprecation")
    public static RegistryFriendlyByteBuf wrap(byte[] bytes)
    {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), RegistryAccess.EMPTY);
    }
}
