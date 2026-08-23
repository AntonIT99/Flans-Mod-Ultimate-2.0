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
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@EventBusSubscriber(modid = FlansMod.MOD_ID, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModClientEventHandler
{
    private static final Identifier SULPHURIC_ACID_OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/misc/sulphuric_acid_overlay.png");
    private static final Material SULPHURIC_ACID_STILL = new Material(Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidstill"));
    private static final Material SULPHURIC_ACID_FLOWING = new Material(Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidflowing"));

    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event)
    {
        event.register(new FluidModel.Unbaked(SULPHURIC_ACID_STILL, SULPHURIC_ACID_FLOWING, null, null),
            ApocalypseContent.sulphuricAcid, ApocalypseContent.flowingSulphuricAcid);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event)
    {
        event.registerFluidType(new IClientFluidTypeExtensions()
        {
            @Override
            public Identifier getRenderOverlayTexture(Minecraft minecraft)
            {
                return SULPHURIC_ACID_OVERLAY_TEXTURE;
            }
        }, ApocalypseContent.sulphuricAcidFluidType.get());
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
