package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpicShipTypeTest
{
    @Test
    void oldDefinitionsKeepOrdinaryFlotation()
    {
        assertFalse(read(new VehicleType(), EnumType.VEHICLE, "Boat").isEpicShip());
        assertFalse(read(new PlaneType(), EnumType.PLANE, "Boat").isEpicShip());
    }

    @Test
    void legacyAndCanonicalSpellingsEnableBothShipRepresentations()
    {
        assertTrue(read(new VehicleType(), EnumType.VEHICLE, "epicShip true").isEpicShip());
        assertTrue(read(new PlaneType(), EnumType.PLANE, "EpicShip True").isEpicShip());
        assertTrue(read(new PlaneType(), EnumType.PLANE, "EPICSHIP true").isEpicShip());
    }

    @Test
    void optInPreservesAuthoredFlotationSettingsAndExplicitFalse()
    {
        DriveableType type = read(new VehicleType(), EnumType.VEHICLE, "Boat", "EpicShip true", "Buoyancy 0.05", "FloatOffset -1.8");
        assertTrue(type.isFloatOnWater());
        assertEquals(0.05F, type.getBuoyancy());
        assertEquals(-1.8F, type.getFloatOffset());
        assertFalse(read(new VehicleType(), EnumType.VEHICLE, "EpicShip false").isEpicShip());
    }

    private static DriveableType read(DriveableType type, EnumType kind, String... lines)
    {
        List<String> definition = new ArrayList<>(List.of(kind == EnumType.PLANE ? "Pilot 0 0 0" : "Driver 0 0 0"));
        definition.addAll(List.of(lines));
        type.read(new TypeFile("ship", kind, new ContentPack("test", Path.of("build", "test-packs", "test")), definition));
        return type;
    }
}
