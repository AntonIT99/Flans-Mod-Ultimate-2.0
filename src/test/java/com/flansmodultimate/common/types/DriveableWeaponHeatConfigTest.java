package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.platform.entity.EntityTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableWeaponHeatConfigTest
{
    @BeforeAll
    static void bootstrap() throws ReflectiveOperationException
    {
        EntityTestBootstrap.initialize();
    }

    @Test
    void legacyCaseInsensitiveFieldsWorkForAircraftAndVehicles()
    {
        for (DriveableType type : List.of(new VehicleType(), new PlaneType()))
        {
            assertEquals(0, type.getOverheatLimit());
            assertEquals(75, type.getOverheatPenalty());
            assertEquals(1, type.getCoolingBonus());
            var pack = new ContentPack("test", Path.of("build", "test-packs", "heat"));
            type.read(new TypeFile("heat", EnumType.VEHICLE, pack, List.of("Driver 0 0 0", "overheatLimit 250", "overheatPenalty 30", "coolingBonus 4", "overheatSound hot")));
            assertEquals(250, type.getOverheatLimit());
            assertEquals(30, type.getOverheatPenalty());
            assertEquals(4, type.getCoolingBonus());
            assertTrue(type.getOverheatSound().endsWith("hot"));
            type.read(new TypeFile("heat", EnumType.VEHICLE, pack, List.of("Driver 0 0 0", "OverheatLimit -1", "OverheatPenalty -5", "CoolingBonus -4")));
            assertEquals(0, type.getOverheatLimit());
            assertEquals(0, type.getOverheatPenalty());
            assertEquals(0, type.getCoolingBonus());
        }
    }
}
