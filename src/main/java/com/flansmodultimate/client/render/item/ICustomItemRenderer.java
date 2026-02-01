package com.flansmodultimate.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ICustomItemRenderer
{
    void renderItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

    static void renderItemFallback(ItemStack stack, ItemDisplayContext itemDisplayContext, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
    {
        var mc = Minecraft.getInstance();
        var ir = mc.getItemRenderer();

        CustomItemRenderers.SKIP_BEWLR.set(true);
        try
        {
            ir.renderStatic(stack, itemDisplayContext, light, overlay, pose, buffers, null, 0);
        }
        finally
        {
            CustomItemRenderers.SKIP_BEWLR.set(false);
        }
    }
}
