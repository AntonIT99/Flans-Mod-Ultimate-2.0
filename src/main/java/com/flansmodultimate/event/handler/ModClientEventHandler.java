package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.gui.ArmorBoxScreen;
import com.flansmodultimate.client.gui.DriveableCraftingScreen;
import com.flansmodultimate.client.gui.DriveableInventoryScreen;
import com.flansmodultimate.client.gui.GunBoxScreen;
import com.flansmodultimate.client.gui.GunWorkbenchScreen;
import com.flansmodultimate.client.gui.PaintjobTableScreen;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.model.BewlrRoutingModel;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.particle.AfterburnParticle;
import com.flansmodultimate.client.particle.BigSmokeParticle;
import com.flansmodultimate.client.particle.Debris1Particle;
import com.flansmodultimate.client.particle.FlareParticle;
import com.flansmodultimate.client.particle.FlashParticle;
import com.flansmodultimate.client.particle.FmFlameParticle;
import com.flansmodultimate.client.particle.FmMuzzleFlashParticle;
import com.flansmodultimate.client.particle.FmSmokeParticle;
import com.flansmodultimate.client.particle.FmTracerParticle;
import com.flansmodultimate.client.particle.LegacyExplodeParticle;
import com.flansmodultimate.client.particle.RocketExhaustParticle;
import com.flansmodultimate.client.particle.SmokeBurstParticle;
import com.flansmodultimate.client.particle.SmokeGrenadeParticle;
import com.flansmodultimate.client.render.ClientHudOverlays;
import com.flansmodultimate.client.render.CustomArmorLayer;
import com.flansmodultimate.client.render.blockentity.ItemHolderRenderer;
import com.flansmodultimate.client.render.entity.AAGunRenderer;
import com.flansmodultimate.client.render.entity.BulletRenderer;
import com.flansmodultimate.client.render.entity.DeployableGunRenderer;
import com.flansmodultimate.client.render.entity.DriveableRenderer;
import com.flansmodultimate.client.render.entity.GrenadeRenderer;
import com.flansmodultimate.client.render.entity.InvisibleEntityRenderer;
import com.flansmodultimate.client.render.entity.ParachuteRenderer;
import com.flansmodultimate.client.render.entity.TeamObjectRenderer;
import com.flansmodultimate.client.render.item.CustomItemRenderers;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.platform.item.ItemStackData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
public final class ModClientEventHandler
{
    private static boolean isSoundEngineInitialized;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            CustomItemRenderers.registerAll();

            // Paintjob registrations
            for (DeferredHolder<Item, ? extends Item> item : FlansMod.getItems())
            {
                if (item.get() instanceof IPaintableItem<?>)
                {
                    ItemProperties.register(item.get(), FlansMod.paintjob, (stack, level, entity, seed) -> {
                        CompoundTag tag = ItemStackData.copy(stack);
                        return tag.contains(IPaintableItem.NBT_PAINTJOB_ID) ? tag.getInt(IPaintableItem.NBT_PAINTJOB_ID) : 0;
                    });
                }
            }
            ItemProperties.register(FlansMod.opStick.get(), ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "teams_mode"),
                (stack, level, entity, seed) -> ItemOpStick.getMode(stack).ordinal());

        });
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(FlansMod.gunWorkbenchMenu.get(), GunWorkbenchScreen::new);
        event.register(FlansMod.driveableCraftingMenu.get(), DriveableCraftingScreen::new);
        event.register(FlansMod.driveableInventoryMenu.get(), DriveableInventoryScreen::new);
        event.register(FlansMod.paintjobTableMenu.get(), PaintjobTableScreen::new);
        event.register(FlansMod.armorBoxMenu.get(), ArmorBoxScreen::new);
        event.register(FlansMod.gunBoxMenu.get(), GunBoxScreen::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event)
    {
        Item[] customRenderedItems = FlansMod.getItems().stream()
            .map(DeferredHolder::get)
            .filter(ICustomRendereredItem.class::isInstance)
            .toArray(Item[]::new);
        if (customRenderedItems.length > 0)
            event.registerItem(ClientHooks.RENDER.customItemExtensions(), customRenderedItems);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event)
    {
        Set<ResourceLocation> customRenderedItemIds = FlansMod.getItems().stream()
            .filter(itemRegistryObject -> itemRegistryObject.get() instanceof ICustomRendereredItem<?>)
            .map(DeferredHolder::getId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());

        // Wrap all variants in one pass. Large legacy installations can have
        // thousands of registered Flan items, so one full map scan per item is
        // prohibitively expensive during every resource reload.
        event.getModels().replaceAll((location, original) -> {
            if (customRenderedItemIds.contains(location.id()) && !(original instanceof BewlrRoutingModel))
                return new BewlrRoutingModel(original);
            return original;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerArmorLayer(EntityRenderersEvent.AddLayers event)
    {
        for (var skin : event.getSkins())
        {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new CustomArmorLayer<>(playerRenderer));
            }
        }

        for (EntityType<?> entityType : event.getEntityTypes())
        {
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            EntityRenderer<? extends LivingEntity> renderer = event.getRenderer(livingType);

            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer && livingRenderer.getModel() instanceof HumanoidModel<?>)
            {
                livingRenderer.addLayer(new CustomArmorLayer<>((RenderLayerParent) livingRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(FlansMod.bulletEntity.get(), BulletRenderer::new);
        event.registerEntityRenderer(FlansMod.grenadeEntity.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(FlansMod.deployedGunEntity.get(), DeployableGunRenderer::new);
        event.registerEntityRenderer(FlansMod.aaGunEntity.get(), AAGunRenderer::new);
        event.registerEntityRenderer(FlansMod.parachuteEntity.get(), ParachuteRenderer::new);
        event.registerEntityRenderer(FlansMod.planeEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansMod.vehicleEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansMod.mechaEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansMod.seatEntity.get(), InvisibleEntityRenderer::new);
        event.registerEntityRenderer(FlansMod.wheelEntity.get(), InvisibleEntityRenderer::new);
        event.registerEntityRenderer(FlansMod.flagpoleEntity.get(), TeamObjectRenderer::new);
        event.registerEntityRenderer(FlansMod.flagEntity.get(), TeamObjectRenderer::new);
        event.registerBlockEntityRenderer(FlansMod.itemHolderBlockEntity.get(), ItemHolderRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "scope"), ClientHudOverlays.SCOPE);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "armor"), ClientHudOverlays.ARMOR);
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "damage_absorption"), ClientHudOverlays.DAMAGE_ABSORPTION);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "hud"), ClientHudOverlays.HUD);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(FlansMod.afterburnParticle.get(), AfterburnParticle.Provider::new);
        event.registerSpriteSet(FlansMod.bigSmokeParticle.get(), BigSmokeParticle.Provider::new);
        event.registerSpriteSet(FlansMod.debris1Particle.get(), Debris1Particle.Provider::new);
        event.registerSpriteSet(FlansMod.explodeParticle.get(), LegacyExplodeParticle.Provider::new);
        event.registerSpriteSet(FlansMod.flareParticle.get(), FlareParticle.Provider::new);
        event.registerSpriteSet(FlansMod.flashParticle.get(), FlashParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmFlameParticle.get(), FmFlameParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmMuzzleFlashParticle.get(), FmMuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmSmokeParticle.get(), FmSmokeParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmTracerParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmTracerGreenParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansMod.fmTracerRedParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansMod.rocketExhaustParticle.get(), RocketExhaustParticle.Provider::new);
        event.registerSpriteSet(FlansMod.smokeBurstParticle.get(), SmokeBurstParticle.Provider::new);
        event.registerSpriteSet(FlansMod.smokeGrenadeParticle.get(), SmokeGrenadeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
        event.register((stack, tintIndex) -> {
            Item item = stack.getItem();
            if (item instanceof IFlanItem<?> flanItem)
                // Legacy content packs store colours as 24-bit RGB. Since
                // 1.21 the item renderer consumes ARGB and therefore treated
                // the missing high byte as alpha=0, making every tinted Flan
                // item completely transparent in every render context.
                return 0xFF000000 | flanItem.getConfigType().getColour();
            return 0xFFFFFFFF;
        },
        FlansMod.getItems().stream()
            .map(DeferredHolder::get)
            .toArray(Item[]::new)
        );
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        KeyInputHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void onClientReload(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> {
            ModelCache.reload();
            ContentManager.logMissingModelTextures(rm);
        });
    }

    @SubscribeEvent
    public static void onSoundEngineLoad(SoundEngineLoadEvent event)
    {
        //Only start checking for missing sounds if the sound engine has been initialized once
        if (!isSoundEngineInitialized)
        {
            isSoundEngineInitialized = true;
            return;
        }

        SoundManager soundManager = event.getEngine().soundManager;

        FlansMod.getSoundsOrigins().entrySet().stream()
            .sorted(Comparator.<Map.Entry<ResourceLocation, TypeFile>, String>comparing(e -> e.getValue().getContentPack().getName(), Comparator.naturalOrder())
                .thenComparing(e -> e.getValue().getType(), Comparator.naturalOrder())
                .thenComparing(e -> e.getValue().getName(), Comparator.naturalOrder())
            )
            .forEach(e -> {
                if (soundManager.getSoundEvent(e.getKey()) == null)
                    FlansMod.log.warn("Missing sound {}: {}", e.getKey(), e.getValue());
            });
    }
}
