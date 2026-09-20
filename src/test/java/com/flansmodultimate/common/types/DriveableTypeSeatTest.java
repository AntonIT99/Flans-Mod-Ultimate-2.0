package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.SeatInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableTypeSeatTest
{
    @Test
    void opticsAreReadAfterSeatConstructionForVehiclesAndPlanes()
    {
        VehicleType vehicle = readVehicle("Driver 0 0 0", "HasScope true", "Gunsight day thermal", "GunsightZoom 3 8",
            "Passenger 1 16 32 48 core", "OpticsModeSeat 1 true", "SeatGunsight 1 periscope");
        assertEquals(8F, vehicle.getOptics().zoom(1));
        org.junit.jupiter.api.Assertions.assertTrue(vehicle.getSeat(1).getOptics().isOpticsMode());
        assertEquals("periscope", vehicle.getSeat(1).getOptics().overlay(0));

        PlaneType plane = new PlaneType();
        plane.read(new TypeFile("plane", EnumType.PLANE,
            new ContentPack("test", Path.of("build", "test-packs", "seats")), List.of(
                "Pilot 0 0 0", "PilotOptics true", "SeatGunsight 0 tv flir", "SeatGunsightZoom 0 2 10")));
        org.junit.jupiter.api.Assertions.assertTrue(plane.getSeat(0).getOptics().isOpticsMode());
        assertEquals(10F, plane.getSeat(0).getOptics().zoom(1));
    }

    @Test
    void passengerAndGunOriginUseLegacyModelPixels()
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "seats"));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, List.of(
            "Driver 0 0 0",
            "Passenger 1 16 32 48 core -90 90 -20 30",
            "GunOrigin 1 64 80 96")));

        SeatInfo seat = type.getSeat(1);
        assertEquals(1F, seat.getPosition().x);
        assertEquals(2F, seat.getPosition().y);
        assertEquals(3F, seat.getPosition().z);
        assertEquals(4F, seat.getGunOrigin().x);
        assertEquals(5F, seat.getGunOrigin().y);
        assertEquals(6F, seat.getGunOrigin().z);
    }

    @Test
    void vehicleTurretRotationSpeedProvidesConvertedDriverYawFallback()
    {
        VehicleType type = readVehicle(
            "Driver 0 0 0",
            "TurretRotationSpeed 0.06");

        assertEquals(2F, type.getSeat(0).getAimingSpeed().x, 0.0001F);
        assertEquals(2F, type.getSeat(0).getAimingSpeed().y);
    }

    @Test
    void driverAimSpeedHasPriorityOverTurretRotationSpeed()
    {
        VehicleType type = readVehicle(
            "Driver 0 0 0",
            "TurretRotationSpeed 0.06",
            "DriverAimSpeed 0.75 0.5 0");

        assertEquals(0.75F, type.getSeat(0).getAimingSpeed().x);
        assertEquals(0.5F, type.getSeat(0).getAimingSpeed().y);
    }

    private static VehicleType readVehicle(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "seats"));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, List.of(lines)));
        return type;
    }
}
