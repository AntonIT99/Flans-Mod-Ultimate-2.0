package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure movement calculations shared by server mecha physics and client animation. */
public final class MechaPhysics
{
    /** Legacy mechas converted their configured blocks-per-second speed to blocks per tick with this factor. */
    public static final double LEGACY_SPEED_PER_TICK = 4.3D / 20D;

    private MechaPhysics() {}

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
        // Legacy local X (forward) converts to modern local -Z, while legacy
        // local Z (right) converts to modern local +X.
        Vec3 intent = new Vec3(-cosine * forwardInput - sine * strafeInput, 0D,
            -sine * forwardInput + cosine * strafeInput);
        return intent.lengthSqr() > 1D ? intent.normalize() : intent;
    }

    public static double movementSpeed(float configuredSpeed, float engineSpeed, float addonMultiplier)
    {
        if (!Float.isFinite(configuredSpeed) || !Float.isFinite(engineSpeed) || !Float.isFinite(addonMultiplier))
            return 0D;
        return Math.max(0D, configuredSpeed) * Math.max(0D, engineSpeed)
            * Math.max(0D, addonMultiplier) * LEGACY_SPEED_PER_TICK;
    }

    /** Returns the legacy mecha-body yaw that faces the supplied world direction. */
    public static float movementYaw(Vec3 intent, float fallbackYaw)
    {
        if (intent == null || !Double.isFinite(intent.x) || !Double.isFinite(intent.z)
            || intent.x * intent.x + intent.z * intent.z < 1.0E-8D)
            return Mth.wrapDegrees(Float.isFinite(fallbackYaw) ? fallbackYaw : 0F);
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-intent.z, -intent.x)));
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
