package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketAllowDebug implements IClientPacket
{
    @Override
    public void encodeInto(RegistryFriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void decodeInto(RegistryFriendlyByteBuf data)
    {
        // No data
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.setDebugMode(true);
    }
}
