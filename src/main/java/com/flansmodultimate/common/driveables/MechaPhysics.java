package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure movement calculations shared by server mecha physics and client animation. */
public final class MechaPhysics
{
    /** Legacy mechas converted their configured blocks-per-second speed to blocks per tick with this factor. */
    public static final double LEGACY_SPEED_PER_TICK = 4.3D / 20D;
    /** Mounted mecha cameras are defined ninety degrees clockwise from the legacy torso axes. */
    public static final float DRIVER_YAW_OFFSET = 90F;

    private MechaPhysics() {}

    public static float driverMovementYaw(float torsoYaw)
    {
        return Mth.wrapDegrees(torsoYaw + DRIVER_YAW_OFFSET);
    }

    /**
     * Converts normalized forward / strafe input into a horizontal world-space direction.
     * Mecha movement follows the torso aim just as the 1.7.10 torso axes followed the driver view.
     */
    public static Vec3 movementIntent(float yawDegrees, float forwardInput, float strafeInput)
    {
        if (!Float.isFinite(yawDegrees) || !Float.isFinite(forwardInput) || !Float.isFinite(strafeInput))
            return Vec3.ZERO;

        double yaw = yawDegrees * Mth.DEG_TO_RAD;
        double sine = Math.sin(yaw);
        double cosine = Math.cos(yaw);
        // Minecraft yaw zero faces +Z. Its right vector is (-cos(yaw), 0, -sin(yaw)).
        Vec3 intent = new Vec3(-sine * forwardInput - cosine * strafeInput, 0D,
            cosine * forwardInput - sine * strafeInput);
        return intent.lengthSqr() > 1D ? intent.normalize() : intent;
    }

    public static double movementSpeed(float configuredSpeed, float engineSpeed, float addonMultiplier)
    {
        if (!Float.isFinite(configuredSpeed) || !Float.isFinite(engineSpeed) || !Float.isFinite(addonMultiplier))
            return 0D;
        return Math.max(0D, configuredSpeed) * Math.max(0D, engineSpeed)
            * Math.max(0D, addonMultiplier) * LEGACY_SPEED_PER_TICK;
    }

    /** Returns the legacy +X-forward mecha-model yaw that faces the supplied world direction. */
    public static float movementYaw(Vec3 intent, float fallbackYaw)
    {
        if (intent == null || !Double.isFinite(intent.x) || !Double.isFinite(intent.z)
            || intent.x * intent.x + intent.z * intent.z < 1.0E-8D)
            return Mth.wrapDegrees(Float.isFinite(fallbackYaw) ? fallbackYaw : 0F);
        float minecraftYaw = (float) Math.toDegrees(Math.atan2(-intent.x, intent.z));
        return Mth.wrapDegrees(minecraftYaw - DRIVER_YAW_OFFSET);
    }

    /** Legacy RotateSpeed is an angular limit in degrees per tick, not a scaled steering input. */
    public static float approachYaw(float currentYaw, float targetYaw, float rotateSpeed)
    {
        if (!Float.isFinite(currentYaw) || !Float.isFinite(targetYaw))
            return 0F;
        float step = Float.isFinite(rotateSpeed) ? Math.max(0F, rotateSpeed) : 0F;
        float difference = Mth.wrapDegrees(targetYaw - currentYaw);
        return Mth.wrapDegrees(currentYaw + Mth.clamp(difference, -step, step));
    }

    /** Mechas have no reverse gear; throttle represents walking effort in every direction. */
    public static float throttle(float forwardInput, float strafeInput)
    {
        if (!Float.isFinite(forwardInput) || !Float.isFinite(strafeInput))
            return 0F;
        return Mth.clamp(Mth.sqrt(forwardInput * forwardInput + strafeInput * strafeInput), 0F, 1F);
    }
}
