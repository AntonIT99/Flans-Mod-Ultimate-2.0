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
     * The fraction of available thrust the throttle lever is calling for.
     *
     * <p>The exponent decides what the lever actually meters. At the default of
     * one it meters engine power directly, which is what a real throttle does:
     * half throttle is half power. Because level-flight drag power rises with
     * the cube of airspeed, that still yields roughly 79% of top speed, which is
     * why an aircraft cruises fast on a modest power setting.
     *
     * <p>Raising the exponent trades that realism for a tidier readout. At three
     * the lever becomes linear in speed instead — half throttle gives half of top
     * speed — but the engine is then only producing an eighth of its rated power
     * at that setting, which is not physical.
     *
     * @param throttle          driver demand, clamped into {@code [0, 1]}
     * @param responseExponent  the configured exponent; one is the physical value
     */
    public static double throttleThrustFactor(double throttle, double responseExponent)
    {
        if (!Double.isFinite(throttle) || throttle <= 0D)
            return 0D;
        double demand = Math.min(1D, throttle);
        if (!Double.isFinite(responseExponent) || responseExponent <= 0D || responseExponent == 1D)
            return demand;
        return Math.pow(demand, responseExponent);
    }

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
     * Induced-drag constant {@code k_i} in N·(m/s)², so that induced drag is
     * {@code k_i / v²}.
     *
     * <p>This is the drag the wing pays for making lift, from the standard
     * {@code D_i = 2 W² / (rho * pi * b² * e * v²)}. It is the term a single
     * {@code k * v²} calibration misses entirely, and it is the dominant one at
     * cruise: without it an aircraft flying well below its top speed sees almost
     * no drag at all and never decelerates.
     *
     * @param massKg    aircraft mass in kilograms
     * @param wingSpanM wing span in metres, or a non-positive value if unavailable
     */
    public static double inducedDragConstant(double massKg, double wingSpanM)
    {
        if (!finitePositive(massKg) || !finitePositive(wingSpanM))
            return 0D;
        double weight = massKg * VehiclePhysicsUnits.STANDARD_GRAVITY;
        return 2D * weight * weight / (VehiclePhysicsUnits.AIR_DENSITY * Math.PI * wingSpanM * wingSpanM
            * VehiclePhysicsConstants.OSWALD_EFFICIENCY);
    }

    /**
     * Total aerodynamic drag in newtons at the given airspeed.
     *
     * <p>Drag is split into the two terms a real airframe actually has: a
     * parasitic term rising with {@code v²} and an induced term falling with
     * {@code 1 / v²}. The parasitic coefficient is back-solved so the two together
     * equal the available thrust at the authored top speed, which keeps
     * {@code RealMaxSpeedKmh} authoritative exactly as before while giving a
     * realistic — and far larger — drag figure at cruise and manoeuvring speeds.
     *
     * @param wingSpanM wing span in metres; non-positive falls back to the pure {@code v²} model
     */
    public static double dragNewtons(double airspeedMs, double massKg, double wingSpanM,
                                     double terminalSpeedMs, double referenceThrustNewtons)
    {
        if (!finitePositive(terminalSpeedMs) || !finitePositive(referenceThrustNewtons))
            return 0D;
        double speed = Double.isFinite(airspeedMs) ? Math.max(0D, Math.abs(airspeedMs)) : 0D;
        double terminalSquared = terminalSpeedMs * terminalSpeedMs;
        // Never let the induced term eat the whole thrust budget: a very heavy,
        // short-span airframe would otherwise leave no parasitic drag at all.
        double induced = Math.min(inducedDragConstant(massKg, wingSpanM),
            referenceThrustNewtons * VehiclePhysicsConstants.MAX_INDUCED_DRAG_SHARE * terminalSquared);
        double parasiticCoefficient = (referenceThrustNewtons - induced / terminalSquared) / terminalSquared;
        double drag = parasiticCoefficient * speed * speed;
        if (induced > 0D)
        {
            // Below the knee the 1/v² term diverges; hold it flat and fade it
            // out toward standstill instead. That is also where a real wing has
            // departed into stall and is no longer making the lift being paid for.
            double kneeSpeed = Math.max(VehiclePhysicsConstants.MIN_LAUNCH_SPEED_MS,
                terminalSpeedMs * VehiclePhysicsConstants.INDUCED_DRAG_KNEE_FRACTION);
            double effective = Math.max(kneeSpeed, speed);
            drag += induced / (effective * effective) * Math.min(1D, speed / kneeSpeed);
        }
        return drag;
    }

    /**
     * Longitudinal acceleration in m/s² for a caller with no span data and no
     * throttle position: the pure {@code v²} drag model, with no coasting floor.
     */
    public static double accelerationMs2(double thrustNewtons, double massKg, double airspeedMs,
                                         double terminalSpeedMs, double referenceThrustNewtons)
    {
        return accelerationMs2(thrustNewtons, massKg, airspeedMs, terminalSpeedMs, referenceThrustNewtons, 0D, 1D);
    }

    /**
     * Longitudinal acceleration in m/s² from thrust against the two-term drag
     * model, plus a coasting floor. Drag is still calibrated so full thrust at
     * the authored top speed nets zero.
     *
     * <p>The floor stands in for the drag of an idled or windmilling propeller
     * disc, which is substantial on a piston fighter and is not expressible in
     * the authored data. It is gated on the throttle lever rather than on thrust,
     * because that is what the propeller is actually responding to, and it ramps
     * in with speed so it never pins a taxiing aircraft to the runway.
     *
     * @param throttleDemand the fraction of available thrust the lever is calling for
     */
    public static double accelerationMs2(double thrustNewtons, double massKg, double airspeedMs,
                                         double terminalSpeedMs, double referenceThrustNewtons,
                                         double wingSpanM, double throttleDemand)
    {
        if (!finitePositive(massKg) || !finitePositive(terminalSpeedMs) || !finitePositive(referenceThrustNewtons))
            return 0D;
        double speed = Double.isFinite(airspeedMs) ? Math.max(0D, Math.abs(airspeedMs)) : 0D;
        double thrust = Double.isFinite(thrustNewtons) ? Math.max(0D, thrustNewtons) : 0D;
        double drag = dragNewtons(speed, massKg, wingSpanM, terminalSpeedMs, referenceThrustNewtons);
        double acceleration = (thrust - drag) / massKg;
        if (!Double.isFinite(acceleration))
            return 0D;

        // Idle or near-idle lever: guarantee a minimum bleed rate, ramped in
        // with speed so a stationary aircraft is unaffected.
        double demand = Double.isFinite(throttleDemand) ? throttleDemand : 1D;
        if (demand <= VehiclePhysicsConstants.IDLE_THROTTLE_FRACTION)
        {
            double ramp = Math.min(1D, speed / Math.max(VehiclePhysicsConstants.MIN_LAUNCH_SPEED_MS,
                terminalSpeedMs * VehiclePhysicsConstants.COAST_DECELERATION_RAMP_FRACTION));
            acceleration = Math.min(acceleration,
                -VehiclePhysicsConstants.MIN_AIRCRAFT_COAST_DECELERATION_MS2 * ramp);
        }
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
     * Scaled and playability-clamped reference speed. The scale deliberately
     * changes takeoff, low-speed lift and stall-like behaviour as one coherent
     * quantity. The result is never allowed above
     * {@link VehiclePhysicsConstants#MAX_REFERENCE_SPEED_FRACTION} of terminal
     * speed, so no aircraft becomes impossible to keep airborne.
     */
    public static double clampedReferenceSpeedMs(double massKg, double wingAreaM2, double terminalSpeedMs,
                                                  double referenceSpeedScale)
    {
        double scale = finitePositive(referenceSpeedScale) ? referenceSpeedScale : 1D;
        double reference = referenceSpeedMs(massKg, wingAreaM2) * scale;
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

    /**
     * Additional aerodynamic deceleration while changing attitude.
     *
     * <p>The simulation has no angle-of-attack or sideslip state, so ordinary
     * calibrated straight-line drag cannot see a turn. Actual body-axis rates
     * stand in for that missing state: yaw is weighted most heavily, pitch next,
     * and roll least. The result scales with airspeed, as aerodynamic losses do,
     * and shares the normal acceleration safety ceiling.
     *
     * @param yawDegPerTick actual local yaw rate in degrees per tick
     * @param pitchDegPerTick actual local pitch rate in degrees per tick
     * @param rollDegPerTick actual local roll rate in degrees per tick
     */
    public static double maneuverDecelerationMs2(double airspeedMs, float yawDegPerTick,
                                                  float pitchDegPerTick, float rollDegPerTick)
    {
        if (!Double.isFinite(airspeedMs) || airspeedMs <= 0D)
            return 0D;
        double yawRate = radiansPerSecond(yawDegPerTick);
        double pitchRate = radiansPerSecond(pitchDegPerTick);
        double rollRate = radiansPerSecond(rollDegPerTick);
        double dragRate = yawRate * VehiclePhysicsConstants.AIRCRAFT_YAW_MANEUVER_DRAG
            + pitchRate * VehiclePhysicsConstants.AIRCRAFT_PITCH_MANEUVER_DRAG
            + rollRate * VehiclePhysicsConstants.AIRCRAFT_ROLL_MANEUVER_DRAG;
        return Math.min(VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2, airspeedMs * dragRate);
    }

    /**
     * Self-correcting nose-down rate in degrees per tick for an aircraft that is
     * slow and nose-high.
     *
     * <p>A real wing at too high an angle of attack stops carrying the aircraft
     * and the nose falls of its own accord. The derived model has no angle of
     * attack, so this stands in for it: below the reference airspeed the nose is
     * eased back toward the horizon in proportion to both how slow the aircraft
     * is and how steeply it is pointed up. The pilot keeps full authority — this
     * is a bias on the attitude, not a takeover — but hanging on the propeller
     * until the aircraft mushes into the ground is no longer possible.
     *
     * @param pitchDegrees the simulation pitch, where negative is nose up
     * @return degrees to add to the pitch, always zero or positive, i.e. never
     *         raising the nose and never pushing past level
     */
    public static float stallRecoveryPitchDegrees(double airspeedMs, double referenceSpeedMs, float pitchDegrees)
    {
        if (!finitePositive(referenceSpeedMs) || !Double.isFinite(airspeedMs) || !Float.isFinite(pitchDegrees))
            return 0F;
        // Only a nose-up attitude can be eased; a diving aircraft is recovering.
        if (pitchDegrees >= 0F)
            return 0F;
        double onset = referenceSpeedMs * VehiclePhysicsConstants.STALL_RECOVERY_ONSET_FRACTION;
        if (onset <= 0D)
            return 0F;
        double deficit = (onset - Math.max(0D, Math.abs(airspeedMs))) / onset;
        if (deficit <= 0D)
            return 0F;
        double steepness = Math.min(1D, -pitchDegrees / VehiclePhysicsConstants.STALL_RECOVERY_FULL_PITCH_DEG);
        double correction = VehiclePhysicsConstants.STALL_RECOVERY_MAX_DEG_PER_TICK
            * Math.min(1D, deficit) * steepness;
        // Never push the nose below the horizon in a single correction.
        return (float) Math.min(correction, -pitchDegrees);
    }

    /**
     * Deceleration in m/s² from the undercarriage while the aircraft is rolling
     * on its wheels, on top of whatever aerodynamic drag it already has.
     *
     * <p>Rolling resistance is always present; wheel braking is added once the
     * throttle is closed, which is what a pilot does on the landing roll. Neither
     * is expressible in the authored real-world data.
     *
     * @param throttleDemand the fraction of available thrust the lever is calling for
     */
    public static double groundDecelerationMs2(double throttleDemand)
    {
        double demand = Double.isFinite(throttleDemand) ? Math.max(0D, throttleDemand) : 0D;
        double deceleration = VehiclePhysicsConstants.GROUND_ROLLING_DECELERATION_MS2;
        if (demand <= VehiclePhysicsConstants.IDLE_THROTTLE_FRACTION)
            deceleration += VehiclePhysicsConstants.GROUND_BRAKING_DECELERATION_MS2;
        return deceleration;
    }

    private static boolean finitePositive(double value)
    {
        return Double.isFinite(value) && value > 0D;
    }

    private static double radiansPerSecond(float degreesPerTick)
    {
        return Float.isFinite(degreesPerTick)
            ? Math.toRadians(Math.abs(degreesPerTick) * VehiclePhysicsUnits.TICKS_PER_SECOND) : 0D;
    }
}
