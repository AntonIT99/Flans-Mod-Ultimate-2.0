package com.flansmodultimate.common.types;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.damage.PartDamageKind;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The type-file keys that shape how a driveable shows damage and dies. */
class DriveableTypeDamageEffectsTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "damage-effects"));

    private static VehicleType read(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, PACK, List.of(lines)));
        return type;
    }

    @Test
    void defaultsCookOffLeaveAWreckAndUseTheNamedDamageEffect()
    {
        VehicleType type = read("Driver 0 0 0");
        assertTrue(type.isCookOff());
        assertFalse(type.isNoWreck());
        assertTrue(type.getTurretTossChance() < 0F, "a negative chance asks for the default");
        assertEquals(PartDamageKind.TURRET, type.getPartDamageKind(EnumDriveablePart.TURRET));
        assertEquals(PartDamageKind.ENGINE, type.getPartDamageKind(EnumDriveablePart.ENGINE_ROOM_2));
    }

    @Test
    void readsCookOffTurretTossAndWreckKeys()
    {
        VehicleType type = read("Driver 0 0 0", "CookOff false", "TurretTossChance 0.8", "NoWreck true");
        assertFalse(type.isCookOff());
        assertEquals(0.8F, type.getTurretTossChance());
        assertTrue(type.isNoWreck());
    }

    @Test
    void partDamageEffectOverridesTheDefaultAndIgnoresUnknownNames()
    {
        VehicleType type = read("Driver 0 0 0", "PartDamageEffect turret boiler", "PartDamageEffect core Electronics", "PartDamageEffect nose smoky", "PartDamageEffect nopart engine");
        assertEquals(PartDamageKind.BOILER, type.getPartDamageKind(EnumDriveablePart.TURRET));
        assertEquals(PartDamageKind.ELECTRONICS, type.getPartDamageKind(EnumDriveablePart.CORE));
        assertEquals(PartDamageKind.AIRFOIL, type.getPartDamageKind(EnumDriveablePart.NOSE), "an unknown effect keeps the default");
    }
}
