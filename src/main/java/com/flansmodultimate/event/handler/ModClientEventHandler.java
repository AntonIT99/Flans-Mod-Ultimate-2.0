package com.flansmodultimate.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.gui.ArmorBoxScreen;
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
import com.flansmodultimate.client.particle.RocketExhaustParticle;
import com.flansmodultimate.client.particle.SmokeBurstParticle;
import com.flansmodultimate.client.particle.SmokeGrenadeParticle;
import com.flansmodultimate.client.render.ClientHudOverlays;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.render.CustomArmorLayer;
import com.flansmodultimate.client.render.entity.BulletRenderer;
import com.flansmodultimate.client.render.entity.DeployableGunRenderer;
import com.flansmodultimate.client.render.entity.GrenadeRenderer;
import com.flansmodultimate.client.render.item.CustomItemRenderers;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.types.TypeFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;

import java.util.Comparator;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@net.neoforged.fml.common.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
                        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                        var tag = customData != null ? customData.copyTag() : null;
                        return (tag != null && tag.contains(IPaintableItem.NBT_PAINTJOB_ID)) ? tag.getInt(IPaintableItem.NBT_PAINTJOB_ID) : 0;
                    });
                }
            }

            // Menus are now registered via registerMenuScreens event
        });
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(FlansMod.gunWorkbenchMenu.get(), GunWorkbenchScreen::new);
        event.register(FlansMod.paintjobTableMenu.get(), PaintjobTableScreen::new);
        event.register(FlansMod.armorBoxMenu.get(), ArmorBoxScreen::new);
        event.register(FlansMod.gunBoxMenu.get(), GunBoxScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "scope"),
            (guiGraphics, partialTick) -> {
                ResourceLocation scopeTexture = null;
                boolean hasScope = ModClient.getCurrentScope() != null && ModClient.getCurrentScope().hasZoomOverlay();
                boolean noScreen = Minecraft.getInstance().screen == null;
                boolean zoomedIn = ModClient.getZoomProgress() > 0.8F;
                if (hasScope && noScreen && zoomedIn)
                    scopeTexture = ModClient.getCurrentScope().getZoomOverlay();
                if (scopeTexture != null)
                {
                    int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    ClientHudOverlays.renderScopeOverlay(guiGraphics, scopeTexture, sw, sh);
                }
            });

        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "ammo_hud"),
            (guiGraphics, partialTick) -> {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null)
                {
                    int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    ClientHudOverlays.renderPlayerAmmo(guiGraphics, sw, sh);
                    ClientHudOverlays.renderDigitalAmmo(guiGraphics, sw, sh);
                }
            });

        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "armor_overlay"),
            (guiGraphics, partialTick) -> {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON)
                {
                    int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    ClientHudOverlays.renderArmorOverlay(guiGraphics, sw, sh);
                }
            });
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event)
    {
        FlansMod.getItems().stream()
            .filter(itemRegistryObject -> itemRegistryObject.get() instanceof ICustomRendereredItem<?>)
            .forEach(itemRegistryObject -> {
                ResourceLocation id = itemRegistryObject.getId();
                // Wrap ALL baked model variants belonging to this item
                event.getModels().replaceAll((loc, original) -> {
                    if (id != null && loc.id().getNamespace().equals(id.getNamespace()) && loc.id().getPath().equals(id.getPath()) && !(original instanceof BewlrRoutingModel))
                    {
                        return new BewlrRoutingModel(original);
                    }
                    return original;
                });
            });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerArmorLayer(EntityRenderersEvent.AddLayers event)
    {
        for (PlayerSkin.Model skin : event.getSkins())
        {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new CustomArmorLayer<>(playerRenderer));
            }
        }

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE)
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
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(FlansMod.bulletEntity.get(), BulletRenderer::new);
        event.registerEntityRenderer(FlansMod.grenadeEntity.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(FlansMod.deployedGunEntity.get(), DeployableGunRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(FlansMod.afterburnParticle.get(), AfterburnParticle.Provider::new);
        event.registerSpriteSet(FlansMod.bigSmokeParticle.get(), BigSmokeParticle.Provider::new);
        event.registerSpriteSet(FlansMod.debris1Particle.get(), Debris1Particle.Provider::new);
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
                return flanItem.getConfigType().getColour();
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
        event.registerReloadListener((ResourceManagerReloadListener) rm -> ModelCache.reload());
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