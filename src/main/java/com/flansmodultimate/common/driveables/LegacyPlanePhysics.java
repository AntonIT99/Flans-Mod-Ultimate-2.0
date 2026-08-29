package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;

/** Pure 1.7.10 aircraft control calculations used by the authoritative plane simulation. */
public final class LegacyPlanePhysics
{
    public static final float GRAVITY = 0.98F / 10F;
    public static final float MAX_FLAP_ANGLE = 20F;
    private static final double PROPELLER_FULL_THROTTLE_RADIANS = 1.5D;
    private static final double PROPELLER_THROTTLE_EXPONENT = 0.4D;
    private static final double ROTOR_THROTTLE_DIVISOR = 7D;
    /** The legacy renderer scaled the raw rotor accumulator by this factor. */
    private static final double ROTOR_RENDER_SCALE = 1440D / Math.PI;

    private LegacyPlanePhysics() {}

    public static float flap(float current, float input)
    {
        return Mth.clamp((finite(current) + finite(input)) * 0.9F, -MAX_FLAP_ANGLE, MAX_FLAP_ANGLE);
    }

    /**
     * Degrees a propeller turns in one tick. Blades stand still on standby and
     * spin up sharply off idle, which is what the legacy throttle^0.4 curve
     * describes; the propeller always turns the same way, even in reverse.
     */
    public static float propellerStep(float throttle)
    {
        float magnitude = Math.abs(finite(throttle));
        return magnitude == 0F ? 0F
            : (float) (Math.pow(magnitude, PROPELLER_THROTTLE_EXPONENT) * PROPELLER_FULL_THROTTLE_RADIANS
                * (180D / Math.PI));
    }

    /** Degrees a rotor turns in one tick. Unlike the propeller this is linear and signed. */
    public static float rotorStep(float throttle)
    {
        float rate = finite(throttle);
        return rate == 0F ? 0F : (float) (rate / ROTOR_THROTTLE_DIVISOR * ROTOR_RENDER_SCALE);
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

    /**
     * Control rates for an aircraft running the real-world profile.
     *
     * <p>Identical in shape to {@link #controlRates}, but the speed-dependent
     * sensitivity is supplied by the caller as a normalised authority in
     * {@code [0, 1]} derived from the aircraft's own terminal speed, instead of
     * the legacy curve's fixed 0.5 / 1 / 3 blocks-per-tick breakpoints. Those
     * breakpoints assumed a two blocks-per-tick top speed and drop authority to
     * zero above three, which is what made fast aircraft uncontrollable.
     *
     * <p>The legacy method is left exactly as it was; legacy aircraft never
     * reach this one.
     */
    public static ControlRates derivedControlRates(float authority, float flapYaw, float flapPitch, float flapRoll,
                                                   float turnLeft, float turnRight, float lookUp, float lookDown,
                                                   float rollLeft, float rollRight)
    {
        float sensitivity = Mth.clamp(finite(authority), 0F, 1F) * 0.125F;
        return new ControlRates(
            finite(flapYaw) * (flapYaw > 0F ? finite(turnLeft) : finite(turnRight)) * sensitivity,
            finite(flapPitch) * (flapPitch > 0F ? finite(lookUp) : finite(lookDown)) * sensitivity,
            finite(flapRoll) * (flapRoll > 0F ? finite(rollLeft) : finite(rollRight)) * sensitivity);
    }

    public static float approachMomentum(float current, float target)
    {
        return approachMomentum(current, target, 1F);
    }

    /**
     * Angular momentum slew with a response multiplier. A multiplier of one is
     * the legacy behaviour exactly; the real-world path passes a factor derived
     * from wing span and mass so a heavy, long-winged aircraft rolls in more
     * slowly than a light one.
     */
    public static float approachMomentum(float current, float target, float responseScale)
    {
        if (!Float.isFinite(current) || !Float.isFinite(target) || !Float.isFinite(responseScale))
            return 0F;
        float step = Math.max(0.05F, responseScale);
        return Mth.clamp(current < target ? Math.min(target, current + step)
            : Math.max(target, current - step), -20F, 20F);
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

    public static boolean isLiftingOff(EnumPlaneMode mode, double horizontalSpeed, float takeoffSpeed,
                                       double forwardVertical, double verticalSpeed)
    {
        if (mode != EnumPlaneMode.PLANE || !Double.isFinite(horizontalSpeed)
            || !Double.isFinite(forwardVertical) || !Double.isFinite(verticalSpeed))
            return false;
        double requiredSpeed = Math.max(0.15D, finite(takeoffSpeed));
        return horizontalSpeed >= requiredSpeed && forwardVertical > 0.02D && verticalSpeed > 0D;
    }

    private static float finite(float value)
    {
        return Float.isFinite(value) ? value : 0F;
    }

    public record ControlRates(float yaw, float pitch, float roll) {}
}
