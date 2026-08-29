package com.flansmodultimate.common.driveables.physics;

import org.jetbrains.annotations.Nullable;

/**
 * Turns authored real-world data plus existing geometry and legacy fields into
 * the {@link ResolvedVehiclePhysics} the runtime uses.
 *
 * <p>This class owns the activation rules, and they are deliberately strict in
 * one direction: a coupled profile activates only when every member of its
 * required set is present and usable. A definition that declares mass but no
 * power stays on legacy propulsion entirely rather than running half of each
 * model, because mixing a real-world top speed with a legacy acceleration term
 * would double-apply or contradict itself.
 *
 * <p>Independently usable parameters are handled separately and each takes effect
 * on its own, which is why a vehicle can be on legacy propulsion while still
 * honouring an authored slope limit.
 *
 * <p>Fallback order, applied per value rather than globally:
 * <pre>
 * explicit real-world parameter  >  value derived from existing geometry
 *                                >  legacy parameter  >  legacy default
 * </pre>
 */
public final class VehiclePhysicsResolver
{
    private VehiclePhysicsResolver() {}

    public static ResolvedVehiclePhysics resolve(EnumVehicleCategory category,
                                                 @Nullable RealWorldVehicleSpec spec,
                                                 @Nullable VehicleGeometry geometry,
                                                 @Nullable LegacyPhysicsHints legacy)
    {
        EnumVehicleCategory resolvedCategory = category == null ? EnumVehicleCategory.OTHER : category;
        RealWorldVehicleSpec source = spec == null ? RealWorldVehicleSpec.EMPTY : spec;
        VehicleGeometry dimensions = geometry == null ? VehicleGeometry.EMPTY : geometry;
        LegacyPhysicsHints hints = legacy == null ? LegacyPhysicsHints.EMPTY : legacy;

        EnumDriveType inferredDriveType = EnumDriveType.infer(hints.tank(), hints.fourWheelDrive());
        if (source.isEmpty())
            return ResolvedVehiclePhysics.legacy(resolvedCategory, inferredDriveType, dimensions);

        // Coupled profiles. Category gates them so a plane carrying ground keys
        // cannot accidentally satisfy the wrong profile.
        boolean groundComplete = resolvedCategory == EnumVehicleCategory.GROUND
            && source.hasCompleteGroundProfile();
        boolean aircraftComplete = resolvedCategory == EnumVehicleCategory.AIRCRAFT
            && source.hasCompleteAircraftProfile();

        // Independently usable parameters.
        EnumDriveType driveType = source.ground().driveType();
        boolean driveTypeExplicit = driveType != null;
        if (!driveTypeExplicit)
            driveType = inferredDriveType;

        Float reverseSpeedKmh = resolveReverseSpeed(source, hints);
        Float slopeDeg = resolveSlope(source, resolvedCategory);
        Float draftM = resolveDraft(source, hints);

        boolean anyOverride = driveTypeExplicit || reverseSpeedKmh != null || slopeDeg != null || draftM != null;
        boolean profileActive = groundComplete || aircraftComplete;

        EnumVehiclePhysicsMode mode;
        if (profileActive)
            mode = EnumVehiclePhysicsMode.REAL_WORLD_PROFILE;
        else if (anyOverride)
            mode = EnumVehiclePhysicsMode.LEGACY_WITH_OVERRIDES;
        else
            mode = EnumVehiclePhysicsMode.LEGACY;

        // Static derivations. These are only meaningful when the matching profile
        // is complete, so they stay at zero otherwise rather than advertising a
        // half-derived number in tooltips.
        float massKg = profileActive ? orZero(source.massKg()) : 0F;
        float maxSpeedKmh = profileActive ? orZero(source.maxSpeedKmh()) : 0F;
        float powerKw = profileActive ? orZero(source.enginePowerKw()) : 0F;
        float thrustKn = aircraftComplete ? orZero(source.engineThrustKn()) : 0F;

        float powerToWeight = VehiclePhysicsUnits.powerToWeight(powerKw, massKg);
        float thrustToWeight = VehiclePhysicsUnits.thrustToWeight(thrustKn, massKg);
        float wingLoading = 0F;
        float rollInertia = 1F;
        if (aircraftComplete)
        {
            float wingArea = orZero(source.aircraft().wingAreaM2());
            float wingSpan = orZero(source.aircraft().wingSpanM());
            wingLoading = VehiclePhysicsUnits.wingLoading(massKg, wingArea);
            rollInertia = AircraftPerformancePhysics.rollInertiaFactor(wingSpan, massKg);
        }

        return new ResolvedVehiclePhysics(mode, resolvedCategory, source, dimensions,
            groundComplete, aircraftComplete,
            powerKw, thrustKn, massKg, maxSpeedKmh,
            powerToWeight, thrustToWeight, wingLoading, rollInertia,
            driveType, driveTypeExplicit, reverseSpeedKmh, slopeDeg, draftM);
    }

    /**
     * The reverse override never enables reverse on its own. A legacy definition
     * with {@code MaxNegativeThrottle 0} has deliberately disabled reverse, and
     * that gate is preserved: the override can only cap a reverse the vehicle
     * already had.
     */
    @Nullable
    private static Float resolveReverseSpeed(RealWorldVehicleSpec source, LegacyPhysicsHints hints)
    {
        Float authored = source.ground().maxReverseSpeedKmh();
        if (!VehiclePhysicsUnits.isUsablePositive(authored) || !hints.allowsReverse())
            return null;
        return authored;
    }

    /** Slope limiting only applies to vehicles that drive on terrain. */
    @Nullable
    private static Float resolveSlope(RealWorldVehicleSpec source, EnumVehicleCategory category)
    {
        if (category != EnumVehicleCategory.GROUND)
            return null;
        Float authored = source.ground().maxSlopeDeg();
        return VehiclePhysicsUnits.isUsablePositive(authored) ? authored : null;
    }

    /** Draft is only meaningful for a hull that actually floats. */
    @Nullable
    private static Float resolveDraft(RealWorldVehicleSpec source, LegacyPhysicsHints hints)
    {
        Float authored = source.marine().draftM();
        if (!VehiclePhysicsUnits.isUsablePositive(authored) || !hints.floatOnWater())
            return null;
        return authored;
    }

    /**
     * Derives a wheelbase from the fore-aft spread of the declared wheel
     * positions. Legacy type files put the fore-aft coordinate in the first
     * component, so no basis conversion is needed to measure the spread.
     *
     * @param forwardCoordinates fore-aft wheel coordinates in blocks
     * @return the spread in blocks, or {@code null} when fewer than two axles exist
     */
    @Nullable
    public static Float deriveWheelbase(float... forwardCoordinates)
    {
        return deriveSpread(forwardCoordinates);
    }

    /** Derives a track width from the lateral spread of the declared wheel positions. */
    @Nullable
    public static Float deriveTrackWidth(float... rightCoordinates)
    {
        return deriveSpread(rightCoordinates);
    }

    @Nullable
    private static Float deriveSpread(float @Nullable [] coordinates)
    {
        if (coordinates == null || coordinates.length < 2)
            return null;
        float minimum = Float.POSITIVE_INFINITY;
        float maximum = Float.NEGATIVE_INFINITY;
        int counted = 0;
        for (float coordinate : coordinates)
        {
            if (!Float.isFinite(coordinate))
                continue;
            minimum = Math.min(minimum, coordinate);
            maximum = Math.max(maximum, coordinate);
            ++counted;
        }
        if (counted < 2)
            return null;
        float spread = maximum - minimum;
        return VehiclePhysicsUnits.isUsablePositive(spread) ? spread : null;
    }

    private static float orZero(@Nullable Float value)
    {
        return VehiclePhysicsUnits.isUsablePositive(value) ? value : 0F;
    }
}
