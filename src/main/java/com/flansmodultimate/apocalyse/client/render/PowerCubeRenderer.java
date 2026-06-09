package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.apocalyse.common.block.entity.PowerCubeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PowerCubeRenderer implements BlockEntityRenderer<PowerCubeBlockEntity>
{
    private static final ItemStack CORE_STACK = new ItemStack(Items.END_CRYSTAL);

    private final ItemRenderer itemRenderer;

    public PowerCubeRenderer(BlockEntityRendererProvider.Context context)
    {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(@NotNull PowerCubeBlockEntity cube, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        float age = cube.getAge() + partialTick;
        renderCore(poseStack, buffer, age, 0.0F, 0.42F);
        renderCore(poseStack, buffer, age, 120.0F, 0.28F);
        renderCore(poseStack, buffer, age, 240.0F, 0.22F);
    }

    private void renderCore(PoseStack poseStack, MultiBufferSource buffer, float age, float angleOffset, float scale)
    {
        poseStack.pushPose();
        try
        {
            float bob = (float)Math.sin(age * 0.08F + angleOffset) * 0.06F;
            poseStack.translate(0.5D, 0.5D + bob, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 4.0F + angleOffset));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 2.7F + angleOffset * 0.5F));
            poseStack.scale(scale, scale, scale);
            itemRenderer.renderStatic(CORE_STACK, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, Minecraft.getInstance().level, 0);
        }
        finally
        {
            poseStack.popPose();
        }
    }
}
