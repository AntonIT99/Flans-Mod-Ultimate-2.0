package com.flansmodultimate.platform.event;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.common.recipe.GunpowderRecipeCondition;
import com.flansmodultimate.config.ConfigBaking;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.content.ModRepositorySource;
import com.flansmodultimate.content.PackagedContentRepositorySource;
import com.flansmodultimate.content.SoundPriority;
import com.flansmodultimate.platform.network.NetworkPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.server.packs.PackType;

import java.nio.file.Files;

/**
 * Forge mod-bus lifecycle subscribers: common setup, pack sources in loader insertion order and config baking.
 * NeoForge registers payloads, recipe conditions and capabilities through dedicated events instead of common setup.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CommonModEvents
{
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event)
    {
        NetworkPlatform.register();
        event.enqueueWork(() -> CraftingHelper.register(GunpowderRecipeCondition.SERIALIZER));
    }

    @SubscribeEvent
    public static void registerPack(AddPackFindersEvent event)
    {
        event.addRepositorySource(PackagedContentRepositorySource.create(event.getPackType()));
        if (event.getPackType() == PackType.CLIENT_RESOURCES)
            event.addRepositorySource(SoundPriority.repositorySource());

        if (event.getPackType() == PackType.SERVER_DATA && ModApocalypseConfig.apocalypseDimensionDatapackEnabled())
            event.addRepositorySource(ApocalypseDatapackSource.create());

        if (ContentManager.getFlanFolder() != null && Files.exists(ContentManager.getFlanFolder()))
            event.addRepositorySource(new ModRepositorySource(ContentManager.getFlanFolder(), event.getPackType()));
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event)
    {
        ConfigBaking.onConfigLoaded(event.getConfig().getSpec());
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event)
    {
        ConfigBaking.onConfigLoaded(event.getConfig().getSpec());
    }
}
