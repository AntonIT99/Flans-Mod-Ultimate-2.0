package com.flansmodultimate.event;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModelCache;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.render.ClientHudOverlays;
import com.flansmodultimate.client.render.CustomArmorLayer;
import com.flansmodultimate.client.render.entity.BulletRenderer;
import com.flansmodultimate.common.item.ICustomRendererItem;
import com.flansmodultimate.common.item.IPaintableItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.nio.file.Files;

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
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "scope", ClientHudOverlays.SCOPE);
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "armor", ClientHudOverlays.ARMOR);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "hud", ClientHudOverlays.HUD);
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
}
