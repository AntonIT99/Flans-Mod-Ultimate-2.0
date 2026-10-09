package com.flansmodultimate.platform.client;

import com.flansmodultimate.client.ModClient;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import net.minecraft.client.model.HumanoidModel;

/**
 * Version boundary for custom arm poses. Forge 1.20.1 extends {@link HumanoidModel.ArmPose} at runtime;
 * NeoForge 1.21 declares the constants in {@code META-INF/enumextensions.json}, which reads their constructor
 * arguments from the {@link EnumProxy} fields below. This class is loaded while the enum initializes, so it
 * must stay free of other static state. Client-only.
 */
public final class ArmPosePlatform
{
    /** Constructor arguments {@code (twoHanded, transformer)} of the {@code FLANSMODULTIMATE_BOTH_ARMS_AIM} constant. */
    public static final EnumProxy<HumanoidModel.ArmPose> BOTH_ARMS_AIM = new EnumProxy<>(HumanoidModel.ArmPose.class, true, (IArmPoseTransformer) ModClient::poseBothArmsAim);
    /** Constructor arguments {@code (twoHanded, transformer)} of the {@code FLANSMODULTIMATE_ONE_ARM_AIM} constant. */
    public static final EnumProxy<HumanoidModel.ArmPose> ONE_ARM_AIM = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) ModClient::poseOneArmAim);
    /** Constructor arguments {@code (twoHanded, transformer)} of the {@code FLANSMODULTIMATE_ONE_ARM_THROW} constant. */
    public static final EnumProxy<HumanoidModel.ArmPose> ONE_ARM_THROW = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) ModClient::poseOneArmThrow);
    /** Constructor arguments {@code (twoHanded, transformer)} of the {@code FLANSMODULTIMATE_BOW_SUPPORT} constant. */
    public static final EnumProxy<HumanoidModel.ArmPose> BOW_SUPPORT = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) ModClient::poseBowSupport);

    private ArmPosePlatform()
    {}

    /** The two-handed pose for aiming a gun in each hand. */
    public static HumanoidModel.ArmPose bothArmsAim()
    {
        return BOTH_ARMS_AIM.getValue();
    }

    /** Raises only the arm holding the gun or shield, leaving the other arm to its own pose. */
    public static HumanoidModel.ArmPose oneArmAim()
    {
        return ONE_ARM_AIM.getValue();
    }

    /** Draws the arm charging a throw back over the shoulder, the other arm keeping its gun pose. */
    public static HumanoidModel.ArmPose oneArmThrow()
    {
        return ONE_ARM_THROW.getValue();
    }

    /** Keeps a free arm reaching across as in the bow pose while the other arm charges a throw. */
    public static HumanoidModel.ArmPose bowSupport()
    {
        return BOW_SUPPORT.getValue();
    }
}
