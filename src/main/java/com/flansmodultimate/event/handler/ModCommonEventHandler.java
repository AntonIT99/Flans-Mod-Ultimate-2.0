package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import com.flansmodultimate.network.PacketHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event)
    {
        PacketHandler.registerPackets();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event)
    {
        if (event.getConfig().getSpec() == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event)
    {
        if (event.getConfig().getSpec() == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }
}
