package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsarmormod.common.item.ICustomRendererItem;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomBewlr extends BlockEntityWithoutLevelRenderer
{
    public static final ThreadLocal<Boolean> SKIP_BEWLR = ThreadLocal.withInitial(() -> false);

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

        Item item = stack.getItem();
        if (item instanceof ICustomRendererItem<?> customRendererItem)
        {
            boolean useCustomRenderer;

            switch (itemDisplayContext)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> useCustomRenderer = customRendererItem.useCustomRendererInHand();
                case GROUND -> useCustomRenderer = customRendererItem.useCustomRendererOnGround();
                case FIXED -> useCustomRenderer = customRendererItem.useCustomRendererInFrame();
                case GUI -> useCustomRenderer = customRendererItem.useCustomRendererInGui();
                default -> useCustomRenderer = false;
            }

            if (useCustomRenderer)
            {
                customRendererItem.renderItem(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
            }
            else
            {
                renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
            }
        }
        else
        {
            renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private void renderFallback(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
    {
        var mc = Minecraft.getInstance();
        var ir = mc.getItemRenderer();

        SKIP_BEWLR.set(true);
        try
        {
            ir.renderStatic(stack, itemDisplayContext, light, overlay, pose, buffers, null, 0);
        }
        finally
        {
            SKIP_BEWLR.set(false);
        }
    }
}