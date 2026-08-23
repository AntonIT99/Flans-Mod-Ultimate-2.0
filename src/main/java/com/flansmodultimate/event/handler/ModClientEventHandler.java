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
import com.flansmodultimate.client.render.CustomRenderType;
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
import com.flansmodultimate.client.render.item.LegacyItemModel;
import com.flansmodultimate.client.render.item.LegacyItemPreviewRenderer;
import com.flansmodultimate.client.render.item.OpStickItemModel;
import com.flansmodultimate.common.types.TypeFile;
import com.google.common.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.Comparator;
import java.util.Map;

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
    public static void registerItemModels(RegisterItemModelsEvent event)
    {
        event.register(LegacyItemModel.TYPE, LegacyItemModel.Unbaked.MAP_CODEC);
        event.register(OpStickItemModel.TYPE, OpStickItemModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event)
    {
        event.register(LegacyItemPreviewRenderer.State.class, LegacyItemPreviewRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerArmorLayer(EntityRenderersEvent.AddLayers event)
    {
        for (var skin : event.getSkins())
        {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer instanceof AvatarRenderer<?> playerRenderer)
            {
                playerRenderer.addLayer(new CustomArmorLayer<>(playerRenderer));
            }
            var mannequinRenderer = event.getMannequinRenderer(skin);
            if (mannequinRenderer != null)
                mannequinRenderer.addLayer(new CustomArmorLayer<>(mannequinRenderer));
        }

        for (EntityType<?> entityType : event.getEntityTypes())
        {
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            EntityRenderer<?, ?> renderer = event.getRenderer(livingType);

            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer && livingRenderer.getModel() instanceof HumanoidModel<?>)
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
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "scope"), ClientHudOverlays.SCOPE);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "armor"), ClientHudOverlays.ARMOR);
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "damage_absorption"), ClientHudOverlays.DAMAGE_ABSORPTION);
        event.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "hud"), ClientHudOverlays.HUD);
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
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event)
    {
        CustomRenderType.registerPipelines(event);
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event)
    {
        event.registerEntityModifier(
            new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() { },
            ClientEventHandler::extractLivingRenderState);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        KeyInputHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void onClientReload(AddClientReloadListenersEvent event)
    {
        event.addListener(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "model_cache"), (ResourceManagerReloadListener) rm -> {
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
            .sorted(Comparator.<Map.Entry<Identifier, TypeFile>, String>comparing(e -> e.getValue().getContentPack().getName(), Comparator.naturalOrder())
                .thenComparing(e -> e.getValue().getType(), Comparator.naturalOrder())
                .thenComparing(e -> e.getValue().getName(), Comparator.naturalOrder())
            )
            .forEach(e -> {
                if (soundManager.getSoundEvent(e.getKey()) == null)
                    FlansMod.log.warn("Missing sound {}: {}", e.getKey(), e.getValue());
            });
    }
}
