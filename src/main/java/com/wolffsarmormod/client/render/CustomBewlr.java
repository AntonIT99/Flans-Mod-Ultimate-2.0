package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.item.ICustomRendererItem;
import com.wolffsarmormod.common.item.IModelItem;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
        if (item instanceof IModelItem<?,?> modelItem && item instanceof ICustomRendererItem customRendererItem && modelItem.useCustomItemRendering())
        {
            VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(modelItem.getTexture()));

            switch (itemDisplayContext)
            {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                {
                    boolean leftHanded = (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) || (itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
                    if (customRendererItem.useCustomRendererInHand())
                        customRendererItem.renderItem(itemDisplayContext, leftHanded, poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    else
                        renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
                }
                case GROUND ->
                {
                    if (customRendererItem.useCustomRendererOnGround())
                        customRendererItem.renderItem(itemDisplayContext, false, poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    else
                        renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
                }
                case FIXED ->
                {
                    if (customRendererItem.useCustomRendererInFrame())
                        customRendererItem.renderItem(itemDisplayContext, false, poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    else
                        renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
                }
                case GUI ->
                {
                    if (customRendererItem.useCustomRendererInGui())
                        customRendererItem.renderItem(itemDisplayContext, false, poseStack, vertexconsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    else
                        renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
                }
                default -> renderFallback(stack, itemDisplayContext, poseStack, buffer, packedLight, packedOverlay);
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