package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleTypeDoorTest
{
    @Test
    void shootWithOpenDoorArmsTheWeaponsOnlyWhileTheDoorIsOpen()
    {
        VehicleType type = vehicle("HasDoor true", "ShootWithOpenDoor true");
        assertTrue(type.doorAllowsFiring(true));
        assertFalse(type.doorAllowsFiring(false));
    }

    @Test
    void otherVehiclesFireWhateverTheStateOfTheirDoor()
    {
        VehicleType type = vehicle("HasDoor true");
        assertTrue(type.doorAllowsFiring(true));
        assertTrue(type.doorAllowsFiring(false));
    }

    private static VehicleType vehicle(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("door", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "test")), definition));
        return type;
    }
}
