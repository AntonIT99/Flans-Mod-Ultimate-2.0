package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

/**
 * The values the new physics path actually uses, resolved once during type
 * finalization.
 *
 * <p>Two rules shape this record. Static derivations that never change, such as
 * power to weight or wing loading, are computed here so no tick method repeats
 * them. Speed conversions are not baked in, because the global
 * {@code realisticVehicleSpeedScale} can change at runtime and the authored
 * real-world values must never be mutated; instead every speed accessor takes the
 * current scale and converts through {@link VehiclePhysicsUnits}. Runtime physics
 * and item tooltips call the same accessors, so they can never disagree.
 *
 * <p>An instance always exists for every driveable type. {@link #mode()} says
 * which path is live, and the {@code has*} predicates are what the tick methods
 * branch on, so nullable fields never leak into per-tick code.
 */
public record ResolvedVehiclePhysics(
    EnumVehiclePhysicsMode mode,
    EnumVehicleCategory category,
    RealWorldVehicleSpec source,
    VehicleGeometry geometry,

    boolean groundProfileComplete,
    boolean aircraftProfileComplete,

    /** Baseline engine power in kilowatts before the installed engine part modifier. */
    float baselinePowerKw,
    /** Baseline jet thrust in kilonewtons before the installed engine part modifier. */
    float baselineThrustKn,
    float massKg,
    float maxSpeedKmh,

    float powerToWeightKwPerKg,
    float thrustToWeight,
    float wingLoadingKgPerM2,
    float rollInertiaFactor,

    EnumDriveType driveType,
    boolean driveTypeExplicit,
    /** Authored reverse cap in km/h, or null to keep legacy reverse behaviour. */
    @Nullable Float maxReverseSpeedKmh,
    /** Authored slope limit in degrees, or null for no slope limiting. */
    @Nullable Float maxSlopeDeg,
    /** Authored draft in metres, or null to keep the legacy constant buoyancy. */
    @Nullable Float draftM)
{
    /** A type with no usable real-world data at all. */
    public static ResolvedVehiclePhysics legacy(EnumVehicleCategory category, EnumDriveType inferredDriveType)
    {
        return legacy(category, inferredDriveType, VehicleGeometry.EMPTY);
    }

    /**
     * A type with no usable real-world data, but with dimensions derived from the
     * geometry it already declares. Those are always available, because they cost
     * nothing and both the tooltip and the debug command show them regardless of
     * which physics path is live.
     */
    public static ResolvedVehiclePhysics legacy(EnumVehicleCategory category, EnumDriveType inferredDriveType,
                                                VehicleGeometry geometry)
    {
        return new ResolvedVehiclePhysics(EnumVehiclePhysicsMode.LEGACY, category,
            RealWorldVehicleSpec.EMPTY, geometry == null ? VehicleGeometry.EMPTY : geometry,
            false, false, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F,
            inferredDriveType, false, null, null, null);
    }

    // ------------------------------------------------------------- predicates

    /** Whether the new ground propulsion model owns acceleration and top speed. */
    public boolean hasGroundPropulsion()
    {
        return groundProfileComplete && category == EnumVehicleCategory.GROUND;
    }

    /** Whether the new fixed-wing model owns thrust, lift and control authority. */
    public boolean hasAircraftProfile()
    {
        return aircraftProfileComplete && category == EnumVehicleCategory.AIRCRAFT;
    }

    public boolean hasReverseSpeedOverride()
    {
        return maxReverseSpeedKmh != null;
    }

    public boolean hasSlopeLimit()
    {
        return maxSlopeDeg != null;
    }

    public boolean hasDraft()
    {
        return draftM != null;
    }

    /** Whether anything at all differs from pure legacy behaviour. */
    public boolean isActive()
    {
        return mode != EnumVehiclePhysicsMode.LEGACY;
    }

    // ------------------------------------------------------- speed accessors

    /**
     * Authoritative forward terminal speed in blocks per tick for the given
     * global speed scale. Zero when no complete profile is active.
     */
    public double maxSpeedBlocksPerTick(double speedScale)
    {
        return VehiclePhysicsUnits.kmhToBlocksPerTick(maxSpeedKmh, speedScale);
    }

    /**
     * Reverse terminal speed in blocks per tick, or zero when the pack authored
     * no reverse override. The legacy {@code MaxNegativeThrottle == 0} gate is
     * applied by the resolver, not here: a vehicle that cannot reverse never gets
     * a reverse override in the first place.
     */
    public double reverseSpeedBlocksPerTick(double speedScale)
    {
        return maxReverseSpeedKmh == null ? 0D
            : VehiclePhysicsUnits.kmhToBlocksPerTick(maxReverseSpeedKmh, speedScale);
    }

    /** Authored climb rate as blocks per tick, or zero when unspecified. */
    public double climbRateBlocksPerTick(double speedScale)
    {
        Float climbRate = source.aircraft().climbRateMs();
        return climbRate == null ? 0D
            : VehiclePhysicsUnits.metresPerSecondToBlocksPerTick(climbRate, speedScale);
    }

    /**
     * Per-axis movement safety clamp appropriate to this type. Legacy types keep
     * the historical value exactly; only types on the real-world path get the
     * raised bound, so no existing vehicle changes behaviour.
     */
    public double movementClampBlocksPerTick()
    {
        return hasGroundPropulsion() || hasAircraftProfile()
            ? VehiclePhysicsConstants.REAL_WORLD_MOVEMENT_CLAMP_BLOCKS_PER_TICK
            : VehiclePhysicsConstants.LEGACY_MOVEMENT_CLAMP_BLOCKS_PER_TICK;
    }

    /**
     * Horizontal look-ahead used when sampling terrain under the wheels. The
     * historical 1.5 blocks is correct up to about 108 km/h; a faster vehicle
     * needs the probe to reach as far as it actually travels in a tick.
     */
    public double wheelPredictionBlocks(double speedScale)
    {
        double legacy = VehiclePhysicsConstants.LEGACY_WHEEL_PREDICTION_BLOCKS;
        if (!hasGroundPropulsion() && !hasAircraftProfile())
            return legacy;
        return Math.max(legacy, maxSpeedBlocksPerTick(speedScale));
    }

    // ---------------------------------------------------- engine interaction

    /**
     * Usable engine power in watts after the installed engine part's gameplay
     * modifier.
     *
     * <p>{@code RealEnginePowerKw} is the vehicle's reference powerplant.
     * {@code PartType.engineSpeed} stays what it has always been, a dimensionless
     * gameplay multiplier that defaults to 1.0, so engine upgrades keep working
     * and no arbitrary gameplay number is reinterpreted as kilowatts.
     * {@code PartType.enginePower} is deliberately not consulted here.
     */
    public double effectivePowerWatts(float engineModifier)
    {
        return baselinePowerKw * VehiclePhysicsUnits.WATTS_PER_KILOWATT * sanitizeModifier(engineModifier);
    }

    /** Usable jet thrust in kilonewtons after the installed engine part's modifier. */
    public double effectiveThrustKn(float engineModifier)
    {
        return baselineThrustKn * sanitizeModifier(engineModifier);
    }

    private static double sanitizeModifier(float engineModifier)
    {
        return Float.isFinite(engineModifier) && engineModifier > 0F ? engineModifier : 1F;
    }

    // ------------------------------------------------------ derived aircraft

    /**
     * Playability-clamped reference airspeed in m/s at which the wing carries the
     * aircraft. Zero when no aircraft profile is active.
     */
    public double referenceSpeedMs(double speedScale)
    {
        if (!hasAircraftProfile())
            return 0D;
        double terminalMs = VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(maxSpeedBlocksPerTick(speedScale));
        Float wingArea = source.aircraft().wingAreaM2();
        return AircraftPerformancePhysics.clampedReferenceSpeedMs(massKg,
            wingArea == null ? 0F : wingArea, terminalMs);
    }
}
