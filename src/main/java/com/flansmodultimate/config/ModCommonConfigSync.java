package com.flansmodultimate.config;

import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketCommonConfigValues;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import com.flansmodultimate.network.server.PacketSetCommonConfigValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfigSync
{
    public static void resyncAllClientsIfServer()
    {
        if (PlatformEnvironment.isClient())
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        PacketSyncCommonConfig packet = createSyncPacket();
        if (packet != null)
            PacketHandler.sendToAll(packet);
    }

    public static void syncClientIfServer(ServerPlayer player)
    {
        PacketSyncCommonConfig packet = createSyncPacket();
        if (packet != null)
            PacketHandler.sendTo(packet, player);
    }

    /**
     * Sends every player the raw common config values an open options screen shows, each with their own
     * permission to change them. The baked snapshot above only carries the settings the game itself uses.
     */
    public static void resyncCommonConfigValuesIfServer()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        Map<String, Object> values = ConfigSpecValues.collect(ModCommonConfig.configSpec);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            PacketHandler.sendTo(new PacketCommonConfigValues(values, PacketSetCommonConfigValue.mayEditCommonConfig(player)), player);
    }

    private static PacketSyncCommonConfig createSyncPacket()
    {
        CommonConfigSnapshot commonConfig = ModCommonConfig.get();
        ApocalypseConfigSnapshot apocalypseConfig = ModApocalypseConfig.get();
        if (commonConfig == null || apocalypseConfig == null)
            return null;

        return new PacketSyncCommonConfig(commonConfig, apocalypseConfig);
    }
}

