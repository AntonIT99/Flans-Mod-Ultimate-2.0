package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.ImpulseMass;
import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics.MassSource;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * End-to-end through the real parser: the mass pushes are weighed against comes
 * from RealMassKg, then a plausible legacy Mass (tonnes for vehicles, kilograms
 * for planes), then the class fallback, and resolving it never disturbs the
 * legacy propulsion fields.
 */
class DriveableTypeImpulseMassTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "test"));

    @Test
    void realMassKgWinsOverLegacyMass()
    {
        ImpulseMass mass = vehicle("RealMassKg 64000", "Mass 1500").getImpulseMass();
        assertEquals(64_000F, mass.massKg());
        assertEquals(MassSource.REAL_MASS, mass.source());
    }

    @Test
    void aPlausibleLegacyMassIsReadAsKilograms()
    {
        // Tyrants And Plebeians authors its Spitfire exactly like this.
        PlaneType type = plane("mass 2666");
        ImpulseMass mass = type.getImpulseMass();
        assertEquals(2_666F, mass.massKg());
        assertEquals(MassSource.LEGACY_MASS, mass.source());
        assertEquals(2_666F, type.getMass(), "the legacy propulsion field keeps its own reading");
    }

    @Test
    void aVehicleLegacyMassIsReadAsTonnes()
    {
        VehicleType type = vehicle("Mass 1.0");
        ImpulseMass mass = type.getImpulseMass();
        assertEquals(1_000F, mass.massKg());
        assertEquals(MassSource.LEGACY_MASS, mass.source());
        assertEquals(1F, type.getMass(), "the legacy propulsion field keeps its own reading");
    }

    @Test
    void aVehicleMassLighterThanTheMinimumFallsBackToTheGroundVehicleDefault()
    {
        ImpulseMass mass = vehicle("Mass 0.05").getImpulseMass();
        assertEquals(MassSource.FALLBACK, mass.source());
        assertEquals((float) (ModCommonConfig.DEFAULT_FALLBACK_GROUND_VEHICLE_MASS_TONS * ModCommonConfig.KILOGRAMS_PER_TON),
            mass.massKg());
    }

    @Test
    void aPlanePlaceholderMassFallsBackToTheAircraftDefault()
    {
        // The modern warfare pack authored its F-22 exactly like this.
        ImpulseMass mass = plane("Mass 2.7").getImpulseMass();
        assertEquals(MassSource.FALLBACK, mass.source());
        assertEquals((float) (ModCommonConfig.DEFAULT_FALLBACK_AIRCRAFT_MASS_TONS * ModCommonConfig.KILOGRAMS_PER_TON),
            mass.massKg());
    }

    @Test
    void aPlaneWithoutAnyMassFallsBackToTheAircraftDefault()
    {
        PlaneType type = plane("Model Spitfire");
        assertNull(type.getAuthoredMassKg());
        ImpulseMass mass = type.getImpulseMass();
        assertEquals(MassSource.FALLBACK, mass.source());
        assertEquals((float) (ModCommonConfig.DEFAULT_FALLBACK_AIRCRAFT_MASS_TONS * ModCommonConfig.KILOGRAMS_PER_TON),
            mass.massKg());
    }

    @Test
    void anAAGunUsesItsRealMassOrTheAAGunDefault()
    {
        ImpulseMass real = aaGun("ShortName testAaGun", "RealMassKg 450").getImpulseMass();
        assertEquals(450F, real.massKg());
        assertEquals(MassSource.REAL_MASS, real.source());

        ImpulseMass fallback = aaGun("ShortName testAaGun").getImpulseMass();
        assertEquals(MassSource.FALLBACK, fallback.source());
        assertEquals((float) (ModCommonConfig.DEFAULT_FALLBACK_AA_GUN_MASS_TONS * ModCommonConfig.KILOGRAMS_PER_TON),
            fallback.massKg());
    }

    private static VehicleType vehicle(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(driveableFile(EnumType.VEHICLE, "testVehicle", lines));
        return type;
    }

    private static PlaneType plane(String... lines)
    {
        PlaneType type = new PlaneType();
        type.read(driveableFile(EnumType.PLANE, "testPlane", lines));
        return type;
    }

    private static AAGunType aaGun(String... lines)
    {
        AAGunType type = new AAGunType();
        type.read(new TypeFile("testAaGun", EnumType.AA_GUN, PACK, List.of(lines)));
        return type;
    }

    /** A driver line keeps the parser off its logging path, as in the other type tests. */
    private static TypeFile driveableFile(EnumType type, String name, String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        return new TypeFile(name, type, PACK, definition);
    }
}
