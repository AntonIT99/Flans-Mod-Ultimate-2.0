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
}
