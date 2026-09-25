package com.flansmodultimate.platform.client;

import com.flansmodultimate.client.ModClient;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import net.minecraft.client.model.HumanoidModel;

/**
 * Version boundary for custom arm poses. Forge 1.20.1 extends {@link HumanoidModel.ArmPose} at runtime;
 * NeoForge 1.21 declares the constant in {@code META-INF/enumextensions.json}, which reads its constructor
 * arguments from {@link #BOTH_ARMS_AIM}. This class is loaded while the enum initializes, so it must stay
 * free of other static state. Client-only.
 */
public final class ArmPosePlatform
{
    /** Constructor arguments {@code (twoHanded, transformer)} of the {@code FLANSMODULTIMATE_BOTH_ARMS_AIM} constant. */
    public static final EnumProxy<HumanoidModel.ArmPose> BOTH_ARMS_AIM = new EnumProxy<>(HumanoidModel.ArmPose.class,
        true, (IArmPoseTransformer) ModClient::poseBothArmsAim);

    private ArmPosePlatform() {}

    /** The two-handed pose for aiming a gun in each hand. */
    public static HumanoidModel.ArmPose bothArmsAim()
    {
        return BOTH_ARMS_AIM.getValue();
    }
}
