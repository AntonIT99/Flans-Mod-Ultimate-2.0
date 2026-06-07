package com.flansmodultimate.apocalyse.client;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ApocalypseClientEvents
{
    private ApocalypseClientEvents()
    {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ApocalypseContent.SURVIVOR.get(), SurvivorRenderer::new);
        event.registerEntityRenderer(ApocalypseContent.TELEPORTER.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.BLOCK_POWER_CUBE_ITEM.get()), 2.0F));
        event.registerEntityRenderer(ApocalypseContent.NUKE_DROP.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(ApocalypseContent.SULPHURIC_ACID_BUCKET.get()), 1.5F));
        event.registerEntityRenderer(ApocalypseContent.SKULL_DRONE.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.SKELETON_SKULL), 1.8F));
        event.registerEntityRenderer(ApocalypseContent.SKULL_BOSS.get(), ctx -> new ItemEntityRenderer<>(ctx, () -> new ItemStack(Items.WITHER_SKELETON_SKULL), 8.0F));
    }

    private static final class SurvivorRenderer extends HumanoidMobRenderer<SurvivorEntity, HumanoidModel<SurvivorEntity>>
    {
        private static final ResourceLocation TEXTURE = ApocalypseContent.id("textures/entity/survivor.png");

        private SurvivorRenderer(EntityRendererProvider.Context context)
        {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        }

        @Override
        @NotNull
        public ResourceLocation getTextureLocation(@NotNull SurvivorEntity entity)
        {
            return TEXTURE;
        }
    }

    private static final class ItemEntityRenderer<T extends Entity> extends EntityRenderer<T>
    {
        private final Supplier<ItemStack> stackSupplier;
        private final ItemRenderer itemRenderer;
        private final float scale;

        private ItemEntityRenderer(EntityRendererProvider.Context context, Supplier<ItemStack> stackSupplier, float scale)
        {
            super(context);
            this.stackSupplier = stackSupplier;
            this.itemRenderer = context.getItemRenderer();
            this.scale = scale;
        }

        @Override
        public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
        {
            poseStack.pushPose();
            poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            poseStack.scale(scale, scale, scale);
            itemRenderer.renderStatic(stackSupplier.get(), ItemDisplayContext.GROUND, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        @Override
        @NotNull
        public ResourceLocation getTextureLocation(@NotNull T entity)
        {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
