package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code DecreaseRecoilYaw} is a divisor applied while crouched, so a gun
 * stating 2 fires with half its yaw recoil. It used to be tested for being
 * negative, which the parser had already made impossible, so every authored
 * value was silently ignored.
 */
class GunTypeSneakingRecoilTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "guns"));
    private static final float EPSILON = 1.0E-6F;
    private static final float DEFAULT_SNEAKING_YAW = 0.8F;

    @Test
    void anAuthoredDivisorDecreasesYawRecoil()
    {
        assertEquals(1F, GunType.sneakingYawRecoil(2F, 2F, DEFAULT_SNEAKING_YAW), EPSILON,
            "DecreaseRecoilYaw 2 is half the yaw recoil, which is what the 44 guns stating it ask for");
    }

    @Test
    void withoutOneTheModernMultiplierApplies()
    {
        assertEquals(1.6F, GunType.sneakingYawRecoil(2F, 0F, DEFAULT_SNEAKING_YAW), EPSILON);
        assertEquals(0.5F, GunType.sneakingYawRecoil(2F, 0F, 0.25F), EPSILON);
    }

    @Test
    void theLegacyDivisorWinsOverTheMultiplier()
    {
        assertEquals(1F, GunType.sneakingYawRecoil(2F, 2F, 0.25F), EPSILON,
            "DecreaseRecoil has the same priority over RecoilSneakingMultiplier on the pitch side");
    }

    @Test
    void aDivisorBelowOneIncreasesRecoil()
    {
        // "Decrease" is the parameter's name, not a constraint: the divisor is
        // honoured as written so an author can make crouching worse on purpose.
        assertEquals(4F, GunType.sneakingYawRecoil(2F, 0.5F, DEFAULT_SNEAKING_YAW), EPSILON);
    }

    @Test
    void anUnauthoredGunKeepsAZeroDivisor()
    {
        // Zero is what hands crouching over to the multiplier. The parser used
        // to fabricate 0.5 here, which under divisor semantics would have
        // doubled the yaw recoil of every gun that never asked for anything.
        assertEquals(0F, read("ShortName testGun").decreaseRecoilYaw, EPSILON);
    }

    @Test
    void anAuthoredDivisorSurvivesParsing()
    {
        assertEquals(2F, read("ShortName testGun", "DecreaseRecoilYaw 2.0").decreaseRecoilYaw, EPSILON);
    }

    @Test
    void aNonPositiveDivisorIsDiscardedRatherThanDividedBy()
    {
        assertEquals(0F, read("ShortName testGun", "DecreaseRecoilYaw -2").decreaseRecoilYaw, EPSILON);
        assertEquals(0F, read("ShortName testGun", "DecreaseRecoilYaw 0").decreaseRecoilYaw, EPSILON);
    }

    private static GunType read(String... lines)
    {
        GunType type = new GunType();
        type.read(new TypeFile("testGun", EnumType.GUN, PACK, List.of(lines)));
        return type;
    }
}
