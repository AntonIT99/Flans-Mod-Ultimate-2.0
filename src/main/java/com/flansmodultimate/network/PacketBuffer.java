package com.flansmodultimate.network;

import net.minecraft.network.FriendlyByteBuf;

/** A packet view over the loader's buffer; it shares the source's reader and writer indices. */
public final class PacketBuffer extends FriendlyByteBuf
{
    public PacketBuffer(FriendlyByteBuf source)
    {
        super(source);
    }
}
