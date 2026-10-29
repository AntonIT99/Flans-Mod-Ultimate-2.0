package com.wolffsmod.npcs.platform.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/**
 * Version boundary for the values module packets write differently across versions. Kept apart from
 * {@link NpcNetworkPlatform}, so encoding a packet does not set up the loader's channel.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPacketIO
{
    /** Writes a text component into a packet. */
    public static void writeComponent(FriendlyByteBuf buffer, Component component)
    {
        buffer.writeComponent(component);
    }

    /** Reads a text component written by {@link #writeComponent}. */
    public static Component readComponent(FriendlyByteBuf buffer)
    {
        return buffer.readComponent();
    }
}
