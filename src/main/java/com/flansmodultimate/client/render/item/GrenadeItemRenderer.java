package com.flansmodultimate.client.render.item;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.item.GrenadeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GrenadeItemRenderer
{
    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (stack.getItem() instanceof GrenadeItem grenadeItem && grenadeItem.useCustomRenderer(context))
        {
            IModelBase model = ModelCache.getOrLoadTypeModel(grenadeItem.getConfigType());
            if (model != null)
            {
                int color = grenadeItem.getConfigType().getColour();
                float red = (color >> 16 & 255) / 255F;
                float green = (color >> 8 & 255) / 255F;
                float blue = (color & 255) / 255F;
                poseStack.pushPose();
                applyHandAdjustments(context, poseStack);
                LegacyTransformApplier.renderModel(model, grenadeItem.getConfigType(), grenadeItem.getConfigType().getTexture(), poseStack, buffer, packedLight, packedOverlay, red, green, blue, 1F);
                poseStack.popPose();
                return;
            }
        }

        ICustomItemRenderer.renderItemFallback(stack, context, poseStack, buffer, packedLight, packedOverlay);
    }

    /** 1.7.10 RenderGrenade's held placement, in the frame 1.7.10 gave it; the left hand mirrors the right. */
    private static void applyHandAdjustments(ItemDisplayContext context, PoseStack poseStack)
    {
        boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        switch (context)
        {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                LegacyHeldItemFrames.beginHand(poseStack, leftHand);
                LegacyHeldItemFrames.firstPerson(poseStack);
                poseStack.mulPose(Axis.ZP.rotationDegrees(25F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-5F));
                poseStack.translate(0.15F, 0.2F + 0.25F, -0.6F);
                LegacyHeldItemFrames.endHand(poseStack, leftHand);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                LegacyHeldItemFrames.beginHand(poseStack, leftHand);
                LegacyHeldItemFrames.thirdPerson(poseStack);
                poseStack.mulPose(Axis.ZP.rotationDegrees(35F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-5F));
                poseStack.translate(0.75F, -0.22F + 0.25F, -0.08F);
                LegacyHeldItemFrames.endHand(poseStack, leftHand);
            }
            default -> {
                // Dropped and framed grenades keep their model placement
            }
        }
    }
}
