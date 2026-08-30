package com.flansmodultimate.common.driveables.physics;

/**
 * Internal tuning constants for the real-world physics path.
 *
 * <p>These are deliberately not exposed in the TOML config. The only user facing
 * primary speed knob is {@code realisticVehicleSpeedScale}; the separate aircraft
 * reference-speed scale controls runway and low-speed behaviour. Everything here
 * is a shape constant that only makes sense to change alongside the formulas that
 * use it. Keeping them in one class stops magic numbers reappearing inside the
 * entity tick methods.
 */
public final class VehiclePhysicsConstants
{
    private VehiclePhysicsConstants() {}

    // ---------------------------------------------------------------- throttle

    /**
     * Ticks for the driver's throttle demand to travel from idle to full under
     * the real-world profile. Vehicle character comes from the power model, not
     * from a per-pack acceleration number, so this is a single global value.
     */
    public static final float REAL_THROTTLE_RAMP_TICKS = 20F;
    /** Per-tick throttle step derived from {@link #REAL_THROTTLE_RAMP_TICKS}. */
    public static final float REAL_THROTTLE_RAMP_PER_TICK = 1F / REAL_THROTTLE_RAMP_TICKS;

    // -------------------------------------------------------------- propulsion

    /**
     * Speed, as a fraction of terminal speed, below which tractive force is
     * treated as launch limited instead of power limited. Constant-power force
     * is {@code P / v}, which diverges at standstill; this is the knee.
     */
    public static final double LAUNCH_SPEED_FRACTION = 0.05D;
    /** Absolute floor on the launch knee in m/s, so very slow vehicles still move. */
    public static final double MIN_LAUNCH_SPEED_MS = 1D;
    /**
     * Hard ceiling on derived longitudinal acceleration in m/s². Roughly 1.2 g;
     * enough for a sports car, low enough that no derived value can teleport a
     * vehicle within a single 20 Hz step.
     */
    public static final double MAX_DERIVED_ACCELERATION_MS2 = 12D;
    /**
     * Deceleration floor in m/s² applied when the driver lifts off or reverses
     * demand, so coasting never stalls on a vanishingly small drag term.
     */
    public static final double MIN_DERIVED_DECELERATION_MS2 = 1.5D;
    /** Extra deceleration in m/s² available while the brake input is held. */
    public static final double BRAKING_DECELERATION_MS2 = 6D;

    // ------------------------------------------------------------------- slope

    /**
     * Fraction of the configured maximum slope below which propulsion is
     * unaffected. Between this and the limit, propulsion ramps down linearly so
     * the vehicle bogs down instead of stopping dead.
     */
    public static final float SLOPE_FALLOFF_START = 0.7F;
    /** Residual propulsion retained exactly at the slope limit, to avoid a hard zero. */
    public static final float SLOPE_LIMIT_RESIDUAL = 0.05F;

    // ---------------------------------------------------------------- aircraft

    /**
     * Propeller efficiency used when converting shaft power to thrust. A single
     * representative value; modelling a real propeller map is out of scope.
     */
    public static final double PROPELLER_EFFICIENCY = 0.8D;
    /**
     * Maximum lift coefficient used to derive the reference (stall-like) speed
     * from wing loading. Typical for a clean wing at high angle of attack.
     */
    public static final double MAX_LIFT_COEFFICIENT = 1.4D;
    /**
     * The derived reference speed is clamped to at most this fraction of the
     * aircraft's own terminal speed. Real stall speeds of heavily loaded jets are
     * unpleasant at Minecraft scale, so this keeps every aircraft flyable.
     */
    public static final double MAX_REFERENCE_SPEED_FRACTION = 0.45D;
    /** Reference wing span in metres for the roll inertia heuristic. */
    public static final double REFERENCE_WING_SPAN_M = 11D;
    /** Reference mass in kilograms for the roll inertia heuristic. */
    public static final double REFERENCE_AIRCRAFT_MASS_KG = 3000D;
    /** Roll inertia factor is clamped into this band so control never fully disappears. */
    public static final float MIN_ROLL_INERTIA_FACTOR = 0.35F;
    public static final float MAX_ROLL_INERTIA_FACTOR = 2.5F;

    // ------------------------------------------------------------------ marine

    /**
     * Restoring stiffness for draft-based flotation, in blocks per tick of
     * vertical velocity per block of depth error. Deliberately gentle: the hull
     * settles onto its draft over roughly a second instead of snapping.
     */
    public static final double DRAFT_RESTORING_STIFFNESS = 0.08D;
    /** Maximum blocks a draft probe will search upward for the water surface. */
    public static final int DRAFT_SURFACE_PROBE_BLOCKS = 8;

    // ------------------------------------------------------------------ safety

    /**
     * Default per-axis movement safety clamp in blocks per tick, used when a
     * driveable has no real-world profile. This is the historical value and is
     * preserved so legacy vehicles keep their exact behaviour.
     */
    public static final double LEGACY_MOVEMENT_CLAMP_BLOCKS_PER_TICK = 8D;
    /**
     * Per-axis movement safety clamp for driveables running the real-world
     * profile, in blocks per tick. Roughly 2300 km/h, which clears every piston
     * aircraft and every jet the mod is likely to carry, while still bounding the
     * region vanilla collision has to sweep in a single step.
     *
     * <p>This is a safety bound against NaN and runaway integration, not a top
     * speed. Top speed comes from {@code RealMaxSpeedKmh}.
     */
    public static final double REAL_WORLD_MOVEMENT_CLAMP_BLOCKS_PER_TICK = 32D;
    /** Historical horizontal look-ahead used by wheel contact sampling, in blocks. */
    public static final double LEGACY_WHEEL_PREDICTION_BLOCKS = 1.5D;
}
