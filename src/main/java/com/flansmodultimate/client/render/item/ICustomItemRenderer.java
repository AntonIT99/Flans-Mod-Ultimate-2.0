package com.flansmodultimate.client.render.item;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
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
            // The left-hand flag mirrors the model's left-hand transform, as vanilla does for held items
            boolean leftHand = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || itemDisplayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            // The holder lets entity-dependent model overrides, such as a throw being charged, resolve again here
            LivingEntity holder = itemDisplayContext.firstPerson() ? mc.player : ModClient.entityRenderContext.get();
            ir.renderStatic(holder, stack, itemDisplayContext, leftHand, pose, buffers, mc.level, light, overlay, 0);
        }
        finally
        {
            CustomItemRenderers.SKIP_BEWLR.set(false);
        }
    }
}
