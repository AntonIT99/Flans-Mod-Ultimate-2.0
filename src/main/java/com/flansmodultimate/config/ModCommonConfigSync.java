package com.flansmodultimate.config;

import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.server.MinecraftServer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfigSync
{
    public static void resyncAllClientsIfServer()
    {
        if (FMLEnvironment.dist.isClient())
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        CommonConfigSnapshot snap = CommonConfigSnapshot.from(ModCommonConfig.get());
        PacketSyncCommonConfig pkt  = new PacketSyncCommonConfig(snap);

        for (var sp : server.getPlayerList().getPlayers())
            com.flansmodultimate.network.PacketHandler.sendTo(pkt, sp);
    }
}

