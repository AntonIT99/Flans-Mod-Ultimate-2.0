package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModRepositorySource;
import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;

import net.minecraft.server.packs.PackType;

import java.nio.file.Files;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = FlansMod.MOD_ID)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FlansMod.itemHolderBlockEntity.get(),
            (blockEntity, direction) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FlansMod.paintjobTableBlockEntity.get(),
            (blockEntity, direction) -> blockEntity.getItemHandler());
    }

    @SubscribeEvent
    public static void registerPack(AddPackFindersEvent event)
    {
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
