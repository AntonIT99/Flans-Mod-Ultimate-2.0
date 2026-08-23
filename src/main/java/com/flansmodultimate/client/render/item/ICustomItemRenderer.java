package com.flansmodultimate.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ICustomItemRenderer
{
    void renderItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

    static void renderItemFallback(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
    {
        // Fallback selection is handled by LegacyItemModel before the deferred
        // custom renderer is submitted. Calling ItemRenderer here would recurse
        // back through the same item model on 26.1.
    }
}
