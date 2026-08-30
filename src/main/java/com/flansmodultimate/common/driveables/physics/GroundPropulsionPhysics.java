package com.flansmodultimate.common.driveables.physics;

/**
 * Derived longitudinal acceleration for ground vehicles running the real-world
 * profile.
 *
 * <p>Power to weight is an input to this model, not the answer. Treating kW/kg
 * directly as blocks per tick squared would make acceleration independent of
 * current speed, so a vehicle would either snap to its top speed or never reach
 * it. Instead this is a constant-power tractive force opposed by a resistance
 * term that is calibrated to balance exactly at the authored top speed:
 *
 * <pre>
 * F_traction(v) = P / max(v, v_launch)          capped by the traction limit
 * F_resist(v)   = k * v^2,  with k = P / v_max^3
 * a(v)          = (F_traction(v) - F_resist(v)) / m
 * </pre>
 *
 * <p>Because {@code F_traction(v_max) == F_resist(v_max)} by construction, the
 * vehicle approaches {@code v_max} asymptotically and never overshoots it. All
 * work is done in SI and converted once at the end, so the formulas stay
 * readable and unit-checkable.
 */
public final class GroundPropulsionPhysics
{
    private GroundPropulsionPhysics() {}

    /**
     * Available acceleration in m/s² at the given forward speed.
     *
     * @param speedMs           current forward speed in m/s, always treated as a magnitude
     * @param powerW            usable engine power in watts, after any gameplay engine modifier
     * @param massKg            vehicle mass in kilograms
     * @param terminalSpeedMs   authored top speed in m/s, the point where acceleration reaches zero
     * @param tractionFactor    drive-layout and wheel-damage multiplier on the launch-limited force
     */
    public static double accelerationMs2(double speedMs, double powerW, double massKg,
                                         double terminalSpeedMs, double tractionFactor)
    {
        if (!finitePositive(powerW) || !finitePositive(massKg) || !finitePositive(terminalSpeedMs))
            return 0D;
        double speed = Double.isFinite(speedMs) ? Math.max(0D, Math.abs(speedMs)) : 0D;
        double traction = Double.isFinite(tractionFactor) ? Math.max(0D, tractionFactor) : 0D;

        double launchSpeed = Math.max(VehiclePhysicsConstants.MIN_LAUNCH_SPEED_MS,
            terminalSpeedMs * VehiclePhysicsConstants.LAUNCH_SPEED_FRACTION);
        double tractionForce = powerW / Math.max(launchSpeed, speed);
        // The same power at the launch knee is the strongest force the vehicle can
        // put down; the drive layout scales that ceiling.
        double launchLimit = powerW / launchSpeed * traction;
        tractionForce = Math.min(tractionForce, launchLimit);

        double resistanceCoefficient = powerW / (terminalSpeedMs * terminalSpeedMs * terminalSpeedMs);
        double resistance = resistanceCoefficient * speed * speed;

        double acceleration = (tractionForce - resistance) / massKg;
        if (!Double.isFinite(acceleration))
            return 0D;
        return Math.min(acceleration, VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2);
    }

    /**
     * Per-tick speed change budget in blocks, derived from
     * {@link #accelerationMs2}. Never negative: the caller decides direction.
     */
    public static double accelerationBlocksPerTickSquared(double speedBlocksPerTick, double powerW, double massKg,
                                                          double terminalSpeedBlocksPerTick, double tractionFactor)
    {
        double speedMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(speedBlocksPerTick);
        double terminalMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(terminalSpeedBlocksPerTick);
        double accelerationMs2 = accelerationMs2(speedMs, powerW, massKg, terminalMs, tractionFactor);
        return Math.max(0D, VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(accelerationMs2));
    }

    /**
     * Deceleration budget in blocks per tick squared when the driver is asking
     * for less speed than the vehicle currently has. Uses the resistance term
     * plus a floor, and adds the brake allowance when the brake is held.
     */
    public static double decelerationBlocksPerTickSquared(double speedBlocksPerTick, double powerW, double massKg,
                                                          double terminalSpeedBlocksPerTick, boolean braking)
    {
        double resistance = VehiclePhysicsConstants.MIN_DERIVED_DECELERATION_MS2;
        double speedMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(speedBlocksPerTick);
        double terminalMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(terminalSpeedBlocksPerTick);
        if (finitePositive(powerW) && finitePositive(massKg) && finitePositive(terminalMs) && Double.isFinite(speedMs))
        {
            double coefficient = powerW / (terminalMs * terminalMs * terminalMs);
            resistance = Math.max(resistance, coefficient * speedMs * speedMs / massKg);
        }
        if (braking)
            resistance += VehiclePhysicsConstants.BRAKING_DECELERATION_MS2;
        resistance = Math.min(resistance, VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2
            + VehiclePhysicsConstants.BRAKING_DECELERATION_MS2);
        return Math.max(0D, VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(resistance));
    }

    /**
     * Moves {@code current} toward {@code target} by at most the appropriate
     * per-tick budget. Never overshoots, which is what keeps the integration
     * stable at 20 Hz regardless of how much power a vehicle has.
     */
    public static double approach(double current, double target, double accelerationBudget, double decelerationBudget)
    {
        if (!Double.isFinite(current) || !Double.isFinite(target))
            return 0D;
        // Reducing the magnitude of motion, or reversing against the direction
        // already travelled, is deceleration. Pulling away from a standstill is
        // not: there is no motion to shed.
        boolean slowing = current != 0D
            && (Math.abs(target) < Math.abs(current) || target * current < 0D);
        double budget = Math.max(0D, slowing ? decelerationBudget : accelerationBudget);
        if (current < target)
            return Math.min(target, current + budget);
        return Math.max(target, current - budget);
    }

    /**
     * Horizontal drag applied after longitudinal integration. The derived model
     * already contains resistance calibrated to the authored terminal speed, so
     * applying the legacy per-tick multiplier as well would create a much lower
     * second equilibrium. Legacy vehicles retain their historical multiplier.
     */
    public static double postIntegrationHorizontalDrag(boolean derivedGroundPropulsion, float legacyDrag)
    {
        if (derivedGroundPropulsion)
            return 1D;
        double configured = Float.isFinite(legacyDrag) ? Math.max(0F, legacyDrag) : 1D;
        return Math.max(0.75D, Math.min(0.995D, 0.98D - Math.max(0D, configured - 1D) * 0.01D));
    }

    /**
     * Longitudinal speed lost to tyre or track scrub while cornering. The body
     * yaw rate produces lateral acceleration {@code v * omega}; a bounded share
     * of that acceleration is paid from forward speed so steering no longer
     * rotates a vehicle at its straight-line terminal speed for free.
     */
    public static double applyTurningLoss(double speedBlocksPerTick, float yawDegPerTick)
    {
        if (!Double.isFinite(speedBlocksPerTick) || !Float.isFinite(yawDegPerTick))
            return 0D;
        double speed = Math.abs(speedBlocksPerTick);
        if (speed == 0D || yawDegPerTick == 0F)
            return speedBlocksPerTick;
        double speedMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(speed);
        double yawRadiansPerSecond = Math.toRadians(Math.abs(yawDegPerTick)
            * VehiclePhysicsUnits.TICKS_PER_SECOND);
        double decelerationMs2 = Math.min(VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2,
            speedMs * yawRadiansPerSecond * VehiclePhysicsConstants.GROUND_TURNING_RESISTANCE);
        double loss = VehiclePhysicsUnits.metresPerSecondSquaredToBlocksPerTickSquared(decelerationMs2);
        return Math.copySign(Math.max(0D, speed - loss), speedBlocksPerTick);
    }

    private static boolean finitePositive(double value)
    {
        return Double.isFinite(value) && value > 0D;
    }
}
