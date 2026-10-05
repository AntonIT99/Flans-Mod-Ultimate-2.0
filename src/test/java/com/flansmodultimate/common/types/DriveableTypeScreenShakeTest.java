package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DriveableTypeScreenShakeTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "screenshake"));

    private static VehicleType read(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("syntheticVehicle", EnumType.VEHICLE, PACK, List.of(lines)));
        return type;
    }

    @Test
    void screenShakeIsOptIn()
    {
        VehicleType type = read("Driver 0 0 0", "Primary shell", "Secondary gun", "CoaxRecoil true");
        assertNull(type.screenShake(false));
        assertNull(type.screenShake(true));
    }

    @Test
    void mainGunUsesLabjacDefaults()
    {
        VehicleType type = read("Driver 0 0 0", "Primary shell", "FancyScreenShake true");
        assertEquals(new DriveableType.ScreenShake(1F, 0.18F), type.screenShake(false));
        assertEquals(10F, type.getFancyScreenShakeRange());
    }

    @Test
    void shellsAndMissilesKickFromEitherBank()
    {
        VehicleType type = read("Driver 0 0 0", "Primary missile", "Secondary shell", "FancyScreenShake true",
            "FancyScreenShakePrimaryIntensity 2.5", "FancyScreenShakePrimaryDuration 0.4");
        DriveableType.ScreenShake expected = new DriveableType.ScreenShake(2.5F, 0.4F);
        assertEquals(expected, type.screenShake(false));
        assertEquals(expected, type.screenShake(true));
    }

    @Test
    void primaryGunKicksLikeTheMainGun()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun", "FancyScreenShake true");
        assertEquals(new DriveableType.ScreenShake(1F, 0.18F), type.screenShake(false));
    }

    @Test
    void coaxialGunKicksOnlyWithCoaxRecoil()
    {
        assertNull(read("Driver 0 0 0", "Primary shell", "Secondary gun", "FancyScreenShake true").screenShake(true));

        VehicleType type = read("Driver 0 0 0", "Primary shell", "Secondary gun", "FancyScreenShake true",
            "CoaxRecoil true", "FancyScreenShakeCoaxIntensity 0.5", "FancyScreenShakeCoaxDuration 0.1");
        assertEquals(new DriveableType.ScreenShake(0.5F, 0.1F), type.screenShake(true));
    }

    @Test
    void droppedOrdnanceNeverKicks()
    {
        VehicleType type = read("Driver 0 0 0", "Primary bomb", "Secondary mine", "FancyScreenShake true", "CoaxRecoil true");
        assertNull(type.screenShake(false));
        assertNull(type.screenShake(true));
    }

    @Test
    void zeroOrNegativeSettingsDisableTheKick()
    {
        assertNull(read("Driver 0 0 0", "Primary shell", "FancyScreenShake true",
            "FancyScreenShakePrimaryIntensity -3").screenShake(false));
        assertNull(read("Driver 0 0 0", "Primary shell", "FancyScreenShake true",
            "FancyScreenShakePrimaryDuration 0").screenShake(false));
        assertNull(read("Driver 0 0 0", "Primary shell", "FancyScreenShake true",
            "FancyScreenShakeRange 0").screenShake(false));
    }

    @Test
    void keysAreCaseInsensitive()
    {
        VehicleType type = read("Driver 0 0 0", "Primary shell", "fancyscreenshake true",
            "FANCYSCREENSHAKEPRIMARYINTENSITY 3");
        assertEquals(new DriveableType.ScreenShake(3F, 0.18F), type.screenShake(false));
    }
}
