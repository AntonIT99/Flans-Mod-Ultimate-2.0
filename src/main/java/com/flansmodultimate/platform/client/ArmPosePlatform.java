package com.flansmodultimate.platform.client;

import com.flansmodultimate.client.ModClient;

import net.minecraft.client.model.HumanoidModel;

/**
 * Version boundary for custom arm poses. Forge 1.20.1 extends {@link HumanoidModel.ArmPose} at runtime;
 * NeoForge 1.21 declares the constant in {@code META-INF/enumextensions.json}. Client-only.
 */
public final class ArmPosePlatform
{
    private static final HumanoidModel.ArmPose BOTH_ARMS_AIM =
        HumanoidModel.ArmPose.create("both_arms_aim", true, ModClient::poseBothArmsAim);

    private ArmPosePlatform() {}

    /** The two-handed pose for aiming a gun in each hand. */
    public static HumanoidModel.ArmPose bothArmsAim()
    {
        return BOTH_ARMS_AIM;
    }
}
