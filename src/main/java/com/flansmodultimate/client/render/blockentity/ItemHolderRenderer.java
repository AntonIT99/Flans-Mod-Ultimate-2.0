package com.flansmodultimate.client.render.blockentity;

import com.flansmod.client.model.ModelItemHolder;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.types.ItemHolderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class ItemHolderRenderer implements BlockEntityRenderer<ItemHolderBlockEntity>
{
    public ItemHolderRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(@NotNull ItemHolderBlockEntity holder, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        ItemHolderType type = holder.getItemHolderType();
        if (type == null)
            return;

        poseStack.pushPose();
        try
        {
            applyLegacyFacingTransform(holder, poseStack);

            if (ModelCache.getOrLoadTypeModel(type) instanceof ModelItemHolder model)
            {
                int color = type.getColour();
                float red = (color >> 16 & 255) / 255F;
                float green = (color >> 8 & 255) / 255F;
                float blue = (color & 255) / 255F;

                LegacyTransformApplier.renderModel(model, type, type.getTexture(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F);
                renderHeldItem(holder.getStack(), model, poseStack, buffer, packedLight);
            }
            else
            {
                renderHeldItem(holder.getStack(), null, poseStack, buffer, packedLight);
            }
        }
        finally
        {
            poseStack.popPose();
        }
    }

    private static void applyLegacyFacingTransform(ItemHolderBlockEntity holder, PoseStack poseStack)
    {
        Direction facing = holder.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));

        switch (facing)
        {
            case NORTH -> poseStack.translate(-1F, 0F, 0F);
            case EAST ->
            {
                poseStack.translate(-1F, 0F, 1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            }
            case SOUTH ->
            {
                poseStack.translate(0F, 0F, 1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270F));
            default ->
            {
                // no-op
            }
        }
    }

    private static void renderHeldItem(ItemStack stack, ModelItemHolder model, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
    {
        if (stack.isEmpty())
            return;

        poseStack.pushPose();
        try
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
            poseStack.translate(-0.5F, 0.5F, 0.5F);

            if (model != null)
            {
                poseStack.translate(model.itemOffset.x, model.itemOffset.y, model.itemOffset.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(model.itemRotation.x));
                poseStack.mulPose(Axis.ZP.rotationDegrees(model.itemRotation.z));
                poseStack.mulPose(Axis.YP.rotationDegrees(model.itemRotation.y));
            }
            else
            {
                poseStack.translate(0F, 0.25F, 0F);
            }

            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, Minecraft.getInstance().level, 0);
        }
        finally
        {
            poseStack.popPose();
        }
    }
}
