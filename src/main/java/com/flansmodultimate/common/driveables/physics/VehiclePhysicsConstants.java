package com.flansmodultimate.common.driveables.physics;

/**
 * Internal tuning constants for the real-world physics path.
 *
 * <p>These are deliberately not exposed in the TOML config. The user facing
 * primary speed knobs are {@code realisticPlaneSpeedScale} and
 * {@code realisticGroundVehicleSpeedScale}; the separate aircraft
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
    /**
     * Longitudinal speed in m/s against the demanded direction above which a
     * ground vehicle's gearbox cannot select that direction. Walking pace: the
     * vehicle has to have all but stopped before reverse, or drive, engages.
     */
    public static final double DIRECTION_CHANGE_SPEED_MS = 0.5D;
    /** Ticks spent in neutral near standstill before the opposite gear is selected. */
    public static final int DIRECTION_CHANGE_TICKS = 8;
    /** Ticks for a newly selected gear's clutch to take up full tractive force. */
    public static final int CLUTCH_ENGAGE_TICKS = 20;

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

    /** Typical ground-vehicle power-to-weight ratio used to normalise climbing capability. */
    public static final float SLOPE_REFERENCE_POWER_TO_WEIGHT_KW_PER_T = 30F;
    /** Bounds prevent unusually weak or powerful content from producing absurd response angles. */
    public static final float SLOPE_MIN_CAPABILITY = 0.5F;
    public static final float SLOPE_MAX_CAPABILITY = 2F;
    /** Full propulsion angle is this base plus the normalised capability times the following span. */
    public static final float SLOPE_BASE_FALLOFF_DEG = 16F;
    public static final float SLOPE_CAPABILITY_FALLOFF_DEG = 8F;
    /** Degrees over which propulsion transitions from full power to the crawl floor. */
    public static final float SLOPE_FALLOFF_SPAN_DEG = 20F;
    /** Minimum uphill propulsion retained for block edges and very steep Minecraft terrain. */
    public static final float SLOPE_CRAWL_PROPULSION = 0.2F;

    // ---------------------------------------------------------------- aircraft

    /**
     * Gravity for the derived fixed-wing model, in blocks per tick squared.
     *
     * <p>Minecraft pulls at roughly four times standard gravity. That is
     * invisible while lift is free, but the moment a climb is made to cost the
     * energy it should, an airframe carrying its real mass and its real thrust
     * is flying at a quarter of its true thrust-to-weight ratio and can barely
     * climb at all. Since every other figure in this model is a real-world one,
     * the weight it is balanced against is real too, and the authored climb rate
     * then falls out of thrust and drag instead of needing a fudge.
     *
     * <p>The legacy flight model, helicopters and every other driveable keep
     * Minecraft's own pull; only the derived fixed-wing path uses this.
     */
    public static final double DERIVED_FLIGHT_GRAVITY_BLOCKS_PER_TICK2 = VehiclePhysicsUnits.STANDARD_GRAVITY
        / (VehiclePhysicsUnits.TICKS_PER_SECOND * VehiclePhysicsUnits.TICKS_PER_SECOND);
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
    /**
     * Oswald span efficiency used in the induced-drag term. Typical for a clean
     * monoplane wing; lower values mean more drag for the same lift.
     */
    public static final double OSWALD_EFFICIENCY = 0.75D;
    /**
     * Ceiling on the share of the thrust budget at top speed that the induced
     * term may claim, so a heavy short-span airframe still keeps a parasitic
     * term and therefore still has a well-behaved top speed.
     */
    public static final double MAX_INDUCED_DRAG_SHARE = 0.5D;
    /**
     * Speed, as a fraction of terminal speed, below which the {@code 1 / v²}
     * induced term is held flat instead of diverging toward standstill.
     */
    public static final double INDUCED_DRAG_KNEE_FRACTION = 0.12D;
    /**
     * Load factor gained per unit of vertical incidence, where incidence is the
     * difference between the vertical component of the nose vector and that of
     * the flight path.
     *
     * <p>The derived model carries no angle-of-attack state, but the angle
     * between the nose and the velocity vector is exactly that, and it is
     * already available. This gain turns it into the load factor the wing is
     * being asked for, so that pulling the nose above the flight path loads the
     * wing and pushing it below unloads it, while an aircraft flying straight is
     * asked for its own weight and nothing more.
     */
    public static final double INCIDENCE_LOAD_FACTOR_GAIN = 4D;
    /**
     * Ceiling on the load factor used to scale induced drag, so a momentary
     * attitude spike cannot produce an unbounded drag figure.
     */
    public static final double MAX_MANEUVER_LOAD_FACTOR = 3D;
    /**
     * Throttle-lever position at or below which the aircraft counts as coasting
     * and the deceleration floor applies.
     */
    public static final double IDLE_THROTTLE_FRACTION = 0.02D;
    /**
     * Deceleration floor in m/s² while coasting, standing in for the drag of an
     * idled or windmilling propeller disc, which the authored data cannot express.
     */
    public static final double MIN_AIRCRAFT_COAST_DECELERATION_MS2 = 1.2D;
    /**
     * Speed, as a fraction of terminal speed, over which the coasting floor ramps
     * in from zero, so a stationary or taxiing aircraft is unaffected by it.
     */
    public static final double COAST_DECELERATION_RAMP_FRACTION = 0.1D;
    /**
     * Drag coefficient of a deployed air brake, referenced to its own frontal
     * area. A speed brake panel stood square into the airflow is very close to a
     * flat plate, which is where the 1.2 comes from.
     */
    public static final double AIR_BRAKE_DRAG_COEFFICIENT = 1.2D;
    /**
     * Air brake area as a fraction of wing area, used when a pack does not state
     * {@code RealAirBrakeAreaM2}. Real speed brake installations sit in the low
     * single-digit percent of wing area, which yields roughly a third of a g of
     * deceleration at combat speeds.
     */
    public static final double AIR_BRAKE_WING_AREA_FRACTION = 0.025D;
    /**
     * Gameplay multiplier on air brake drag, kept separate from the physical
     * coefficient and area so that {@code RealAirBrakeAreaM2} stays an honest
     * researched dimension and a researched plane never brakes worse than an
     * unresearched one.
     *
     * <p>Drag rises with the square of speed, so a physically exact brake that
     * feels right at top speed all but disappears at manoeuvring speed, where it
     * barely exceeds the idle-coast floor the model already applies. Doubling puts
     * the effective wing-referenced increment at {@code Cd * fraction * 2 = 0.06},
     * against the 0.05 that experiment and CFD measure at 60 degrees of deflection
     * — the top of the real envelope rather than past it.
     */
    public static final double AIR_BRAKE_EFFECTIVENESS = 2D;
    /**
     * How much of the air-brake-to-wing area ratio becomes per-tick velocity loss
     * on the legacy flight model, which has no force budget to add a drag term to.
     * The default ratio lands near a third of the legacy airframe drag, which is
     * the same order the derived model produces for the same aircraft.
     */
    public static final double LEGACY_AIR_BRAKE_DRAG_SCALE = 0.6D;
    /** Ceiling on the legacy per-tick air brake velocity loss, so it cannot stop a plane dead. */
    public static final double MAX_LEGACY_AIR_BRAKE_DRAG = 0.06D;
    /**
     * Per-axis multipliers on the derived control authority, relative to the
     * legacy sensitivity the fixed-wing model inherited.
     *
     * <p>The legacy value was a single constant for all three axes, which left
     * a fighter rolling at roughly 20 degrees per second — an airliner rate. A
     * real aircraft rolls fastest, pitches more slowly and yaws slowest of all,
     * because the rudder is the smallest surface, so the axes are separated
     * here. Roll at 2.0 puts a wartime fighter near its real 90 degrees per
     * second at full deflection.
     */
    public static final float DERIVED_ROLL_AUTHORITY_SCALE = 2F;
    public static final float DERIVED_PITCH_AUTHORITY_SCALE = 1.4F;
    public static final float DERIVED_YAW_AUTHORITY_SCALE = 1.2F;

    /**
     * Fraction of terminal speed at which the control axes reach full authority.
     * The short ramp preserves an inert standstill while making ordinary
     * reduced-throttle flight fully controllable for arcade handling.
     */
    public static final double FULL_CONTROL_AUTHORITY_SPEED_FRACTION = 0.1D;

    /**
     * Manoeuvre-drag weights applied to actual body-axis angular rates. Yaw is
     * most expensive because it presents the fuselage to the airflow, pitch
     * follows, and roll is cheapest while still carrying a measurable induced
     * drag cost. Units are inverse radians.
     */
    public static final double AIRCRAFT_YAW_MANEUVER_DRAG = 0.045D;
    public static final double AIRCRAFT_PITCH_MANEUVER_DRAG = 0.03D;
    public static final double AIRCRAFT_ROLL_MANEUVER_DRAG = 0.012D;

    /** Share of lateral cornering acceleration paid as longitudinal tyre / track scrub. */
    public static final double GROUND_TURNING_RESISTANCE = 0.25D;

    /**
     * Airspeed, as a fraction of the reference speed, below which the nose
     * starts dropping on its own. At the reference speed the wing is carrying
     * the aircraft, so nothing happens until it is meaningfully slower.
     */
    public static final double STALL_RECOVERY_ONSET_FRACTION = 0.9D;
    /**
     * Maximum self-correcting nose-down rate in degrees per tick, reached only
     * when the aircraft is both very slow and steeply nose-up. Roughly 30
     * degrees per second, which recovers an incipient stall without wresting
     * the aircraft away from the pilot.
     */
    public static final float STALL_RECOVERY_MAX_DEG_PER_TICK = 1.5F;
    /**
     * Nose-up attitude in degrees at which the self-correction is at full
     * strength. Below this it is scaled down, so level flight is untouched.
     */
    public static final float STALL_RECOVERY_FULL_PITCH_DEG = 30F;

    /**
     * Rolling resistance of the undercarriage in m/s², applied whenever the
     * aircraft is on its wheels. Tyres, bearings and grass are not expressible
     * in the authored data, and without them a landed aircraft only has the
     * aerodynamic drag of the airframe to stop it.
     */
    public static final double GROUND_ROLLING_DECELERATION_MS2 = 1.2D;
    /**
     * Wheel braking in m/s² added on top of rolling resistance once the
     * throttle is closed, which is what a pilot does on the landing roll.
     */
    public static final double GROUND_BRAKING_DECELERATION_MS2 = 4D;
    /**
     * Ground speed in m/s below which a closed-throttle aircraft is parked
     * outright. Without a floor the exponential tail of any deceleration leaves
     * a permanent crawl, which is what made a freshly placed aircraft drift.
     */
    public static final double GROUND_PARKING_SPEED_MS = 0.35D;
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

    // --------------------------------------------------------------- knockback

    /**
     * Lowest legacy {@code Mass}, after conversion to kilograms, accepted as a
     * real weight. Aircraft packs that meant kilograms author hundreds or
     * thousands; their copied placeholders such as 2.7 or 16.7 fall below it.
     */
    public static final float MIN_PLAUSIBLE_LEGACY_MASS_KG = 100F;
    /** Floor on any resolved impulse mass, so no value can make a driveable lighter than a person. */
    public static final float MIN_IMPULSE_MASS_KG = 50F;
    /**
     * Outside velocity changes that are still below this after being weighed
     * against mass, in blocks per tick (0.1 m/s), are discarded, so a heavy hull
     * does not jitter under every small push.
     */
    public static final double EXTERNAL_IMPULSE_DEAD_ZONE_BLOCKS_PER_TICK = 0.005D;
    /** Coefficient of restitution between two driveables. Hulls crumple far more than they bounce. */
    public static final double DRIVEABLE_COLLISION_RESTITUTION = 0.1D;
    /** Horizontal speed in blocks per tick below which contact with another driveable is not resolved. */
    public static final double MIN_DRIVEABLE_CONTACT_SPEED_BLOCKS_PER_TICK = 1.0E-3D;
    /**
     * Minimum deceleration in m/s² of a grounded vehicle nobody is driving, whose
     * engine is off or whose brake is held: locked wheels or tracks sliding on
     * the ground, roughly a friction coefficient of 0.6.
     */
    public static final double PARKED_GROUND_FRICTION_DECELERATION_MS2 = 6D;
    /** Minimum coasting deceleration in m/s² of a driven tracked vehicle with no drive demand. */
    public static final double TRACKED_IDLE_DECELERATION_MS2 = 4D;
    /** Minimum coasting deceleration in m/s² of a driven wheeled vehicle with no drive demand. */
    public static final double WHEELED_IDLE_DECELERATION_MS2 = MIN_DERIVED_DECELERATION_MS2;
    /** Horizontal speed in blocks per tick below which a grounded vehicle without drive demand comes to rest. */
    public static final double GROUND_REST_SPEED_BLOCKS_PER_TICK = 0.002D;

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
