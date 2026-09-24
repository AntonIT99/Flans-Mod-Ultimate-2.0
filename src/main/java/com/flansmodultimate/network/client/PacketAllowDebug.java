package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import org.jetbrains.annotations.NotNull;

import com.flansmodultimate.network.PacketBuffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketAllowDebug implements IClientPacket
{
    @Override
    public void encodeInto(PacketBuffer data)
    {
        // No data
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {
        // No data
    }

    @Override
    public void handleClientSide(@NotNull Player player, @NotNull Level level)
    {
        ClientHooks.RENDER.setDebugMode(true);
    }
}
