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
 * A helicopter has no wing, so the aircraft profile would never activate for one.
 * Rotor diameter and rotor count are published for every real rotorcraft, and the
 * swept disc stands in as the lifting surface.
 */
class RotorcraftGeometryTest
{
    @Test
    void aRotorDiameterSuppliesTheSpanAndTheDiscArea()
    {
        // UH-60 Black Hawk: 16.36 m main rotor.
        RealWorldVehicleSpec.Aircraft a = read("RealRotorDiameterM 16.36").spec().aircraft();
        assertEquals(16.36F, a.effectiveWingSpanM(), 1.0E-4F);
        assertEquals((float) (Math.PI * 8.18 * 8.18), a.effectiveWingAreaM2(), 1.0E-2F);
        assertEquals(1, a.effectiveRotorCount());
        assertTrue(a.usesRotorGeometry());
    }

    @Test
    void aTandemLayoutSweepsTwoDiscs()
    {
        // CH-47 Chinook: two 18.29 m rotors.
        RealWorldVehicleSpec.Aircraft a =
            read("RealRotorDiameterM 18.29", "RealRotorCount 2").spec().aircraft();
        assertEquals(2, a.effectiveRotorCount());
        assertEquals((float) (2 * Math.PI * 9.145 * 9.145), a.effectiveWingAreaM2(), 1.0E-2F);
        assertEquals(18.29F, a.effectiveWingSpanM(), 1.0E-4F, "span stays one rotor wide");
    }

    @Test
    void anAuthoredWingWinsOverRotorGeometry()
    {
        // A compound helicopter with real wings must keep them.
        RealWorldVehicleSpec.Aircraft a = read(
            "RealRotorDiameterM 16.36", "RealWingSpanM 5.0", "RealWingAreaM2 12.0").spec().aircraft();
        assertEquals(5.0F, a.effectiveWingSpanM());
        assertEquals(12.0F, a.effectiveWingAreaM2());
        assertFalse(a.usesRotorGeometry());
    }

    @Test
    void rotorGeometryCompletesTheAircraftProfileOnItsOwn()
    {
        RealWorldVehicleSpec spec = read(
            "RealMassKg 9979", "RealMaxSpeedKmh 295", "RealEnginePowerHp 3780",
            "RealRotorDiameterM 16.36", "RealClimbRateMs 4.5").spec();
        assertTrue(spec.hasCompleteAircraftProfile(),
            "a helicopter with no wing keys must still qualify");

        RealWorldVehicleSpec noRotor = read(
            "RealMassKg 9979", "RealMaxSpeedKmh 295", "RealEnginePowerHp 3780").spec();
        assertFalse(noRotor.hasCompleteAircraftProfile());
    }

    @Test
    void theResolverDerivesWingLoadingFromTheDisc()
    {
        ResolvedVehiclePhysics physics = VehiclePhysicsResolver.resolve(
            EnumVehicleCategory.AIRCRAFT,
            read("RealMassKg 9979", "RealMaxSpeedKmh 295", "RealEnginePowerHp 3780",
                 "RealRotorDiameterM 16.36").spec(),
            null, null);
        assertTrue(physics.hasAircraftProfile());
        float discArea = (float) (Math.PI * 8.18 * 8.18);
        assertEquals(VehiclePhysicsUnits.wingLoading(9979F, discArea), physics.wingLoadingKgPerM2(), 1.0E-3F,
            "disc loading is the rotorcraft equivalent of wing loading");
    }

    @Test
    void anAbsentOrInvalidRotorCountFallsBackToASingleRotor()
    {
        assertEquals(1, read("RealRotorDiameterM 10").spec().aircraft().effectiveRotorCount());
        RealWorldSpecReader.Result zero = read("RealRotorDiameterM 10", "RealRotorCount 0");
        assertEquals(1, zero.spec().aircraft().effectiveRotorCount());
        assertFalse(zero.warnings().isEmpty());
    }

    @Test
    void aMalformedRotorDiameterWarnsAndIsDropped()
    {
        RealWorldSpecReader.Result result = read("RealRotorDiameterM nonsense");
        assertNull(result.spec().aircraft().rotorDiameterM());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("RealRotorDiameterM")));
    }

    private static RealWorldSpecReader.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "rotor"));
        return RealWorldSpecReader.read(new TypeFile("testHeli", EnumType.PLANE, pack, List.of(lines)));
    }
}
