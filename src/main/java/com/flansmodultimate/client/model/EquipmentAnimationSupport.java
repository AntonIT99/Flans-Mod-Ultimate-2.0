package com.flansmodultimate.client.model;

import com.flansmodultimate.api.IEquipmentPolicy;
import com.flansmodultimate.common.guns.GunArmPoses;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;

/** Resting limb pose for equipment whose wearer disables armor animations. */
public final class EquipmentAnimationSupport
{
    private EquipmentAnimationSupport()
    {}

    public static void restArmor(HumanoidModel<?> model)
    {
        for (ModelPart part : new ModelPart[]{model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg})
        {
            part.xRot = 0F;
            part.yRot = 0F;
            part.zRot = 0F;
        }
    }

    public static void weapons(HumanoidModel<?> model, LivingEntity entity)
    {
        if (entity instanceof IEquipmentPolicy policy && !policy.flansWeaponAnimations())
            return;
        GunArmPoses.Result poses = GunArmPoses.resolve(entity);
        for (InteractionHand hand : InteractionHand.values())
            hand(model, entity, hand, poses.get(hand));
    }

    private static void hand(HumanoidModel<?> model, LivingEntity entity, InteractionHand hand, GunArmPoses.Arm pose)
    {
        boolean right = (hand == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.RIGHT);
        ModelPart arm = right ? model.rightArm : model.leftArm;
        if (pose != GunArmPoses.Arm.NONE)
        {
            arm.xRot = model.head.xRot - (pose == GunArmPoses.Arm.THROW ? (float) Math.PI : (float) Math.PI / 2F);
            arm.yRot = model.head.yRot;
            arm.zRot = 0F;
            if (pose == GunArmPoses.Arm.BOW)
            {
                ModelPart other = right ? model.leftArm : model.rightArm;
                other.xRot = model.head.xRot - (float) Math.PI / 2F;
                other.yRot = model.head.yRot + (right ? 0.4F : -0.4F);
                other.zRot = 0F;
            }
        }
        if (entity.isUsingItem() && entity.getUsedItemHand() == hand)
            using(model, arm, right, entity.getUseItem().getUseAnimation());
    }

    private static void using(HumanoidModel<?> model, ModelPart arm, boolean right, UseAnim animation)
    {
        if (animation == UseAnim.BLOCK)
        {
            arm.xRot = -0.95F + model.head.xRot;
            arm.yRot = model.head.yRot + (right ? -0.5F : 0.5F);
            arm.zRot = 0F;
        }
        else if (animation == UseAnim.SPEAR)
        {
            arm.xRot = -(float) Math.PI + model.head.xRot;
            arm.yRot = model.head.yRot;
        }
        else if (animation == UseAnim.BOW || animation == UseAnim.CROSSBOW)
        {
            model.rightArm.xRot = model.leftArm.xRot = -(float) Math.PI / 2F + model.head.xRot;
            model.rightArm.yRot = model.head.yRot - 0.1F;
            model.leftArm.yRot = model.head.yRot + 0.4F;
            model.rightArm.zRot = model.leftArm.zRot = 0F;
        }
    }
}
