package com.flansmodultimate.common.types;

import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableOccupantVisibilityTest
{
    @Test
    void seatAliasesHideOnlyTheAuthoredSeatAndCombineWithDoorRules()
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("seats", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "visibility")),
            List.of("Driver 0 0 0", "Passenger 1 0 0 0 core", "Passenger 2 0 0 0 core", "invisiblePassenger 1 true", "SetPassengerInvisible 2 false", "SetPassengerInvisible 999 true")));
        assertFalse(FlansEntityTypes.hidesOccupants(type, false, 0));
        assertTrue(FlansEntityTypes.hidesOccupants(type, false, 1));
        assertTrue(FlansEntityTypes.hidesOccupants(type, true, 1));
        assertFalse(FlansEntityTypes.hidesOccupants(type, false, 2));
        type.setPlayerInvisibleOnDoorClose = true;
        assertTrue(FlansEntityTypes.hidesOccupants(type, false, 2));
        assertFalse(FlansEntityTypes.hidesOccupants(type, true, 2));
    }

    @Test
    void doorRulesFollowBothStatesAndUnconditionalHidingWins()
    {
        for (boolean always : List.of(false, true))
            for (boolean open : List.of(false, true))
                for (boolean closed : List.of(false, true))
                {
                    VehicleType type = new VehicleType();
                    type.read(new TypeFile("visibility", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "visibility")),
                        List.of("Driver 0 0 0", "SetPlayerInvisible " + always, "SetPlayerInvisibleOnDoorOpen " + open, "SetPlayerInvisibleOnDoorClose " + closed)));
                    assertEquals(always || open, FlansEntityTypes.hidesOccupants(type, true));
                    assertEquals(always || closed, FlansEntityTypes.hidesOccupants(type, false));
                }
        assertFalse(FlansEntityTypes.hidesOccupants(new GunType(), false));
    }
}
