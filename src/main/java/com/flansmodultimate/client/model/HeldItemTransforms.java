package com.flansmodultimate.client.model;

import com.flansmodultimate.api.FlansItemAnimations;
import com.flansmodultimate.api.HeldItemTransform;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.item.ICustomRendereredItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shared placement for native first/third-person render paths and addon held-item render layers. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeldItemTransforms
{
    public static void apply(PoseStack pose, LivingEntity holder, ItemStack stack, InteractionHand hand, ItemDisplayContext context)
    {
        if (!(stack.getItem() instanceof ICustomRendereredItem<?> item) || !item.useCustomRenderer(context) || GunArmPoses.classify(stack) == GunArmPoses.HandItem.SHIELD)
            return;
        HeldItemTransform transform = FlansItemAnimations.get(holder, hand).transform();
        if (transform.equals(HeldItemTransform.IDENTITY))
            return;
        pose.translate(transform.x() / 16F, transform.y() / 16F, transform.z() / 16F);
        pose.mulPose(Axis.XP.rotationDegrees(transform.rotationX()));
        pose.mulPose(Axis.YP.rotationDegrees(transform.rotationY()));
        pose.mulPose(Axis.ZP.rotationDegrees(transform.rotationZ()));
    }
}
