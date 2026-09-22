package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.client.ApocalypseHudOverlays;
import com.flansmodultimate.apocalyse.client.render.InventoryHolderRenderer;
import com.flansmodultimate.apocalyse.client.render.ItemEntityRenderer;
import com.flansmodultimate.apocalyse.client.render.PowerCubeRenderer;
import com.flansmodultimate.apocalyse.client.render.SurvivorRenderer;
import com.flansmodultimate.client.render.entity.DriveableRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModClientEventHandler
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ApocalypseContent.sulphuricAcid.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ApocalypseContent.flowingSulphuricAcid.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ApocalypseContent.survivor.get(), SurvivorRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.worldgenSpawnMarker.get(), NoopRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.teleporter.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.BLOCK_POWER_CUBE_ITEM.get()), 2.0F));
        event.registerEntityRenderer(ApocalypseContent.nukeDrop.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.SULPHURIC_ACID_BUCKET.get()), 1.5F));
        event.registerEntityRenderer(ApocalypseContent.skullDrone.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.SKELETON_SKULL), 1.8F));
        event.registerEntityRenderer(ApocalypseContent.skullBoss.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.WITHER_SKELETON_SKULL), 8.0F));
        event.registerEntityRenderer(ApocalypseContent.inventoryHolder.get(), InventoryHolderRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.flyByPlane.get(), DriveableRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.aiMecha.get(), DriveableRenderer::new);
        event.registerBlockEntityRenderer(ApocalypseContent.powerCubeBlockEntity.get(), PowerCubeRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "apocalypse_countdown", ApocalypseHudOverlays.COUNTDOWN);
    }
}
