package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A warship is a warship whichever type class the pack filed it under. Some hulls are
 * {@code VehicleType} definitions; one, whose model extends {@code ModelPlane}, is a
 * {@code PlaneType}. The marine profile is therefore category-independent, so both
 * reach the same propulsion from the same authored displacement, power and speed.
 */
class MarineProfileTest
{
    private static final LegacyPhysicsHints FLOATS =
        new LegacyPhysicsHints(false, false, 0.5F, true, false, false);
    private static final LegacyPhysicsHints DRY =
        new LegacyPhysicsHints(false, false, 0.5F, false, false, false);

    @Test
    void aCompleteHullNeedsMassSpeedPowerAndADraft()
    {
        assertTrue(hull().hasCompleteMarineProfile());
        assertFalse(read("RealDisplacementT 16970", "RealMaxSpeedKn 32",
            "RealEnginePowerPS 132000").spec().hasCompleteMarineProfile(),
            "no draft means this could be any amphibian, not a hull");
        assertFalse(read("RealDisplacementT 16970", "RealDraftM 7.2",
            "RealMaxSpeedKn 32").spec().hasCompleteMarineProfile(),
            "no engine power means no propulsion to resolve");
    }

    @Test
    void theProfileActivatesForAHullFiledAsAPlane()
    {
        ResolvedVehiclePhysics physics = VehiclePhysicsResolver.resolve(
            EnumVehicleCategory.AIRCRAFT, hull(), null, FLOATS);
        assertTrue(physics.hasMarineProfile());
        assertFalse(physics.hasAircraftProfile(), "a ship has no wing and needs none");
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, physics.mode());
    }

    @Test
    void theProfileActivatesIdenticallyForAHullFiledAsAVehicle()
    {
        ResolvedVehiclePhysics asPlane = VehiclePhysicsResolver.resolve(
            EnumVehicleCategory.AIRCRAFT, hull(), null, FLOATS);
        ResolvedVehiclePhysics asVehicle = VehiclePhysicsResolver.resolve(
            EnumVehicleCategory.GROUND, hull(), null, FLOATS);

        assertTrue(asVehicle.hasMarineProfile());
        assertEquals(asPlane.massKg(), asVehicle.massKg());
        assertEquals(asPlane.maxSpeedKmh(), asVehicle.maxSpeedKmh());
        assertEquals(asPlane.baselinePowerKw(), asVehicle.baselinePowerKw(), 1.0E-3F,
            "the two ways of authoring a warship must resolve to the same propulsion");
    }

    @Test
    void aDefinitionThatDoesNotFloatNeverGetsAMarineProfile()
    {
        ResolvedVehiclePhysics physics = VehiclePhysicsResolver.resolve(
            EnumVehicleCategory.GROUND, hull(), null, DRY);
        assertFalse(physics.hasMarineProfile(),
            "the draft is dropped for a hull the resolver does not consider afloat");
    }

    @Test
    void navalUnitsFeedTheProfileDirectly()
    {
        RealWorldVehicleSpec spec = hull();
        assertEquals(16_970_000F, spec.massKg(), 1F, "displacement in tonnes becomes kilograms");
        assertEquals(32F * 1.852F, spec.maxSpeedKmh(), 1.0E-3F, "knots become km/h");
        assertEquals(7.2F, spec.marine().draftM());
    }

    private static RealWorldVehicleSpec hull()
    {
        // Admiral Hipper class: about 16 970 t full load, 32 kn, 132 000 PS, 7.2 m draft.
        return read("RealDisplacementT 16970", "RealMaxSpeedKn 32",
            "RealEnginePowerPS 132000", "RealDraftM 7.2").spec();
    }

    private static RealWorldSpecReader.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "marine"));
        return RealWorldSpecReader.read(new TypeFile("testHull", EnumType.PLANE, pack, List.of(lines)));
    }
}
