package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.guns.ShotCooldown;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableTypeShootDelayTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "delays"));

    private static VehicleType read(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(new TypeFile("syntheticVehicle", EnumType.VEHICLE, PACK, List.of(lines)));
        return type;
    }

    @Test
    void rateAboveTwelveHundredRoundsPerMinuteSurvivesAsAFraction()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun", "RoundsPerMinPrimary 3000");
        assertEquals(0.4F, type.shootDelay(false), 1.0E-6F);
    }

    @Test
    void subSecondDelayInSecondsSurvivesAsAFraction()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun", "ShootDelayPrimarySeconds 0.025");
        assertEquals(0.5F, type.shootDelay(false), 1.0E-6F);
    }

    @Test
    void declaredDelayBelowTheGuardIsHeldAtTheGuard()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun", "ShootDelayPrimary 0");
        assertEquals(ShotCooldown.MIN_DELAY, type.shootDelay(false), 1.0E-6F);
        assertTrue(type.shootDelay(false) > 0F);
    }

    @Test
    void secondsOutrankRoundsPerMinuteWhichOutrankTheLegacyDelay()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun",
            "ShootDelayPrimarySeconds 0.5", "RoundsPerMinPrimary 600", "ShellDelay 40");
        assertEquals(10F, type.shootDelay(false), 1.0E-6F);

        VehicleType byRate = read("Driver 0 0 0", "Primary gun", "RoundsPerMinPrimary 600", "ShellDelay 40");
        assertEquals(2F, byRate.shootDelay(false), 1.0E-6F);
    }

    @Test
    void bankWithoutAnyDelayKeyFallsBackToSixtyRoundsPerMinute()
    {
        VehicleType type = read("Driver 0 0 0", "Primary gun");
        assertEquals(20F, type.shootDelay(false), 1.0E-6F);
        assertFalse(type.shootDelayDeclared(false));
    }

    @Test
    void reloadTimeIsTheLongestFigureTheBankStatesAnywhere()
    {
        // The cadence stays on the fastest key by precedence, while the reload
        // takes the slowest of everything stated, so a pack carrying several keys
        // for backwards compatibility reloads at the pace the slowest one implies.
        VehicleType type = read("Driver 0 0 0", "Primary shell",
            "RoundsPerMinPrimary 600", "ShellDelay 40", "ReloadTimePrimary 30");
        assertEquals(2F, type.shootDelay(false), 1.0E-6F);
        assertEquals(40F, type.reloadTime(false), 1.0E-6F);
    }

    @Test
    void sharedReloadTimeCountsTowardsEitherBank()
    {
        VehicleType type = read("Driver 0 0 0", "Primary shell", "Secondary missile", "ReloadTime 60");
        assertEquals(60F, type.reloadTime(false), 1.0E-6F);
        assertEquals(60F, type.reloadTime(true), 1.0E-6F);
    }

    @Test
    void reloadTimeNeverFallsBelowTheBankCadence()
    {
        VehicleType type = read("Driver 0 0 0", "Primary shell", "ShellDelay 40", "ReloadTimePrimary 5");
        assertEquals(40F, type.reloadTime(false), 1.0E-6F);
    }

    @Test
    void reloadRoundsFallsBackToTheSharedKeyPerBank()
    {
        VehicleType shared = read("Driver 0 0 0", "Primary shell", "ReloadRounds 10");
        assertEquals(10, shared.reloadRounds(false));
        assertEquals(10, shared.reloadRounds(true));

        VehicleType perBank = read("Driver 0 0 0", "Primary shell", "ReloadRounds 10", "ReloadRoundsPrimary 4");
        assertEquals(4, perBank.reloadRounds(false));
        assertEquals(10, perBank.reloadRounds(true));

        assertEquals(0, read("Driver 0 0 0", "Primary shell").reloadRounds(false));
    }
}
