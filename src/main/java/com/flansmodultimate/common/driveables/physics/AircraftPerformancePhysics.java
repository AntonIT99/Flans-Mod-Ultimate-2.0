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
        return dragNewtons(airspeedMs, massKg, wingSpanM, terminalSpeedMs, referenceThrustNewtons, 1D);
    }

    /**
     * Total aerodynamic drag in newtons, with the induced term scaled by the
     * square of the load factor the wing is actually carrying.
     *
     * <p>Induced drag is the price of lift, so pulling more than one g costs
     * more than one g's worth of it: {@code D_i} scales with {@code n²}. Only
     * the induced term moves. The parasitic coefficient is still back-solved at
     * one g, which is what keeps the authored top speed authoritative in the
     * level flight it was measured in.
     *
     * @param loadFactor lift as a multiple of weight; one is level flight
     */
    public static double dragNewtons(double airspeedMs, double massKg, double wingSpanM,
                                     double terminalSpeedMs, double referenceThrustNewtons,
                                     double loadFactor)
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
        // Calibration above is at one g; the wing pays n² of that once loaded.
        double load = Double.isFinite(loadFactor)
            ? Math.min(VehiclePhysicsConstants.MAX_MANEUVER_LOAD_FACTOR, Math.max(0D, loadFactor)) : 1D;
        induced *= load * load;
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
        return accelerationMs2(thrustNewtons, massKg, airspeedMs, terminalSpeedMs, referenceThrustNewtons,
            wingSpanM, throttleDemand, 1D);
    }

    /**
     * Longitudinal acceleration in m/s² with the wing carrying the given load
     * factor, so that a hard pull costs speed through induced drag the way it
     * physically must.
     *
     * @param loadFactor lift as a multiple of weight; one is level flight
     */
    public static double accelerationMs2(double thrustNewtons, double massKg, double airspeedMs,
                                         double terminalSpeedMs, double referenceThrustNewtons,
                                         double wingSpanM, double throttleDemand, double loadFactor)
    {
        if (!finitePositive(massKg) || !finitePositive(terminalSpeedMs) || !finitePositive(referenceThrustNewtons))
            return 0D;
        double speed = Double.isFinite(airspeedMs) ? Math.max(0D, Math.abs(airspeedMs)) : 0D;
        double thrust = Double.isFinite(thrustNewtons) ? Math.max(0D, thrustNewtons) : 0D;
        double drag = dragNewtons(speed, massKg, wingSpanM, terminalSpeedMs, referenceThrustNewtons, loadFactor);
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
     * The load factor the current attitude is asking the wing for, as a multiple
     * of weight.
     *
     * <p>The derived model carries no angle-of-attack state, but the angle
     * between the nose and the flight path is exactly that, and both vectors are
     * already to hand. When the pilot pulls, the nose leads the velocity vector,
     * the wing flies at positive incidence and makes more than trim lift; when
     * the pilot pushes over, the nose falls below the path and it makes less.
     * With the nose on the path — hands off, in steady flight — the wing is
     * asked for the weight of the aircraft and nothing more, which is what stops
     * an untouched aircraft from ballooning upward on its own.
     *
     * @param forwardY          vertical component of the unit nose vector
     * @param flightPathY       vertical component of the unit velocity vector
     * @param maxExcessFraction how far above one the wing may be asked to go
     * @return the commanded load factor, never negative
     */
    public static double commandedLoadFactor(double forwardY, double flightPathY, double maxExcessFraction)
    {
        if (!Double.isFinite(forwardY) || !Double.isFinite(flightPathY))
            return 1D;
        double excess = Double.isFinite(maxExcessFraction) ? Math.max(0D, maxExcessFraction) : 0D;
        double demand = VehiclePhysicsConstants.INCIDENCE_LOAD_FACTOR_GAIN * (forwardY - flightPathY);
        if (!Double.isFinite(demand))
            return 1D;
        // Unloading is bounded at zero g; the wing cannot push the aircraft down.
        return 1D + Math.max(-Math.min(excess, 1D), Math.min(excess, demand));
    }

    /**
     * The share of a vertical lift force that acts along the flight path, which
     * a real wing never produces and which the caller must therefore remove.
     *
     * <p>Lift acts perpendicular to the relative wind, so it does no work: it
     * turns the flight path without changing speed. Applying it on the world
     * vertical axis instead — as the tick integration must, since gravity lives
     * there too — leaks a component along the velocity vector, and that
     * component is free energy. In a climb it is positive and the aircraft
     * accelerates for nothing; in a dive it is negative and the aircraft is
     * robbed of speed it should have kept. Subtracting it along the velocity
     * vector leaves gravity as the only force that may change speed, which is
     * what makes a zoom climb cost exactly the height it buys.
     *
     * @param verticalLift the vertical lift applied this tick, in the caller's units
     * @param verticalSpeed the vertical component of velocity, in the same units
     * @param speed         the magnitude of velocity, in the same units
     * @return the speed change to remove along the velocity vector, bounded so
     *         that it can never reverse the aircraft
     */
    public static double liftWorkAlongPath(double verticalLift, double verticalSpeed, double speed)
    {
        if (!finitePositive(speed) || !Double.isFinite(verticalLift) || !Double.isFinite(verticalSpeed))
            return 0D;
        double component = verticalLift * (verticalSpeed / speed);
        if (!Double.isFinite(component))
            return 0D;
        return Math.max(-speed, Math.min(speed, component));
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
     * <p>Authority builds quickly from zero at a standstill, reaches full strength
     * at a tenth of terminal speed, remains there through the low-speed range,
     * and tapers at high speed as control surfaces load up. Unlike the legacy
     * curve it never reaches zero at speed, which made fast aircraft uncontrollable.
     */
    public static float normalizedControlAuthority(double airspeedBlocksPerTick, double terminalBlocksPerTick)
    {
        if (!finitePositive(terminalBlocksPerTick) || !Double.isFinite(airspeedBlocksPerTick))
            return 0F;
        double ratio = Math.max(0D, airspeedBlocksPerTick) / terminalBlocksPerTick;
        double authority;
        if (ratio <= VehiclePhysicsConstants.FULL_CONTROL_AUTHORITY_SPEED_FRACTION)
            authority = ratio / VehiclePhysicsConstants.FULL_CONTROL_AUTHORITY_SPEED_FRACTION;
        else if (ratio <= 0.35D)
            authority = 1D;
        else
            authority = 1D - 0.55D * Math.min(1D, (ratio - 0.35D) / 0.65D);
        return (float) Math.max(0D, Math.min(1D, authority));
    }

    /**
     * Reference area of a deployed air brake in square metres.
     *
     * <p>{@code RealAirBrakeAreaM2} is the authored figure: the total frontal
     * area a type's speed brake panels present when fully out, which is a
     * researchable dimension for a real aircraft. Where it is absent the brake
     * is sized as a fixed fraction of wing area, so every aircraft that has not
     * been researched yet still decelerates in proportion to how big it is.
     *
     * @param authoredAreaM2 authored brake area, or a non-positive value if unavailable
     * @param wingAreaM2     wing reference area, used for the fallback
     * @return the brake area, or zero when neither figure is usable
     */
    public static double airBrakeAreaM2(double authoredAreaM2, double wingAreaM2)
    {
        if (finitePositive(authoredAreaM2))
            return authoredAreaM2;
        if (finitePositive(wingAreaM2))
            return wingAreaM2 * VehiclePhysicsConstants.AIR_BRAKE_WING_AREA_FRACTION;
        return 0D;
    }

    /**
     * Drag in newtons contributed by a deployed air brake, from the flat-plate
     * form {@code D = 0.5 * rho * S * Cd * v²}, times
     * {@link VehiclePhysicsConstants#AIR_BRAKE_EFFECTIVENESS}. It is a genuine
     * extra force rather than a multiplier, so it is heaviest exactly where a
     * speed brake matters — at high airspeed — and vanishes at a standstill.
     */
    public static double airBrakeDragNewtons(double airspeedMs, double airBrakeAreaM2)
    {
        if (!finitePositive(airBrakeAreaM2))
            return 0D;
        double speed = Double.isFinite(airspeedMs) ? Math.abs(airspeedMs) : 0D;
        return 0.5D * VehiclePhysicsUnits.AIR_DENSITY * airBrakeAreaM2
            * VehiclePhysicsConstants.AIR_BRAKE_DRAG_COEFFICIENT * speed * speed
            * VehiclePhysicsConstants.AIR_BRAKE_EFFECTIVENESS;
    }

    /**
     * Deceleration in m/s² from a deployed air brake, capped by its own ceiling
     * so the quadratic term stays decisive without becoming a wall at the top of
     * the speed range. Always zero or positive; the caller subtracts it.
     */
    public static double airBrakeDecelerationMs2(double airspeedMs, double airBrakeAreaM2, double massKg)
    {
        if (!finitePositive(massKg))
            return 0D;
        double deceleration = airBrakeDragNewtons(airspeedMs, airBrakeAreaM2) / massKg;
        if (!Double.isFinite(deceleration) || deceleration <= 0D)
            return 0D;
        return Math.min(deceleration, VehiclePhysicsConstants.AIR_BRAKE_MAX_DECELERATION_MS2);
    }

    /**
     * Per-tick velocity multiplier standing in for a deployed air brake on the
     * legacy flight model, which carries drag as a multiplier rather than a force.
     *
     * <p>Both areas are used only as a ratio, so a pack that states neither, or
     * only one of them, falls back to the same default proportion the derived
     * model uses rather than mixing a real dimension with a gameplay number.
     */
    public static float legacyAirBrakeDragFactor(double airBrakeAreaM2, double wingAreaM2)
    {
        double fraction = finitePositive(airBrakeAreaM2) && finitePositive(wingAreaM2)
            ? airBrakeAreaM2 / wingAreaM2
            : VehiclePhysicsConstants.AIR_BRAKE_WING_AREA_FRACTION;
        double loss = Math.min(VehiclePhysicsConstants.MAX_LEGACY_AIR_BRAKE_DRAG,
            fraction * VehiclePhysicsConstants.LEGACY_AIR_BRAKE_DRAG_SCALE
                * VehiclePhysicsConstants.AIR_BRAKE_EFFECTIVENESS);
        return (float) (1D - Math.max(0D, loss));
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
