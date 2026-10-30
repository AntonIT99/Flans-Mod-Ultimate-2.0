package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.guns.GunArmPoses.Arm;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * Head and gun arms of a humanoid lying prone. The renderer tips the body forward a quarter turn about its width, so arms
 * raised as when standing would aim into the ground. Each rotation is instead worked out as when standing and then turned
 * back by the same quarter turn: the head and the gun point where the entity looks. The pitch moves to the X rotation, and
 * the head's yaw, which now turns about the lying body's depth, to the Z rotation. Shared by the model and the server
 * hitboxes, so both agree.
 *
 * <p>
 * Rotations are model part angles in radians, as {@code HumanoidModel} uses them.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProneArmPoses
{
    /** How far the free arm reaches across to support a gun held in the bow pose, as {@code EquipmentAnimationSupport} does. */
    private static final float BOW_SUPPORT_YAW = 0.4F;
    /** How far apart two guns aimed together point, as {@code EquipmentAnimationSupport} does. */
    private static final float BOTH_ARMS_YAW = 0.05F;

    public record Rotation(float x, float y, float z)
    {}

    /** The head, and each arm that holds or supports a gun; a null arm keeps the pose it already has. */
    public record Result(Rotation head, @Nullable Rotation rightArm, @Nullable Rotation leftArm)
    {}

    /**
     * @param poses
     *            the gun pose of each hand
     * @param rightHanded
     *            whether the main hand is the right one
     * @param headPitch
     *            where the entity looks, down positive, in radians
     * @param headYaw
     *            the head's yaw relative to the body, in radians
     * @param lying
     *            how far the body lies flat, from 0 standing to 1 prone
     * @return the rotations to set, or empty when the entity stands or raises no gun
     */
    public static Optional<Result> resolve(GunArmPoses.Result poses, boolean rightHanded, float headPitch, float headYaw, float lying)
    {
        if (lying <= 0F)
            return Optional.empty();
        Arm right = rightHanded ? poses.mainHand() : poses.offHand();
        Arm left = rightHanded ? poses.offHand() : poses.mainHand();
        if (isLowered(right) && isLowered(left))
            return Optional.empty();

        Rotation rightArm = standingArm(right, true, headPitch, headYaw);
        Rotation leftArm = standingArm(left, false, headPitch, headYaw);
        if (right == Arm.BOW && isLowered(left))
            leftArm = new Rotation(headPitch - Mth.HALF_PI, headYaw + BOW_SUPPORT_YAW, 0F);
        else if (left == Arm.BOW && isLowered(right))
            rightArm = new Rotation(headPitch - Mth.HALF_PI, headYaw - BOW_SUPPORT_YAW, 0F);

        return Optional.of(new Result(lieDown(new Rotation(headPitch, headYaw, 0F), lying), rightArm == null ? null : lieDown(rightArm, lying), leftArm == null ? null : lieDown(leftArm, lying)));
    }

    /**
     * Turns a rotation worked out for a standing body back by the forward tip of a body lying down by {@code lying}: about
     * the X axis by the tip, and with the Y and Z axes trading places as the body's length and depth do.
     */
    static Rotation lieDown(Rotation standing, float lying)
    {
        return new Rotation(standing.x() - lying * Mth.HALF_PI, Mth.lerp(lying, standing.y(), standing.z()), Mth.lerp(lying, standing.z(), -standing.y()));
    }

    private static boolean isLowered(Arm arm)
    {
        return arm == Arm.NONE || arm == Arm.RELAXED;
    }

    /** The arm as {@code EquipmentAnimationSupport} raises it on a standing body, or null for an arm it leaves alone. */
    @Nullable
    private static Rotation standingArm(Arm arm, boolean right, float headPitch, float headYaw)
    {
        return switch (arm)
        {
            case NONE, RELAXED -> null;
            case THROW -> new Rotation(headPitch - Mth.PI, headYaw, 0F);
            case BOTH -> new Rotation(-Mth.HALF_PI, right ? -BOTH_ARMS_YAW : BOTH_ARMS_YAW, 0F);
            case ONE_ARM, BOW, SUPPORT -> new Rotation(headPitch - Mth.HALF_PI, headYaw, 0F);
        };
    }
}
