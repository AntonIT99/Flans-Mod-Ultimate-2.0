package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModRepositorySource;
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
import com.flansmodultimate.client.render.CustomArmorLayer;
import com.flansmodultimate.client.render.entity.BulletRenderer;
import com.flansmodultimate.client.render.entity.GrenadeRenderer;
import com.flansmodultimate.common.item.ICustomRendererItem;
import com.flansmodultimate.common.item.IFlanItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.types.TypeFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
import net.minecraft.world.item.Item;

import java.nio.file.Files;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEventHandler
{
    /** Paintjob registrations */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        for (RegistryObject<Item> item : FlansMod.getItems())
        {
            if (item.get() instanceof IPaintableItem<?>)
            {
                ItemProperties.register(item.get(), FlansMod.paintjob, (stack, level, entity, seed) -> {
                    CompoundTag tag = stack.getTag();
                    return (tag != null && tag.contains(IPaintableItem.NBT_PAINTJOB_ID)) ? tag.getInt(IPaintableItem.NBT_PAINTJOB_ID) : 0;
                });
            }
        }
    }

    @SubscribeEvent
    public static void registerPack(AddPackFindersEvent event)
    {
        if (Files.exists(ContentManager.getFlanFolder()))
        {
            event.addRepositorySource(new ModRepositorySource(ContentManager.getFlanFolder()));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event)
    {
        FlansMod.getItems().stream()
            .filter(itemRegistryObject -> itemRegistryObject.get() instanceof ICustomRendererItem<?>)
            .forEach(itemRegistryObject -> {
                ResourceLocation id = itemRegistryObject.getId();
                // Wrap ALL baked model variants belonging to this item
                event.getModels().replaceAll((loc, original) -> {
                    if (id != null && loc.getNamespace().equals(id.getNamespace()) && loc.getPath().equals(id.getPath()) && !(original instanceof BewlrRoutingModel))
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
        for (String skin : event.getSkins())
        {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new CustomArmorLayer<>(playerRenderer));
            }
        }

        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues())
        {
            try
            {
                EntityRenderer<?> renderer = event.getRenderer((EntityType)entityType);

                if (renderer instanceof LivingEntityRenderer<?, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel)
                {
                    livingEntityRenderer.addLayer(new CustomArmorLayer<>((RenderLayerParent) livingEntityRenderer));
                }
            }
            catch (ClassCastException e)
            {
                // Ignoring (Entity is not a LivingEntity)
            }
            catch (Exception e)
            {
                FlansMod.log.error("Could not add armor layer to {}", entityType.getDescriptionId(), e);
            }
        }
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(FlansMod.bulletEntity.get(), BulletRenderer::new);
        event.registerEntityRenderer(FlansMod.grenadeEntity.get(), GrenadeRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "scope", ClientHudOverlays.SCOPE);
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "armor", ClientHudOverlays.ARMOR);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "hud", ClientHudOverlays.HUD);
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
            .map(RegistryObject::get)
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
        SoundManager soundManager = event.getEngine().soundManager;

        for (Map.Entry<ResourceLocation, TypeFile> soundOrigin : FlansMod.getSoundsOrigins().entrySet())
        {
            if (soundManager.getSoundEvent(soundOrigin.getKey()) == null)
            {
                FlansMod.log.warn("Missing sound: {} from file {}", soundOrigin.getKey(), soundOrigin.getValue());
            }
        }
    }
}
