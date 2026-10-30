package com.flansmodultimate.client.model;

import com.flansmodultimate.api.client.equipment.ArmorPoseStep;
import com.flansmodultimate.api.equipment.ArmPoseRule;
import com.flansmodultimate.api.equipment.IEquipmentPolicy;
import com.flansmodultimate.common.entity.ProneSupport;
import com.flansmodultimate.common.guns.GunArmPoses;
import com.flansmodultimate.common.guns.HeldItemPresentation;
import com.flansmodultimate.common.guns.ProneArmPoses;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Resting limb pose for equipment whose wearer disables armor animations. */
public final class EquipmentAnimationSupport
{
    /** Steps addons registered for armour models; read on the render thread, written during client setup. */
    private static final List<ArmorPoseStep> armorPoseSteps = new CopyOnWriteArrayList<>();

    private EquipmentAnimationSupport()
    {}

    public static void addArmorPoseStep(ArmorPoseStep step)
    {
        armorPoseSteps.add(Objects.requireNonNull(step));
    }

    /** Lets addons adjust an armour model whose pose has just been copied from its wearer's model. */
    public static void armorPoseSteps(HumanoidModel<?> model, LivingEntity wearer)
    {
        for (ArmorPoseStep step : armorPoseSteps)
            step.apply(wearer, model);
    }

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
        GunArmPoses.Result poses = GunArmPoses.resolve(entity);
        for (InteractionHand hand : InteractionHand.values())
            hand(model, entity, hand, poses.get(hand));
        attack(model, entity, poses);
        // A native two-handed use animation may have moved the other arm after its shield was posed.
        for (InteractionHand hand : InteractionHand.values())
            if (GunArmPoses.classify(entity.getItemInHand(hand)) == GunArmPoses.HandItem.SHIELD)
                hand(model, entity, hand, GunArmPoses.Arm.ONE_ARM);
        prone(model, entity, Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot), entity.getXRot());
        customArms(model, entity);
    }

    /**
     * Holds the guns of an entity lying prone forward along its body, towards where it looks, in place of the native crawling
     * stroke; see {@link ProneArmPoses}.
     *
     * @param headYaw
     *            the head's yaw relative to the body, in degrees
     * @param headPitch
     *            the head's pitch, in degrees
     */
    public static void prone(HumanoidModel<?> model, LivingEntity entity, float headYaw, float headPitch)
    {
        float lying = ProneSupport.lyingAmount(entity, model.swimAmount);
        if (lying <= 0F)
            return;
        ProneArmPoses.resolve(GunArmPoses.resolve(entity), entity.getMainArm() == HumanoidArm.RIGHT, headPitch * Mth.DEG_TO_RAD, headYaw * Mth.DEG_TO_RAD, lying).ifPresent(prone ->
        {
            rotate(model.head, prone.head());
            model.hat.copyFrom(model.head);
            if (prone.rightArm() != null)
                rotate(model.rightArm, prone.rightArm());
            if (prone.leftArm() != null)
                rotate(model.leftArm, prone.leftArm());
        });
    }

    private static void rotate(ModelPart part, ProneArmPoses.Rotation rotation)
    {
        part.xRot = rotation.x();
        part.yRot = rotation.y();
        part.zRot = rotation.z();
    }

    private static void hand(HumanoidModel<?> model, LivingEntity entity, InteractionHand hand, GunArmPoses.Arm pose)
    {
        boolean right = (hand == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.RIGHT);
        ModelPart arm = right ? model.rightArm : model.leftArm;
        if (pose == GunArmPoses.Arm.RELAXED)
            arm.xRot = arm.yRot = arm.zRot = 0F;
        else if (pose != GunArmPoses.Arm.NONE)
            aiming(model, arm, right, pose);
        if (pose == GunArmPoses.Arm.NONE && mayAnimateUse(entity, hand))
            using(model, arm, right, entity.getUseItem().getUseAnimation());
    }

    private static void aiming(HumanoidModel<?> model, ModelPart arm, boolean right, GunArmPoses.Arm pose)
    {
        arm.xRot = model.head.xRot - (pose == GunArmPoses.Arm.THROW ? Mth.PI : Mth.HALF_PI);
        arm.yRot = model.head.yRot;
        arm.zRot = 0F;
        if (pose == GunArmPoses.Arm.BOTH)
        {
            arm.xRot = -Mth.HALF_PI;
            arm.yRot = right ? -0.05F : 0.05F;
        }
        if (pose == GunArmPoses.Arm.BOW)
        {
            ModelPart other = right ? model.leftArm : model.rightArm;
            other.xRot = model.head.xRot - Mth.HALF_PI;
            other.yRot = model.head.yRot + (right ? 0.4F : -0.4F);
            other.zRot = 0F;
        }
    }

    /** Restores vanilla's striking-arm stroke after a late equipment pose has replaced its rotations. */
    private static void attack(HumanoidModel<?> model, LivingEntity entity, GunArmPoses.Result poses)
    {
        InteractionHand hand = entity.swingingArm == InteractionHand.OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (model.attackTime <= 0F || poses.get(hand) == GunArmPoses.Arm.NONE || poses.get(hand) == GunArmPoses.Arm.RELAXED || poses.get(hand) == GunArmPoses.Arm.THROW)
            return;
        ModelPart arm = (hand == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        float eased = 1F - model.attackTime;
        eased *= eased;
        eased *= eased;
        float stroke = Mth.sin((1F - eased) * Mth.PI);
        float swing = Mth.sin(model.attackTime * Mth.PI);
        arm.xRot -= stroke * 1.2F + swing * -(model.head.xRot - 0.7F) * 0.75F;
        arm.yRot += model.body.yRot * 2F;
        arm.zRot -= swing * 0.4F;
    }

    private static boolean mayAnimateUse(LivingEntity entity, InteractionHand hand)
    {
        return (!(entity instanceof IEquipmentPolicy policy) || policy.flansWeaponAnimations()) && entity.isUsingItem() && entity.getUsedItemHand() == hand;
    }

    /** Runs after native animation; the only path allowed to replace a shield's required rotations. */
    public static void customArms(HumanoidModel<?> model, LivingEntity entity)
    {
        for (InteractionHand hand : InteractionHand.values())
        {
            ArmPoseRule rule = HeldItemPresentation.rule(entity, hand);
            ModelPart arm = (hand == InteractionHand.MAIN_HAND) == (entity.getMainArm() == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
            if (rule.pose() == ArmPoseRule.Pose.RELAXED && GunArmPoses.classify(entity.getItemInHand(hand)) != GunArmPoses.HandItem.SHIELD)
                arm.xRot = arm.yRot = arm.zRot = 0F;
            if (rule.pose() != ArmPoseRule.Pose.CUSTOM)
                continue;
            if ((rule.axes() & 1) != 0)
                arm.xRot = rule.x() * Mth.DEG_TO_RAD;
            if ((rule.axes() & 2) != 0)
                arm.yRot = rule.y() * Mth.DEG_TO_RAD;
            if ((rule.axes() & 4) != 0)
                arm.zRot = rule.z() * Mth.DEG_TO_RAD;
        }
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
