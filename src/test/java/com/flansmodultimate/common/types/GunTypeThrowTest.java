package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.guns.EnumFunction;
import com.flansmodultimate.common.guns.ShootingHelper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code SecondaryFunction Throw} turns a melee weapon such as a pilum into one that can also be
 * thrown, its velocity and dispersion coming from the ordinary gun keys.
 */
class GunTypeThrowTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));
    private static final float EPSILON = 1.0E-4F;

    @Test
    void aMeleeWeaponCanBeThrownWithItsSecondaryFunction()
    {
        GunType type = read("ShortName testPilum", "MeleeDamage 9", "SecondaryFunction Throw");

        assertEquals(EnumFunction.MELEE, type.getPrimaryFunction(), "a gun without ammo and with MeleeDamage still swings");
        assertEquals(EnumFunction.THROW, type.getSecondaryFunction());
        assertTrue(type.isThrowable());
    }

    @Test
    void anOrdinaryMeleeWeaponIsNotThrowable()
    {
        assertFalse(read("ShortName testGladius", "MeleeDamage 7").isThrowable());
    }

    @Test
    void throwMassMakesTheHitDamageKinetic()
    {
        GunType type = read("ShortName testPilum", "SecondaryFunction Throw", "ThrowMass 2000", "MuzzleVelocity 20.0");

        assertEquals(Math.round(ShootingHelper.getKineticDamage(2000F, 1F) * 2F) / 2F, type.getThrowDamage(null), EPSILON,
            "20 m/s is one block per tick, the speed the thrown weapon leaves the hand with");
    }

    @Test
    void throwDamageIsRoundedToHalfPoints()
    {
        // 700 g at 25 m/s is 6.83 kinetically
        assertEquals(7F, read("ShortName testJavelin", "SecondaryFunction Throw", "ThrowMass 700", "MuzzleVelocity 25.0").getThrowDamage(null), EPSILON);
        assertEquals(11F, read("ShortName testPilum", "SecondaryFunction Throw", "ThrowMass 2000", "MuzzleVelocity 20.0").getThrowDamage(null), EPSILON);
    }

    @Test
    void theChargeTimeDefaultsToTheTridentsAndCannotBeNegative()
    {
        assertEquals(10, read("ShortName testPilum", "SecondaryFunction Throw").getThrowChargeTime());
        assertEquals(4, read("ShortName testPilum", "SecondaryFunction Throw", "ThrowChargeTime 4").getThrowChargeTime());
        assertEquals(0, read("ShortName testPilum", "SecondaryFunction Throw", "ThrowChargeTime -3").getThrowChargeTime());
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testPilum", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
