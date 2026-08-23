package com.flansmodultimate.config;

import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfigSync
{
    public static void resyncAllClientsIfServer()
    {
        if (FMLEnvironment.getDist().isClient())
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

    private static PacketSyncCommonConfig createSyncPacket()
    {
        CommonConfigSnapshot commonConfig = ModCommonConfig.get();
        ApocalypseConfigSnapshot apocalypseConfig = ModApocalypseConfig.get();
        if (commonConfig == null || apocalypseConfig == null)
            return null;

        return new PacketSyncCommonConfig(commonConfig, apocalypseConfig);
    }
}

