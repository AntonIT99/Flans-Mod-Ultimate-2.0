package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModRepositorySource;
import com.flansmodultimate.PackagedContentRepositorySource;
import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import com.flansmodultimate.network.PacketHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.server.packs.PackType;

import java.nio.file.Files;

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
    public static void registerPack(AddPackFindersEvent event)
    {
        event.addRepositorySource(PackagedContentRepositorySource.create(event.getPackType()));

        if (event.getPackType() == PackType.SERVER_DATA && ModApocalypseConfig.apocalypseDimensionDatapackEnabled())
            event.addRepositorySource(ApocalypseDatapackSource.create());

        if (ContentManager.getFlanFolder() != null && Files.exists(ContentManager.getFlanFolder()))
        {
            event.addRepositorySource(new ModRepositorySource(ContentManager.getFlanFolder(), event.getPackType()));
        }
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event)
    {
        if (event.getConfig().getSpec() == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModApocalypseConfig.configSpec)
        {
            ModApocalypseConfig.bake();
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

        if (event.getConfig().getSpec() == ModApocalypseConfig.configSpec)
        {
            ModApocalypseConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }
}
