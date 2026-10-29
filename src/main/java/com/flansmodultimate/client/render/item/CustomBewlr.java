package com.flansmodultimate.client.render.item;

import com.flansmodultimate.api.client.equipment.FlansEquipmentRender;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
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
        if (itemDisplayContext.firstPerson() && Minecraft.getInstance().player != null)
        {
            var holder = Minecraft.getInstance().player;
            boolean left = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            InteractionHand hand = left == (holder.getMainArm() == HumanoidArm.LEFT) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            FlansEquipmentRender.applyHeldItemTransform(poseStack, holder, stack, hand, itemDisplayContext);
        }

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
