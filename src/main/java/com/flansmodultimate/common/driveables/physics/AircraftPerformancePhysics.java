package com.flansmodultimate.common.driveables.physics;

/**
 * Derived fixed-wing performance for aircraft running the real-world profile.
 *
 * <p>Three quantities are derived here, all from data a reference book actually
 * lists: thrust (directly, or from shaft power and a propeller efficiency), a
 * reference airspeed standing in for stall behaviour, and a roll inertia factor.
 *
 * <p>Stall is deliberately soft. Real stall speeds for heavily loaded aircraft
 * are unpleasant at Minecraft scale, so the derived reference speed is clamped to
 * a fraction of the aircraft's own terminal speed and lift falls off smoothly
 * below it rather than vanishing at a hard threshold.
 *
 * <p>Only {@code EnumPlaneMode.PLANE} consumes this. Helicopter, VTOL and six
 * degree of freedom craft keep their legacy behaviour untouched.
 */
public final class AircraftPerformancePhysics
{
    private AircraftPerformancePhysics() {}

    /**
     * Thrust in newtons at the given airspeed.
     *
     * <p>Jet thrust is airspeed independent. Shaft power becomes thrust through
     * {@code T = eta * P / v}, which diverges at standstill, so the same launch
     * knee used for ground vehicles bounds it.
     *
     * @param thrustKn      authored thrust in kilonewtons, or a non-positive value if unavailable
     * @param powerKw       authored shaft power in kilowatts, used when thrust is unavailable
     * @param airspeedMs    current airspeed in m/s
     * @param terminalSpeedMs authored top speed in m/s
     */
    public static double thrustNewtons(double thrustKn, double powerKw, double airspeedMs, double terminalSpeedMs)
    {
        if (finitePositive(thrustKn))
            return thrustKn * VehiclePhysicsUnits.NEWTONS_PER_KILONEWTON;
        if (!finitePositive(powerKw) || !finitePositive(terminalSpeedMs))
            return 0D;
        double launchSpeed = Math.max(VehiclePhysicsConstants.MIN_LAUNCH_SPEED_MS,
            terminalSpeedMs * VehiclePhysicsConstants.LAUNCH_SPEED_FRACTION);
        double speed = Double.isFinite(airspeedMs) ? Math.max(launchSpeed, Math.abs(airspeedMs)) : launchSpeed;
        return VehiclePhysicsConstants.PROPELLER_EFFICIENCY * powerKw * VehiclePhysicsUnits.WATTS_PER_KILOWATT / speed;
    }

    /**
     * Longitudinal acceleration in m/s², using the same calibrated-drag approach
     * as the ground model so the aircraft settles at its authored top speed.
     */
    public static double accelerationMs2(double thrustNewtons, double massKg, double airspeedMs,
                                         double terminalSpeedMs, double referenceThrustNewtons)
    {
        if (!finitePositive(massKg) || !finitePositive(terminalSpeedMs) || !finitePositive(referenceThrustNewtons))
            return 0D;
        double speed = Double.isFinite(airspeedMs) ? Math.max(0D, Math.abs(airspeedMs)) : 0D;
        // Drag is calibrated so that full thrust at terminal speed nets zero.
        double dragCoefficient = referenceThrustNewtons / (terminalSpeedMs * terminalSpeedMs);
        double drag = dragCoefficient * speed * speed;
        double acceleration = (Math.max(0D, thrustNewtons) - drag) / massKg;
        if (!Double.isFinite(acceleration))
            return 0D;
        return Math.max(-VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2,
            Math.min(acceleration, VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2));
    }

    /**
     * Reference airspeed in m/s at which the wing produces exactly enough lift to
     * carry the aircraft, from {@code L = 0.5 * rho * v^2 * S * CLmax = m * g}.
     * This is the physical stall speed before any playability clamp.
     */
    public static double referenceSpeedMs(double massKg, double wingAreaM2)
    {
        if (!finitePositive(massKg) || !finitePositive(wingAreaM2))
            return 0D;
        double wingLoading = massKg / wingAreaM2;
        double value = 2D * wingLoading * VehiclePhysicsUnits.STANDARD_GRAVITY
            / (VehiclePhysicsUnits.AIR_DENSITY * VehiclePhysicsConstants.MAX_LIFT_COEFFICIENT);
        return value <= 0D ? 0D : Math.sqrt(value);
    }

    /**
     * Playability-clamped reference speed. Never allowed above
     * {@link VehiclePhysicsConstants#MAX_REFERENCE_SPEED_FRACTION} of terminal
     * speed, so no aircraft becomes impossible to keep airborne.
     */
    public static double clampedReferenceSpeedMs(double massKg, double wingAreaM2, double terminalSpeedMs)
    {
        double reference = referenceSpeedMs(massKg, wingAreaM2);
        if (reference <= 0D || !finitePositive(terminalSpeedMs))
            return reference;
        return Math.min(reference, terminalSpeedMs * VehiclePhysicsConstants.MAX_REFERENCE_SPEED_FRACTION);
    }

    /**
     * Fraction of weight the wing is currently supporting, as
     * {@code (v / v_ref)^2} clamped to a sane band. At and above the reference
     * speed the wing carries the aircraft; below it, lift falls off with the
     * square of airspeed exactly as it physically should, but never to a hard
     * zero, which keeps a slow aircraft controllable rather than dropping.
     */
    public static double liftFraction(double airspeedMs, double referenceSpeedMs)
    {
        if (!finitePositive(referenceSpeedMs) || !Double.isFinite(airspeedMs))
            return 0D;
        double ratio = Math.max(0D, Math.abs(airspeedMs)) / referenceSpeedMs;
        return Math.min(ratio * ratio, 1.5D);
    }

    /**
     * Excess lift permitted above the weight of the aircraft, expressed as a
     * fraction of weight, constrained so that sustained climb approaches the
     * authored climb rate instead of being unbounded.
     *
     * <p>This is how {@code RealClimbRateMs} enters the model. It is not applied
     * as a vertical velocity: the aircraft still climbs because thrust and lift
     * exceed drag and weight. The climb rate only caps how much of that excess
     * the wing is allowed to convert into a sustained climb.
     *
     * @param climbRateMs     authored climb rate in m/s, or a non-positive value for no constraint
     * @param terminalSpeedMs authored top speed in m/s
     * @return the maximum lift fraction above 1.0, or a permissive default when unconstrained
     */
    public static double maxExcessLiftFraction(double climbRateMs, double terminalSpeedMs)
    {
        if (!finitePositive(climbRateMs) || !finitePositive(terminalSpeedMs))
            return 0.35D;
        // In steady climb the flight path angle is climbRate / airspeed; the
        // excess normal force needed to sustain it scales with that angle.
        double climbAngleRatio = Math.min(1D, climbRateMs / terminalSpeedMs);
        return Math.max(0.05D, Math.min(1D, climbAngleRatio * 2D));
    }

    /**
     * Angular response multiplier from span and mass, normalised so that a
     * reference light aircraft returns roughly 1.0. Larger and heavier aircraft
     * roll and pitch more slowly. Clamped so control never disappears.
     */
    public static float rollInertiaFactor(double wingSpanM, double massKg)
    {
        if (!finitePositive(wingSpanM) || !finitePositive(massKg))
            return 1F;
        double spanRatio = wingSpanM / VehiclePhysicsConstants.REFERENCE_WING_SPAN_M;
        double massRatio = massKg / VehiclePhysicsConstants.REFERENCE_AIRCRAFT_MASS_KG;
        // Roll inertia grows with mass and with the square of span; response is
        // its inverse. The square root keeps the spread playable.
        double inertia = massRatio * spanRatio * spanRatio;
        double response = inertia <= 0D ? 1D : 1D / Math.sqrt(inertia);
        return (float) Math.max(VehiclePhysicsConstants.MIN_ROLL_INERTIA_FACTOR,
            Math.min(VehiclePhysicsConstants.MAX_ROLL_INERTIA_FACTOR, response));
    }

    /**
     * Control authority as a function of how fast the aircraft is going relative
     * to its own terminal speed, replacing the legacy fixed breakpoints at 0.5, 1
     * and 3 blocks per tick.
     *
     * <p>Authority builds from zero at a standstill, peaks around a third of
     * terminal speed where the aircraft is most manoeuvrable, and tapers at high
     * speed as control surfaces load up. Unlike the legacy curve it never reaches
     * zero at speed, which is what made fast aircraft uncontrollable.
     */
    public static float normalizedControlAuthority(double airspeedBlocksPerTick, double terminalBlocksPerTick)
    {
        if (!finitePositive(terminalBlocksPerTick) || !Double.isFinite(airspeedBlocksPerTick))
            return 0F;
        double ratio = Math.max(0D, airspeedBlocksPerTick) / terminalBlocksPerTick;
        double authority;
        if (ratio <= 0.35D)
            authority = ratio / 0.35D;
        else
            authority = 1D - 0.55D * Math.min(1D, (ratio - 0.35D) / 0.65D);
        return (float) Math.max(0D, Math.min(1D, authority));
    }

    private static boolean finitePositive(double value)
    {
        return Double.isFinite(value) && value > 0D;
    }
}
