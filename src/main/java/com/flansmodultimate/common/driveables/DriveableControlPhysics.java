package com.flansmodultimate.common.driveables;

/** Pure legacy control calculations shared by driveable simulations. */
public final class DriveableControlPhysics
{
    private static final float CONTROL_RETENTION = 0.9F;
    private static final float MAX_CONTROL_ANGLE = 20F;

    private DriveableControlPhysics() {}

    /**
     * Legacy throttle is a normalized control value. MaxThrottle and
     * MaxNegativeThrottle describe propulsion, not the range of this value.
     */
    public static float normalizedThrottle(float throttle, float reversePower)
    {
        if (!Float.isFinite(throttle))
            return 0F;
        float minimum = Float.isFinite(reversePower) && reversePower > 0F ? -1F : 0F;
        return clamp(throttle, minimum, 1F);
    }

    /** HUD presentation of the normalized control range, independent of propulsion tuning. */
    public static int throttlePercent(float throttle)
    {
        if (!Float.isFinite(throttle))
            return 0;
        return Math.round(clamp(throttle, -1F, 1F) * 100F);
    }

    /** Signed propulsion after applying the configured forward / reverse / water power. */
    public static float directionalPropulsion(float throttle, float forwardPower, float reversePower,
                                               float waterPower, boolean inWater)
    {
        float normalized = normalizedThrottle(throttle, reversePower);
        if (normalized == 0F)
            return 0F;
        float configured = normalized > 0F
            ? (inWater ? nonNegative(waterPower) : nonNegative(forwardPower))
            : nonNegative(reversePower);
        return normalized * configured;
    }

    /** Matches the 1.7.10 per-key-tick input followed by its 0.9 resting decay. */
    public static float dampedControl(float current, float input, float inputStep)
    {
        if (!Float.isFinite(current) || !Float.isFinite(input) || !Float.isFinite(inputStep))
            return 0F;
        return clamp((current + input * Math.max(0F, inputStep)) * CONTROL_RETENTION,
            -MAX_CONTROL_ANGLE, MAX_CONTROL_ANGLE);
    }

    /** Engine-room damage reduced both acceleration and the attainable normalized throttle. */
    public static float damagedThrottleLimit(float damageNerf)
    {
        return clamp(1F - finiteOrZero(damageNerf), 0F, 1F);
    }

    public static float damagedAccelerationMultiplier(float damageNerf)
    {
        return 0.1F + 0.9F * (float) Math.sqrt(damagedThrottleLimit(damageNerf));
    }

    /**
     * Updates the ground-vehicle throttle lever mode. W/S are momentary pedals
     * and always take control back; Q/E select and retain a fixed throttle.
     */
    public static boolean fixedVehicleThrottle(boolean fixed, boolean canControl, boolean braking, int input)
    {
        if (!canControl || braking || DriveableInput.isDown(input, DriveableInput.FORWARD | DriveableInput.BACKWARD))
            return false;
        if (DriveableInput.isDown(input, DriveableInput.THROTTLE_INCREASE | DriveableInput.THROTTLE_DECREASE))
            return true;
        return fixed;
    }

    /** Load for the legacy per-wheel vehicle fuel burn used by Driveable.consumeFuel. */
    public static float vehicleFuelLoad(float throttle, int wheelCount)
    {
        return Math.abs(normalizedThrottle(throttle, 1F)) * 2F * Math.max(0, wheelCount);
    }

    /**
     * Legacy aircraft burned fuel from their thrust term, which was based on
     * configured throttle power plus engine speed rather than propeller count.
     */
    public static float aircraftFuelLoad(float throttle, float configuredPower, float engineSpeed)
    {
        if (!Float.isFinite(throttle))
            return 0F;
        return Math.min(1F, Math.abs(throttle)) * 0.4F
            * (nonNegative(configuredPower) + nonNegative(engineSpeed));
    }

    private static float nonNegative(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, value) : 0F;
    }

    private static float finiteOrZero(float value)
    {
        return Float.isFinite(value) ? value : 0F;
    }

    private static float clamp(float value, float minimum, float maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
