package com.flansmodultimate.network.server.config;

import com.flansmodultimate.config.ConfigSpecValues;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.config.PacketCommonConfigValues;
import com.flansmodultimate.platform.network.PacketBuffer;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Asks the server for its common config, so the options screen can show what is really in force. */
@NoArgsConstructor
public class PacketRequestCommonConfig implements IServerPacket
{
    @Override
    public void encodeInto(PacketBuffer data)
    {
        // The request carries nothing: the answer is the same for every player but their permission
    }

    @Override
    public void decodeInto(PacketBuffer data)
    {}

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        sendTo(player);
    }

    /** Sends this player the common config values and whether they are allowed to change them. */
    public static void sendTo(ServerPlayer player)
    {
        PacketHandler.sendTo(new PacketCommonConfigValues(ConfigSpecValues.collect(ModCommonConfig.configSpec), PacketSetCommonConfigValue.mayEditCommonConfig(player)), player);
    }
}
