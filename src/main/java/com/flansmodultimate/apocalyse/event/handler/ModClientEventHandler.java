package com.flansmodultimate.apocalyse.event.handler;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.client.render.ItemEntityRenderer;
import com.flansmodultimate.apocalyse.client.render.PowerCubeRenderer;
import com.flansmodultimate.apocalyse.client.render.SurvivorRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModClientEventHandler
{
    private static final ResourceLocation SULPHURIC_ACID_STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidstill");
    private static final ResourceLocation SULPHURIC_ACID_FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidflowing");
    private static final ResourceLocation SULPHURIC_ACID_OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/misc/sulphuric_acid_overlay.png");

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event)
    {
        event.registerFluidType(new IClientFluidTypeExtensions()
        {
            @Override
            public ResourceLocation getStillTexture()
            {
                return SULPHURIC_ACID_STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture()
            {
                return SULPHURIC_ACID_FLOWING_TEXTURE;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft minecraft)
            {
                return SULPHURIC_ACID_OVERLAY_TEXTURE;
            }
        }, ApocalypseContent.sulphuricAcidFluidType.get());
    }

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
        event.registerEntityRenderer(ApocalypseContent.teleporter.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.BLOCK_POWER_CUBE_ITEM.get()), 2.0F));
        event.registerEntityRenderer(ApocalypseContent.nukeDrop.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.SULPHURIC_ACID_BUCKET.get()), 1.5F));
        event.registerEntityRenderer(ApocalypseContent.skullDrone.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.SKELETON_SKULL), 1.8F));
        event.registerEntityRenderer(ApocalypseContent.skullBoss.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.WITHER_SKELETON_SKULL), 8.0F));
        event.registerBlockEntityRenderer(ApocalypseContent.powerCubeBlockEntity.get(), PowerCubeRenderer::new);
    }
}
