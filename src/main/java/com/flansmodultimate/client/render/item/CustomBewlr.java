package com.flansmodultimate.client.render.item;

import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomBewlr extends BlockEntityWithoutLevelRenderer
{
    public CustomBewlr(BlockEntityRenderDispatcher berd, EntityModelSet models)
    {
        super(berd, models);
    }

    @Override
    public void renderByItem(ItemStack stack, @NotNull ItemDisplayContext itemDisplayContext, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        poseStack.pushPose();
        // Cancel the offsetting in ItemRenderer.render()
        poseStack.translate(0.5F, 0.5F, 0.5F);

        boolean useCustomRenderer = false;

        if (stack.getItem() instanceof ICustomRendereredItem<?> customRendererItem)
        {
            useCustomRenderer = switch (itemDisplayContext)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> customRendererItem.useCustomRendererInHand();
                case GROUND -> customRendererItem.useCustomRendererOnGround();
                case FIXED -> customRendererItem.useCustomRendererInFrame();
                case GUI -> customRendererItem.useCustomRendererInGui();
                default -> false;
            };
        }

        if (useCustomRenderer)
        {
            ICustomItemRenderer renderer = CustomItemRenderers.get(stack.getItem());
            if (renderer != null)
                renderer.renderItem(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
            else
                useCustomRenderer = false;
        }

        if (!useCustomRenderer)
            ICustomItemRenderer.renderItemFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}