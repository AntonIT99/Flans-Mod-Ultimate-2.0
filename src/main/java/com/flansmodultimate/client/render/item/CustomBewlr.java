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

        boolean rendered = false;

        if (stack.getItem() instanceof ICustomRendereredItem<?> customRendererItem && customRendererItem.useCustomRenderer(itemDisplayContext))
        {
            ICustomItemRenderer renderer = CustomItemRenderers.get(stack.getItem());
            if (renderer != null)
            {
                renderer.renderItem(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
                rendered = true;
            }
        }

        if (!rendered)
            ICustomItemRenderer.renderItemFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}