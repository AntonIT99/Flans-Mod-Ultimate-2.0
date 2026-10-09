package com.flansmodultimate.event.handler;

import com.flansmodultimate.*;
import com.flansmodultimate.client.EntityCullingCompat;
import com.flansmodultimate.client.distant.DistantHorizonsClient;
import com.flansmodultimate.client.gui.ModMenuScreens;
import com.flansmodultimate.client.gui.options.FlansSettingsHubScreen;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.model.BewlrRoutingModel;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.particle.*;
import com.flansmodultimate.client.render.blockentity.ItemHolderRenderer;
import com.flansmodultimate.client.render.entity.*;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.client.render.hud.ClientHudOverlays;
import com.flansmodultimate.client.render.item.CustomItemRenderers;
import com.flansmodultimate.client.render.layer.*;
import com.flansmodultimate.client.render.thermal.VehicleThermalRenderer;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.item.*;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.platform.client.HudOverlayPlatform;
import com.flansmodultimate.platform.item.ItemStackData;
import com.flansmodultimate.platform.registry.RegistryEntry;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEventHandler
{
    private static boolean isSoundEngineInitialized;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        // The Config button of the mod list opens the same screen as the pause menu button
        ModList.get().getModContainerById(FlansMod.MOD_ID).ifPresent(container -> container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new FlansSettingsHubScreen(parent))));

        event.enqueueWork(() ->
        {
            CustomItemRenderers.registerAll();
            DistantHorizonsClient.init();
            EntityCullingCompat.init();

            // Paintjob registrations
            for (RegistryEntry<Item> item : FlansMod.getItems())
            {
                if (item.get() instanceof IPaintableItem<?>)
                {
                    ItemProperties.register(item.get(), FlansMod.PAINTJOB, (stack, level, entity, seed) ->
                    {
                        CompoundTag tag = ItemStackData.copy(stack);
                        return tag.contains(IPaintableItem.NBT_PAINTJOB_ID) ? tag.getInt(IPaintableItem.NBT_PAINTJOB_ID) : 0;
                    });
                }

                // Like the trident's, lets a model switch to a raised pose while a throw is charged
                if (item.get() instanceof GunItem gunItem && gunItem.getConfigType().isThrowable())
                {
                    ItemProperties.register(item.get(), FlansMod.THROWING, (stack, level, entity, seed) -> entity != null && gunItem.isChargingThrow(entity, stack) ? 1F : 0F);
                }
            }
            ItemProperties.register(FlansModItems.opStick.get(), ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "teams_mode"),
                (stack, level, entity, seed) -> ItemOpStick.getMode(stack).ordinal());

            ModMenuScreens.register(MenuScreens::register);
        });
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event)
    {
        Set<ResourceLocation> customRenderedItemIds = FlansMod.getItems().stream().filter(itemRegistryObject -> itemRegistryObject.get() instanceof ICustomRendereredItem<?>).map(RegistryEntry::getId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());

        // Wrap all variants in one pass. Large legacy installations can have
        // thousands of registered Flan items, so one full map scan per item is
        // prohibitively expensive during every resource reload.
        event.getModels().replaceAll((location, original) ->
        {
            if (customRenderedItemIds.contains(ClientPlatform.modelItemId(location)) && !(original instanceof BewlrRoutingModel))
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
                playerRenderer.addLayer(new ArmorCapeLayer<>(playerRenderer));
            }
        }

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE)
        {
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            EntityRenderer<? extends LivingEntity> renderer = event.getRenderer(livingType);

            if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer && livingRenderer.getModel() instanceof HumanoidModel<?>)
            {
                livingRenderer.addLayer(new CustomArmorLayer<>((RenderLayerParent) livingRenderer));
                livingRenderer.addLayer(new ArmorCapeLayer<>((RenderLayerParent) livingRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(FlansModEntities.bulletEntity.get(), BulletRenderer::new);
        event.registerEntityRenderer(FlansModEntities.grenadeEntity.get(), GrenadeRenderer::new);
        event.registerEntityRenderer(FlansModEntities.thrownGunEntity.get(), ThrownGunRenderer::new);
        event.registerEntityRenderer(FlansModEntities.deployedGunEntity.get(), DeployableGunRenderer::new);
        event.registerEntityRenderer(FlansModEntities.aaGunEntity.get(), AAGunRenderer::new);
        event.registerEntityRenderer(FlansModEntities.parachuteEntity.get(), ParachuteRenderer::new);
        event.registerEntityRenderer(FlansModEntities.planeEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansModEntities.vehicleEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansModEntities.mechaEntity.get(), DriveableRenderer::new);
        event.registerEntityRenderer(FlansModEntities.seatEntity.get(), InvisibleEntityRenderer::new);
        event.registerEntityRenderer(FlansModEntities.wheelEntity.get(), InvisibleEntityRenderer::new);
        event.registerEntityRenderer(FlansModEntities.flagpoleEntity.get(), TeamObjectRenderer::new);
        event.registerEntityRenderer(FlansModEntities.flagEntity.get(), TeamObjectRenderer::new);
        event.registerEntityRenderer(FlansModEntities.gunItemEntity.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(FlansModEntities.teamItemEntity.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(FlansModBlocks.itemHolderBlockEntity.get(), ItemHolderRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        ClientHudOverlays.register(HudOverlayPlatform.registrar(event));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(FlansModParticles.afterburnParticle.get(), AfterburnParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.bigSmokeParticle.get(), BigSmokeParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.blastPuffParticle.get(), BlastPuffParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.debris1Particle.get(), Debris1Particle.Provider::new);
        event.registerSpriteSet(FlansModParticles.explodeParticle.get(), LegacyExplodeParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fireExplosionParticle.get(), FireExplosionParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.flareParticle.get(), FlareParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.flashParticle.get(), FlashParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmFlameParticle.get(), FmFlameParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmMuzzleFlashParticle.get(), FmMuzzleFlashParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmSmokeParticle.get(), FmSmokeParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmTracerParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmTracerGreenParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.fmTracerRedParticle.get(), FmTracerParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.rocketExhaustParticle.get(), RocketExhaustParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.smokeBurstParticle.get(), SmokeBurstParticle.Provider::new);
        event.registerSpriteSet(FlansModParticles.smokeGrenadeParticle.get(), SmokeGrenadeParticle.Provider::new);
    }

    /** Team spawners paint their overlay decal in the colour of the team that owns them. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event)
    {
        event.register((state, level, pos, tintIndex) ->
        {
            if (tintIndex != 0 || level == null || pos == null)
                return TeamSpawnerBlockEntity.UNOWNED_COLOUR;
            return level.getBlockEntity(pos) instanceof TeamSpawnerBlockEntity spawner ? spawner.getTeamColour() : TeamSpawnerBlockEntity.UNOWNED_COLOUR;
        }, FlansModBlocks.playerSpawner.get(), FlansModBlocks.itemSpawner.get(), FlansModBlocks.vehicleSpawner.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
        // A spawner in the inventory belongs to no team yet
        event.register((stack, tintIndex) -> tintIndex == 0 ? TeamSpawnerBlockEntity.UNOWNED_COLOUR : 0xFFFFFFFF, FlansModItems.playerSpawnerItem.get(), FlansModItems.itemSpawnerItem.get(),
            FlansModItems.vehicleSpawnerItem.get());

        event.register((stack, tintIndex) ->
        {
            Item item = stack.getItem();
            if (item instanceof IFlanItem<?> flanItem)
                // Legacy content packs store colours as 24-bit RGB. The 1.21
                // item renderer reads ARGB and would treat the missing high
                // byte as alpha=0, making tinted items transparent; 1.20.1
                // ignores the alpha byte.
                return 0xFF000000 | flanItem.getConfigType().getColour();
            return 0xFFFFFFFF;
        }, FlansMod.getItems().stream().map(RegistryEntry::get).toArray(Item[]::new));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        KeyInputHandler.registerKeys(event::register);
    }

    @SubscribeEvent
    public static void onClientReload(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener((ResourceManagerReloadListener) rm ->
        {
            VehicleThermalRenderer.reset();
            ModelCache.reload();
            PlayerSkinOverrides.clearValidationCache();
            ArmorCapeLayer.clearValidationCache();
            ContentManager.logMissingModelTextures(rm);
        });
    }

    @SubscribeEvent
    public static void registerGpuModelShader(net.minecraftforge.client.event.RegisterShadersEvent event)
    {
        GpuModelCache.registerShader(event);
    }

    @SubscribeEvent
    public static void onSoundEngineLoad(SoundEngineLoadEvent event)
    {
        // Only start checking for missing sounds if the sound engine has been initialized once
        if (!isSoundEngineInitialized)
        {
            isSoundEngineInitialized = true;
            return;
        }

        SoundManager soundManager = event.getEngine().soundManager;

        FlansMod.getSoundsOrigins().entrySet().stream()
            .sorted(Comparator.<Map.Entry<ResourceLocation, TypeFile>, String>comparing(e -> e.getValue().getContentPack().getName(), Comparator.naturalOrder())
                .thenComparing(e -> e.getValue().getType(), Comparator.naturalOrder()).thenComparing(e -> e.getValue().getName(), Comparator.naturalOrder()))
            .forEach(e ->
            {
                if (soundManager.getSoundEvent(e.getKey()) == null)
                    FlansLog.log.warn("Missing sound {}: {}", e.getKey(), e.getValue());
            });
    }
}
