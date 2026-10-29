package com.wolffsmod.npcs.platform.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

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
        ComponentSerialization.STREAM_CODEC.encode(registryBuffer(buffer), component);
    }

    /** Reads a text component written by {@link #writeComponent}. */
    public static Component readComponent(FriendlyByteBuf buffer)
    {
        return ComponentSerialization.STREAM_CODEC.decode(registryBuffer(buffer));
    }

    /** Minecraft 1.21 encodes components with registry context; play packets already carry it. */
    private static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buffer)
    {
        return buffer instanceof RegistryFriendlyByteBuf registries ? registries : new RegistryFriendlyByteBuf(buffer, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }
}
