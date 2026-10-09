package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.physics.VehiclePhysicsUnits;

/** Pure control calculations shared by driveable simulations. */
public final class DriveableControlPhysics
{
    private static final float CONTROL_RETENTION = 0.9F;
    private static final float MAX_CONTROL_ANGLE = 20F;
    private static final float HELD_CONTROL_ANGLE = CONTROL_RETENTION / (1F - CONTROL_RETENTION);
    /** Aircraft taxi steering is measured at walking/slow taxi speed, not maximum flight speed. */
    public static final double PLANE_GROUND_STEERING_REFERENCE_KMH = 10D;

    private DriveableControlPhysics()
    {}

    /**
     * Authored hull yaw rate, independent of engine speed and legacy steering modifiers.
     * The held-key recurrence tends to 9, not MAX_CONTROL_ANGLE. Single-track control
     * already carries its half-authority factor. Rolling vehicles reverse steering
     * with their travel direction and cannot pivot while stationary.
     */
    public static float realSteeringYawDelta(float rateDegPerSec, float control, boolean tracked, boolean engineActive, double signedSpeed, double referenceSpeed)
    {
        if (!Float.isFinite(rateDegPerSec) || rateDegPerSec <= 0F || !Float.isFinite(control))
            return 0F;
        double speedFactor;
        if (tracked)
            speedFactor = engineActive ? 1D : 0D;
        else
            speedFactor = Double.isFinite(signedSpeed) && Double.isFinite(referenceSpeed) && referenceSpeed > 0D ? Math.max(-1D, Math.min(1D, signedSpeed / referenceSpeed)) : 0D;
        return (float) (rateDegPerSec / VehiclePhysicsUnits.TICKS_PER_SECOND * clamp(control / HELD_CONTROL_ANGLE, -1F, 1F) * speedFactor);
    }

    /** The automatic broken-track turn uses the held-input range in the real-rate model. */
    public static float realSingleTrackTurnControl(float throttle, float steeringControl, boolean steeringHeld, boolean leftTrackIntact, boolean rightTrackIntact)
    {
        float control = singleTrackTurnControl(throttle, steeringControl, steeringHeld, leftTrackIntact, rightTrackIntact);
        return steeringHeld ? control : control * HELD_CONTROL_ANGLE / MAX_CONTROL_ANGLE;
    }

    /** Replaces only wheel-supported aircraft yaw; an airborne or forced-legacy plane keeps flight yaw. */
    public static float planeGroundSteeringYawDelta(float flightYaw, float rate, float control, double signedSpeed, boolean wheelSupported, boolean forceLegacy)
    {
        if (forceLegacy || !wheelSupported || !Float.isFinite(rate) || rate <= 0F)
            return flightYaw;
        return realSteeringYawDelta(rate, control, false, false, signedSpeed, VehiclePhysicsUnits.kmhToBlocksPerTick(PLANE_GROUND_STEERING_REFERENCE_KMH));
    }

    /** Steering follows rolling direction, even with a neutral throttle or an inactive engine. */
    public static double wheeledSteeringVelocityScale(double signedSpeed)
    {
        // Match the legacy full-throttle steering scale at its nominal 0.32 speed factor.
        return Double.isFinite(signedSpeed) ? signedSpeed * (0.1D / 0.32D) : 0D;
    }

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

    /** Interpolates the authored forward curve; reverse traverses it by its speed ratio. */
    public static float engineSoundPitch(float throttle, EngineSoundPitch curve, float reverseSpeedRatio)
    {
        float magnitude = Float.isFinite(throttle) ? clamp(Math.abs(throttle), 0F, 1F) : 0F;
        if (throttle < 0F)
            magnitude = clamp(magnitude * (Float.isFinite(reverseSpeedRatio) ? Math.max(0F, reverseSpeedRatio) : 0F), 0F, 1F);
        float pitch = magnitude <= 0.5F
            ? curve.base() + (curve.half() - curve.base()) * (magnitude * 2F)
            : curve.half() + (curve.full() - curve.half()) * ((magnitude - 0.5F) * 2F);
        return Float.isFinite(pitch) ? Math.max(0.01F, pitch) : 0.01F;
    }

    /** The ground brake takes priority over both pedals and persistent throttle keys. */
    public static int vehicleThrottleInput(int input)
    {
        if (DriveableInput.isDown(input, DriveableInput.BRAKE | DriveableInput.ASCEND))
            return input & ~(DriveableInput.FORWARD | DriveableInput.BACKWARD | DriveableInput.THROTTLE_INCREASE | DriveableInput.THROTTLE_DECREASE);
        return input;
    }

    /** Moves a brake-held throttle lever toward neutral without snapping it there. */
    public static float brakedThrottle(float throttle, float step)
    {
        if (!Float.isFinite(throttle))
            return 0F;
        float amount = Float.isFinite(step) ? Math.max(0F, step) : 0F;
        if (throttle > 0F)
            return Math.max(0F, throttle - amount);
        return Math.min(0F, throttle + amount);
    }

    /** Signed propulsion after applying the configured forward / reverse / water power. */
    public static float directionalPropulsion(float throttle, float forwardPower, float reversePower, float waterPower, boolean inWater)
    {
        float normalized = normalizedThrottle(throttle, reversePower);
        if (normalized == 0F)
            return 0F;
        float configured = normalized > 0F ? (inWater ? nonNegative(waterPower) : nonNegative(forwardPower)) : nonNegative(reversePower);
        return normalized * configured;
    }

    /** Matches the 1.7.10 per-key-tick input followed by its 0.9 resting decay. */
    public static float dampedControl(float current, float input, float inputStep)
    {
        if (!Float.isFinite(current) || !Float.isFinite(input) || !Float.isFinite(inputStep))
            return 0F;
        return clamp((current + input * Math.max(0F, inputStep)) * CONTROL_RETENTION, -MAX_CONTROL_ANGLE, MAX_CONTROL_ANGLE);
    }

    /**
     * Converts the surviving side of a tracked vehicle into rotation-only control.
     *
     * <p>
     * A steering key keeps its ordinary turn direction. Without a steering key,
     * forward or reverse drives the remaining track and therefore yaws toward the
     * broken side. One moving track has half the turning authority of two tracks
     * counter-rotating.
     * </p>
     */
    public static float singleTrackTurnControl(float throttle, float steeringControl, boolean steeringHeld, boolean leftTrackIntact, boolean rightTrackIntact)
    {
        if (leftTrackIntact == rightTrackIntact)
            return 0F;
        if (steeringHeld)
            return clamp(steeringControl, -MAX_CONTROL_ANGLE, MAX_CONTROL_ANGLE) * 0.5F;
        float safeThrottle = Float.isFinite(throttle) ? clamp(throttle, -1F, 1F) : 0F;
        float survivingSide = rightTrackIntact ? 1F : -1F;
        return safeThrottle * survivingSide * MAX_CONTROL_ANGLE * 0.5F;
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
        return Math.min(1F, Math.abs(throttle)) * 0.4F * (nonNegative(configuredPower) + nonNegative(engineSpeed));
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
