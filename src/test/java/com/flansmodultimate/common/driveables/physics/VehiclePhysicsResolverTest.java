package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Activation and precedence. A coupled profile must be all-or-nothing, and each
 * independently usable parameter must take effect on its own without dragging a
 * half-built propulsion model along with it.
 */
class VehiclePhysicsResolverTest
{
    private static final LegacyPhysicsHints WHEELED =
        new LegacyPhysicsHints(false, false, 0.5F, false, false, false);
    private static final LegacyPhysicsHints TANK =
        new LegacyPhysicsHints(true, false, 0.5F, false, false, false);
    private static final LegacyPhysicsHints NO_REVERSE =
        new LegacyPhysicsHints(false, false, 0F, false, false, false);
    private static final LegacyPhysicsHints BOAT =
        new LegacyPhysicsHints(false, false, 0.25F, true, false, false);

    // ------------------------------------------------------------------ legacy

    @Test
    void noRealWorldDataMeansPureLegacy()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, WHEELED);
        assertEquals(EnumVehiclePhysicsMode.LEGACY, resolved.mode());
        assertFalse(resolved.isActive());
        assertFalse(resolved.hasGroundPropulsion());
        assertFalse(resolved.hasAircraftProfile());
        assertFalse(resolved.hasReverseSpeedOverride());
        assertFalse(resolved.hasSlopeLimit());
        assertFalse(resolved.hasDraft());
    }

    @Test
    void legacyTypesKeepTheHistoricalMovementClampAndWheelProbe()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, WHEELED);
        assertEquals(8D, resolved.movementClampBlocksPerTick());
        assertEquals(1.5D, resolved.wheelPredictionBlocks(1D));
    }

    // ---------------------------------------------------------- ground profile

    @Test
    void aCompleteGroundProfileActivatesTheNewPropulsionPath()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, groundSpec(), WHEELED);
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, resolved.mode());
        assertTrue(resolved.hasGroundPropulsion());
        assertFalse(resolved.hasAircraftProfile());
        assertEquals(2400F, resolved.massKg());
        assertEquals(140F, resolved.baselinePowerKw());
        assertEquals(113F, resolved.maxSpeedKmh());
        assertEquals(113D / 72D, resolved.maxSpeedBlocksPerTick(1D), 1.0E-9D);
        assertEquals(113D / 144D, resolved.maxSpeedBlocksPerTick(0.5D), 1.0E-9D);
    }

    @Test
    void aPartialGroundProfileStaysEntirelyOnLegacyPropulsion()
    {
        // Mass and power but no top speed: half a model is worse than none.
        RealWorldVehicleSpec partial = new RealWorldVehicleSpec(2400F, null, 140F, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, RealWorldVehicleSpec.Ground.EMPTY, RealWorldVehicleSpec.Marine.EMPTY);
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, partial, WHEELED);
        assertFalse(resolved.hasGroundPropulsion());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, resolved.mode());
        assertEquals(0F, resolved.massKg(), "an inactive profile must not advertise half-derived values");
        assertEquals(0F, resolved.powerToWeightKwPerKg());
    }

    @Test
    void groundKeysOnAnAircraftDoNotActivateAnything()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.AIRCRAFT, groundSpec(), WHEELED);
        assertFalse(resolved.hasGroundPropulsion());
        assertFalse(resolved.hasAircraftProfile());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, resolved.mode());
    }

    @Test
    void theRealWorldProfileRaisesTheMovementClampAndWheelProbe()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.AIRCRAFT, aircraftSpec(), WHEELED);
        assertEquals(32D, resolved.movementClampBlocksPerTick());
        // 635 km/h is 8.82 blocks per tick, well beyond the historical 1.5 probe.
        assertEquals(635D / 72D, resolved.wheelPredictionBlocks(1D), 1.0E-9D);
    }

    // -------------------------------------------------------- aircraft profile

    @Test
    void aCompleteAircraftProfileActivatesAndDerivesItsRatios()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.AIRCRAFT, aircraftSpec(), WHEELED);
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, resolved.mode());
        assertTrue(resolved.hasAircraftProfile());
        assertEquals(0.3436F, resolved.powerToWeightKwPerKg(), 1.0E-4F);
        assertEquals(128.56F, resolved.wingLoadingKgPerM2(), 1.0E-2F);
        assertTrue(resolved.rollInertiaFactor() > 0F);
    }

    @Test
    void anAircraftMissingItsWingDataStaysLegacy()
    {
        RealWorldVehicleSpec noWings = new RealWorldVehicleSpec(2890F, 635F, 993F, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, RealWorldVehicleSpec.Ground.EMPTY, RealWorldVehicleSpec.Marine.EMPTY);
        assertFalse(resolve(EnumVehicleCategory.AIRCRAFT, noWings, WHEELED).hasAircraftProfile());
    }

    @Test
    void aCompleteProfileOutranksTheLegacyExperimentalFlags()
    {
        LegacyPhysicsHints experimental = new LegacyPhysicsHints(false, false, 0.5F, false, true, true);
        assertTrue(resolve(EnumVehicleCategory.AIRCRAFT, aircraftSpec(), experimental).hasAircraftProfile());
        assertTrue(resolve(EnumVehicleCategory.GROUND, groundSpec(), experimental).hasGroundPropulsion());
    }

    // -------------------------------------------------- independent overrides

    @Test
    void slopeAloneGivesLegacyPropulsionWithASlopeLimit()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, spec(ground(null, null, 35F)), WHEELED);
        assertEquals(EnumVehiclePhysicsMode.LEGACY_WITH_OVERRIDES, resolved.mode());
        assertFalse(resolved.hasGroundPropulsion());
        assertTrue(resolved.hasSlopeLimit());
        assertEquals(35F, resolved.maxSlopeDeg());
    }

    @Test
    void reverseSpeedAloneGivesLegacyPropulsionWithAReverseCap()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, spec(ground(null, 8F, null)), WHEELED);
        assertEquals(EnumVehiclePhysicsMode.LEGACY_WITH_OVERRIDES, resolved.mode());
        assertFalse(resolved.hasGroundPropulsion());
        assertTrue(resolved.hasReverseSpeedOverride());
        assertEquals(8D / 72D, resolved.reverseSpeedBlocksPerTick(1D), 1.0E-9D);
        assertEquals(8D / 144D, resolved.reverseSpeedBlocksPerTick(0.5D), 1.0E-9D);
    }

    @Test
    void aReverseCapNeverEnablesReverseOnAVehicleThatLegacyForbidsIt()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, spec(ground(null, 8F, null)), NO_REVERSE);
        assertFalse(resolved.hasReverseSpeedOverride());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, resolved.mode());
        assertEquals(0D, resolved.reverseSpeedBlocksPerTick(1D));
    }

    @Test
    void driveTypeAloneGivesLegacyPropulsionWithAnExplicitDriveLayout()
    {
        ResolvedVehiclePhysics resolved =
            resolve(EnumVehicleCategory.GROUND, spec(ground(EnumDriveType.AWD, null, null)), WHEELED);
        assertEquals(EnumVehiclePhysicsMode.LEGACY_WITH_OVERRIDES, resolved.mode());
        assertFalse(resolved.hasGroundPropulsion());
        assertTrue(resolved.driveTypeExplicit());
        assertEquals(EnumDriveType.AWD, resolved.driveType());
    }

    @Test
    void anAbsentDriveTypeIsInferredFromTheLegacyBooleansAndStaysImplicit()
    {
        assertEquals(EnumDriveType.RWD,
            resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, WHEELED).driveType());
        assertEquals(EnumDriveType.TRACKED,
            resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, TANK).driveType());
        assertEquals(EnumDriveType.AWD, resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY,
            new LegacyPhysicsHints(false, true, 0.5F, false, false, false)).driveType());
        assertFalse(resolve(EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, TANK).driveTypeExplicit(),
            "inference must stay implicit so legacy traction selection is untouched");
    }

    @Test
    void anExplicitDriveTypeOverridesTheLegacyInference()
    {
        ResolvedVehiclePhysics resolved =
            resolve(EnumVehicleCategory.GROUND, spec(ground(EnumDriveType.RWD, null, null)), TANK);
        assertEquals(EnumDriveType.RWD, resolved.driveType());
        assertTrue(resolved.driveTypeExplicit());
    }

    @Test
    void draftAppliesOnlyToAHullThatFloats()
    {
        RealWorldVehicleSpec withDraft = new RealWorldVehicleSpec(null, null, null, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, RealWorldVehicleSpec.Ground.EMPTY,
            new RealWorldVehicleSpec.Marine(1.5F));
        assertTrue(resolve(EnumVehicleCategory.GROUND, withDraft, BOAT).hasDraft());
        assertFalse(resolve(EnumVehicleCategory.GROUND, withDraft, WHEELED).hasDraft(),
            "a land vehicle must not gain flotation just because a draft was authored");
    }

    @Test
    void slopeLimitsDoNotApplyToAircraft()
    {
        assertFalse(resolve(EnumVehicleCategory.AIRCRAFT, spec(ground(null, null, 35F)), WHEELED).hasSlopeLimit());
    }

    @Test
    void overridesRemainActiveAlongsideACompleteProfile()
    {
        RealWorldVehicleSpec full = new RealWorldVehicleSpec(2400F, 113F, 140F, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, ground(EnumDriveType.AWD, 8F, 35F),
            RealWorldVehicleSpec.Marine.EMPTY);
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, full, WHEELED);
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, resolved.mode());
        assertTrue(resolved.hasGroundPropulsion());
        assertTrue(resolved.hasSlopeLimit());
        assertTrue(resolved.hasReverseSpeedOverride());
        assertEquals(EnumDriveType.AWD, resolved.driveType());
    }

    // ------------------------------------------------------ engine interaction

    @Test
    void theInstalledEngineActsAsAMultiplierOnTheRealWorldBaseline()
    {
        ResolvedVehiclePhysics resolved = resolve(EnumVehicleCategory.GROUND, groundSpec(), WHEELED);
        assertEquals(140_000D, resolved.effectivePowerWatts(1F), 1.0E-6D);
        assertEquals(210_000D, resolved.effectivePowerWatts(1.5F), 1.0E-6D);
        // A missing or nonsensical engine modifier falls back to neutral.
        assertEquals(140_000D, resolved.effectivePowerWatts(0F), 1.0E-6D);
        assertEquals(140_000D, resolved.effectivePowerWatts(Float.NaN), 1.0E-6D);
    }

    // ------------------------------------------------------ geometry fallbacks

    @Test
    void wheelbaseAndTrackComeFromTheSpreadOfTheDeclaredWheels()
    {
        assertEquals(3F, VehiclePhysicsResolver.deriveWheelbase(-1.5F, -1.5F, 1.5F, 1.5F));
        assertEquals(2F, VehiclePhysicsResolver.deriveTrackWidth(-1F, 1F, -1F, 1F));
        assertNull(VehiclePhysicsResolver.deriveWheelbase(1.5F), "one wheel is not an axle pair");
        assertNull(VehiclePhysicsResolver.deriveWheelbase(1.5F, 1.5F), "coincident wheels have no spread");
        assertNull(VehiclePhysicsResolver.deriveWheelbase(Float.NaN, 1.5F));
    }

    @Test
    void coreBoxDimensionsMapLengthToDepthAndWidthToWidth()
    {
        // The S-100's core box: 561 by 86 by 84 model pixels, so 35.06 by 5.375
        // by 5.25 blocks. The real boat is 34.94 m long with a 5.06 m beam.
        VehicleGeometry geometry = VehicleGeometry.fromCoreBox(84F / 16F, 86F / 16F, 561F / 16F, null, null);
        assertEquals(35.0625F, geometry.lengthM(), 1.0E-4F);
        assertEquals(5.25F, geometry.widthM(), 1.0E-4F);
        assertEquals(5.375F, geometry.heightM(), 1.0E-4F);
        assertEquals(35.0625F, geometry.horizontalExtentM(), 1.0E-4F);
    }

    @Test
    void nullInputsResolveToLegacyRatherThanThrowing()
    {
        ResolvedVehiclePhysics resolved = VehiclePhysicsResolver.resolve(null, null, null, null);
        assertEquals(EnumVehiclePhysicsMode.LEGACY, resolved.mode());
        assertEquals(EnumVehicleCategory.OTHER, resolved.category());
    }

    // ------------------------------------------------------------------ helpers

    private static ResolvedVehiclePhysics resolve(EnumVehicleCategory category, RealWorldVehicleSpec spec,
                                                  LegacyPhysicsHints hints)
    {
        return VehiclePhysicsResolver.resolve(category, spec, VehicleGeometry.EMPTY, hints);
    }

    private static RealWorldVehicleSpec groundSpec()
    {
        return new RealWorldVehicleSpec(2400F, 113F, 140F, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, RealWorldVehicleSpec.Ground.EMPTY, RealWorldVehicleSpec.Marine.EMPTY);
    }

    private static RealWorldVehicleSpec aircraftSpec()
    {
        return new RealWorldVehicleSpec(2890F, 635F, 993F, null,
            new RealWorldVehicleSpec.Aircraft(11.23F, 22.48F, 17F),
            RealWorldVehicleSpec.Ground.EMPTY, RealWorldVehicleSpec.Marine.EMPTY);
    }

    private static RealWorldVehicleSpec spec(RealWorldVehicleSpec.Ground ground)
    {
        return new RealWorldVehicleSpec(null, null, null, null,
            RealWorldVehicleSpec.Aircraft.EMPTY, ground, RealWorldVehicleSpec.Marine.EMPTY);
    }

    private static RealWorldVehicleSpec.Ground ground(EnumDriveType driveType, Float reverseKmh, Float slopeDeg)
    {
        return new RealWorldVehicleSpec.Ground(driveType, reverseKmh, slopeDeg);
    }
}
