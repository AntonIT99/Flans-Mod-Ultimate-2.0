package com.flansmodultimate.network.client;

import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

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
