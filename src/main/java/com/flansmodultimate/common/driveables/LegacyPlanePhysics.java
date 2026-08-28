package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;

/** Pure 1.7.10 aircraft control calculations used by the authoritative plane simulation. */
public final class LegacyPlanePhysics
{
    public static final float GRAVITY = 0.98F / 10F;
    public static final float MAX_FLAP_ANGLE = 20F;

    private LegacyPlanePhysics() {}

    public static float flap(float current, float input)
    {
        return Mth.clamp((finite(current) + finite(input)) * 0.9F, -MAX_FLAP_ANGLE, MAX_FLAP_ANGLE);
    }

    public static ControlRates controlRates(EnumPlaneMode mode, float speed, float horizontalSpeed, float throttle,
                                            float flapYaw, float flapPitch, float flapRoll,
                                            float turnLeft, float turnRight, float lookUp, float lookDown,
                                            float rollLeft, float rollRight)
    {
        float sensitivity;
        float yawSensitivity;
        if (mode == EnumPlaneMode.HELI)
        {
            sensitivity = throttle > 0.5F ? 1.5F - throttle : 4F * throttle - 1F;
            sensitivity = Math.max(0F, sensitivity);
            yawSensitivity = sensitivity;
        }
        else
        {
            float safeSpeed = Math.max(0F, finite(speed));
            sensitivity = safeSpeed < 0.5F ? 0F
                : safeSpeed < 1F ? 2F * safeSpeed - 1F
                : safeSpeed < 3F ? 1.5F - safeSpeed * 0.5F : 0F;
            float divisor = (float)Math.sqrt(Math.max(1.0E-4F, finite(turnRight)));
            yawSensitivity = horizontalSpeed < 0.7F ? 2.5F * safeSpeed / divisor : sensitivity;
        }
        sensitivity *= 0.125F;
        yawSensitivity *= 0.125F;
        return new ControlRates(
            finite(flapYaw) * (flapYaw > 0F ? finite(turnLeft) : finite(turnRight)) * yawSensitivity,
            finite(flapPitch) * (flapPitch > 0F ? finite(lookUp) : finite(lookDown)) * sensitivity,
            finite(flapRoll) * (flapRoll > 0F ? finite(rollLeft) : finite(rollRight)) * sensitivity);
    }

    public static float approachMomentum(float current, float target)
    {
        if (!Float.isFinite(current) || !Float.isFinite(target))
            return 0F;
        return Mth.clamp(current < target ? Math.min(target, current + 1F)
            : Math.max(target, current - 1F), -20F, 20F);
    }

    public static float drag(float configuredDrag)
    {
        return Mth.clamp(1F - 0.05F * Math.max(0F, finite(configuredDrag)), 0F, 1F);
    }

    public static float thrust(float throttle, float forwardPower, float reversePower, float waterPower,
                               float engineSpeed, boolean underWater)
    {
        float configured = throttle > 0F ? (underWater ? waterPower : forwardPower) : reversePower;
        return 0.01F * Math.max(0F, finite(configured) + finite(engineSpeed));
    }

    private static float finite(float value)
    {
        return Float.isFinite(value) ? value : 0F;
    }

    public record ControlRates(float yaw, float pitch, float roll) {}
}
