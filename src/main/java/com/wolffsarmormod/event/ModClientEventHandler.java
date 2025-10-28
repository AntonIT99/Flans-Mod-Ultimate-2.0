package com.wolffsarmormod.event;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.ContentManager;
import com.wolffsarmormod.client.input.KeyInputHandler;
import com.wolffsarmormod.client.render.ClientHudOverlays;
import com.wolffsarmormod.client.render.CustomArmorLayer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;

import java.nio.file.Files;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(modid = ArmorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEventHandler
{
    @SubscribeEvent
    public static void registerPack(AddPackFindersEvent event)
    {
        if (Files.exists(ContentManager.getFlanFolder()))
        {
            event.addRepositorySource(new ModRepositorySource(ContentManager.getFlanFolder()));
        }
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
                ArmorMod.log.error("Could not add armor layer to {}", entityType.getDescriptionId(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent e)
    {
        // Draw scope/armor after everything else
        e.registerAboveAll("scope", ClientHudOverlays.SCOPE);
        e.registerAbove(VanillaGuiOverlay.HELMET.id(), "armor", ClientHudOverlays.ARMOR);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        KeyInputHandler.registerKeys(event);
    }
}
