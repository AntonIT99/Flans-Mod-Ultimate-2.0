package com.wolffsarmormod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.item.IModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(ItemStack stack, ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, BakedModel model, CallbackInfo ci)
    {
        if (shouldUseCustomRendering(stack, itemDisplayContext))
        {
            IModelItem<?, ?> item = (IModelItem<?, ?>) stack.getItem();
            VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityTranslucent(item.getTexture()));
            item.renderItem(itemDisplayContext, leftHanded, poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            ci.cancel();
        }
    }

    @Unique
    private boolean shouldUseCustomRendering(ItemStack stack, ItemDisplayContext itemDisplayContext)
    {
        return itemDisplayContext != ItemDisplayContext.NONE
            && itemDisplayContext != ItemDisplayContext.GUI
            && itemDisplayContext != ItemDisplayContext.HEAD
            && stack.getItem() instanceof IModelItem<?, ?> item
            && item.useCustomItemRendering();
    }
}
