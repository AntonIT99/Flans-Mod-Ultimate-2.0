package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AAGunTypeTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "aaguns"));

    private static AAGunType read(String... lines)
    {
        AAGunType type = new AAGunType();
        type.read(new TypeFile("testAaGun", EnumType.AA_GUN, PACK, List.of(lines)));
        return type;
    }

    @Test
    void roundsPerMinuteOverridesLegacyShootDelay()
    {
        AAGunType type = read("ShortName testAaGun", "ShootDelay 8", "RoundsPerMin 600");
        assertEquals(2F, type.getShootDelay(), 1.0E-6F);
        assertEquals(600F, type.getRoundsPerMin());
    }

    @Test
    void legacyShootDelayRemainsWhenRoundsPerMinuteIsAbsent()
    {
        AAGunType type = read("ShortName testAaGun", "ShootDelay 8");
        assertEquals(8F, type.getShootDelay(), 1.0E-6F);
    }

    @Test
    void gunDeclaringNoRateKeepsItsOneShotPerTickCadence()
    {
        AAGunType type = read("ShortName testAaGun");
        assertEquals(1F, type.getShootDelay(), 1.0E-6F);
    }

    @Test
    void rateAboveTwelveHundredRoundsPerMinuteStaysSubTick()
    {
        AAGunType type = read("ShortName testAaGun", "RoundsPerMin 2400");
        assertEquals(0.5F, type.getShootDelay(), 1.0E-6F);
    }

    @Test
    void hitBoxDefaultsToTheLegacyTwoBlockCube()
    {
        AAGunType type = read("ShortName testAaGun");
        assertEquals(2F, type.getHitBoxWidth());
        assertEquals(2F, type.getHitBoxHeight());
    }

    @Test
    void traverseSpeedDefaultsToLegacyAimAndReadsDegreesPerSecond()
    {
        assertEquals(0F, read("ShortName testAaGun").getTraverseSpeed());
        assertEquals(55F, read("ShortName testAaGun", "TraverseSpeed 55").getTraverseSpeed());
    }

    @Test
    void hitBoxSizeSetsBothSidesAndWidthOrHeightRefineIt()
    {
        AAGunType square = read("ShortName testAaGun", "HitBoxSize 1.5");
        assertEquals(1.5F, square.getHitBoxWidth());
        assertEquals(1.5F, square.getHitBoxHeight());

        AAGunType refined = read("ShortName testAaGun", "HitBoxSize 1.5", "HitBoxHeight 2.5");
        assertEquals(1.5F, refined.getHitBoxWidth());
        assertEquals(2.5F, refined.getHitBoxHeight());
    }

    @Test
    void hitBoxDimensionsHaveNoMaximumButKeepAMinimum()
    {
        AAGunType type = read("ShortName testAaGun", "HitBoxWidth 8", "HitBoxHeight 9");
        assertEquals(8F, type.getHitBoxWidth());
        assertEquals(9F, type.getHitBoxHeight());

        AAGunType small = read("ShortName testAaGun", "HitBoxWidth 0", "HitBoxHeight 0");
        assertEquals(AAGunType.MIN_HIT_BOX_SIZE, small.getHitBoxWidth());
        assertEquals(AAGunType.MIN_HIT_BOX_SIZE, small.getHitBoxHeight());
    }

    @Test
    void barrelLinesReadWholeAndFractionalPixels()
    {
        AAGunType type = read("ShortName testAaGun", "NumBarrels 2", "Barrel 0 88 40 0",
            "Barrel 1 45.25 -3.5 6");
        assertEquals(88F, type.getBarrelX()[0]);
        assertEquals(40F, type.getBarrelY()[0]);
        assertEquals(45.25F, type.getBarrelX()[1]);
        assertEquals(-3.5F, type.getBarrelY()[1]);
        assertEquals(6F, type.getBarrelZ()[1]);
    }

    @Test
    void debugBarrelsKeepHundredthsAndResetToTheAuthoredLine()
    {
        AAGunType type = read("ShortName testAaGun", "Barrel 0 45 18 6");
        assertTrue(type.setDebugBarrel(0, new Vector3f(44.1234F, 17.996F, -0.004F)));
        assertEquals(44.12F, type.getBarrelX()[0], 1.0E-5F);
        assertEquals(18F, type.getBarrelY()[0], 1.0E-5F);
        assertEquals(0F, type.getBarrelZ()[0], 1.0E-5F);

        type.resetDebugOverrides();
        assertEquals(45F, type.getBarrelX()[0]);
        assertEquals(6F, type.getBarrelZ()[0]);
    }

    @Test
    void realisticHealthUsesMass()
    {
        AAGunType scaled = read("ShortName testAaGun", "Health 20", "RealMassKg 1000",
            "UseRealisticVehicleHealth true");
        assertTrue(scaled.isRealisticVehicleHealthEnabled());
        assertEquals(500, scaled.getHealth());
    }

    @Test
    void aTrustedPackFiresFromTheBarrelLinesItWrites()
    {
        AAGunType type = read("ShortName testAaGun", "NumBarrels 2", "Barrel 0 10 20 -3");
        type.setTrustBarrelLines(true);

        assertTrue(type.firesFromBarrelLine(0));
        assertFalse(type.firesFromBarrelLine(1), "a barrel with no line would fire from the gun's feet");
    }

    @Test
    void anUntrustedPackFiresFromTheModelWhateverItsLinesSay()
    {
        AAGunType type = read("ShortName testAaGun", "NumBarrels 2", "Barrel 0 10 20 -3", "Barrel 1 10 20 3");
        type.setTrustBarrelLines(false);

        assertFalse(type.firesFromBarrelLine(0));
        assertFalse(type.firesFromBarrelLine(1));
    }
}
