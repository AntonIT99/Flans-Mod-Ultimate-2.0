package com.flansmodultimate.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

/** A packet view over the loader's buffer; it shares the source's reader and writer indices. */
public final class PacketBuffer extends RegistryFriendlyByteBuf
{
    public PacketBuffer(RegistryFriendlyByteBuf source)
    {
        super(source, source.registryAccess());
    }
}
