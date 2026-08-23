package com.flansmodultimate.client.render.blockentity;

import com.flansmod.client.model.ModelItemHolder;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.types.ItemHolderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemHolderRenderer implements BlockEntityRenderer<ItemHolderBlockEntity, ItemHolderRenderer.State>
{
    private final ItemModelResolver itemModelResolver;

    public ItemHolderRenderer(BlockEntityRendererProvider.Context context)
    {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(ItemHolderBlockEntity holder, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(holder, state, partialTick, cameraPosition, breakProgress);
        state.type = holder.getItemHolderType();
        state.model = state.type != null && ModelCache.getOrLoadTypeModel(state.type) instanceof ModelItemHolder model ? model : null;
        state.facing = holder.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        itemModelResolver.updateForTopItem(state.item, holder.getStack(), ItemDisplayContext.FIXED, holder.getLevel(), null,
            (int)holder.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        if (state.type == null)
            return;

        poseStack.pushPose();
        applyLegacyFacingTransform(state.facing, poseStack);

        if (state.model != null)
        {
            int color = state.type.getColour();
            float red = (color >> 16 & 255) / 255F;
            float green = (color >> 8 & 255) / 255F;
            float blue = (color & 255) / 255F;
            LegacyTransformApplier.submitModel(state.model, state.type, state.type.getTexture(), poseStack, collector,
                state.lightCoords, OverlayTexture.NO_OVERLAY, red, green, blue, 1F);
        }

        submitHeldItem(state, poseStack, collector);
        poseStack.popPose();
    }

    private static void applyLegacyFacingTransform(Direction facing, PoseStack poseStack)
    {
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        switch (facing)
        {
            case NORTH -> poseStack.translate(-1F, 0F, 0F);
            case EAST -> {
                poseStack.translate(-1F, 0F, 1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            }
            case SOUTH -> {
                poseStack.translate(0F, 0F, 1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270F));
            default -> { }
        }
    }

    private static void submitHeldItem(State state, PoseStack poseStack, SubmitNodeCollector collector)
    {
        if (state.item.isEmpty())
            return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.translate(-0.5F, 0.5F, 0.5F);
        if (state.model != null)
        {
            poseStack.translate(state.model.itemOffset.x, state.model.itemOffset.y, state.model.itemOffset.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(state.model.itemRotation.x));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.model.itemRotation.z));
            poseStack.mulPose(Axis.YP.rotationDegrees(state.model.itemRotation.y));
        }
        else
        {
            poseStack.translate(0F, 0.25F, 0F);
        }
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState
    {
        private final ItemStackRenderState item = new ItemStackRenderState();
        private @Nullable ItemHolderType type;
        private @Nullable ModelItemHolder model;
        private Direction facing = Direction.NORTH;
    }
}
