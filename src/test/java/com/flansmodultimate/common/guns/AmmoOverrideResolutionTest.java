package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Field-by-field precedence of a per-weapon override against the ammunition's own
 * statistics. Highest first: the scalar override, then a round from
 * {@code AddRoundForAmmo}, then the ammunition's own {@code AddRound} belt, then the
 * ammunition's own top-level value.
 */
class AmmoOverrideResolutionTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "ammo"));

    @Test
    void anEmptyOverrideChangesNothing()
    {
        BulletType bullet = bullet("Mass 6800", "MuzzleVelocity 770", "ExplosiveMass 0.029",
            "PenetrationAt100m 143");

        assertEquals(6800F, AmmoOverride.EMPTY.resolveMass(bullet, 0));
        assertEquals(38.5F, AmmoOverride.EMPTY.resolveBulletSpeed(bullet, 0, 0F), 1.0E-4F);
        assertEquals(0.029F, AmmoOverride.EMPTY.resolveExplosiveMass(bullet, 0), 1.0E-6F);
        assertEquals(143F, AmmoOverride.EMPTY.resolvePenetrationAt100m(bullet, 0));
    }

    @Test
    void aScalarOverrideReplacesTheAmmunitionValue()
    {
        BulletType bullet = bullet("Mass 6800", "MuzzleVelocity 770", "ExplosiveMass 0.029",
            "PenetrationAt100m 143");
        AmmoOverride override = overrides(
            "AmmoMass shell 4100",
            "AmmoMuzzleVelocity shell 990",
            "AmmoExplosiveMass shell 0",
            "AmmoPenetrationAt100m shell 177");

        assertEquals(4100F, override.resolveMass(bullet, 0));
        assertEquals(49.5F, override.resolveBulletSpeed(bullet, 0, 0F), 1.0E-4F);
        assertEquals(0F, override.resolveExplosiveMass(bullet, 0));
        assertEquals(177F, override.resolvePenetrationAt100m(bullet, 0));
    }

    @Test
    void anUnsetFieldFallsThroughToTheAmmunition()
    {
        BulletType bullet = bullet("Mass 6800", "MuzzleVelocity 770", "PenetrationAt100m 143");
        AmmoOverride override = overrides("AmmoPenetrationAt100m shell 188");

        assertEquals(6800F, override.resolveMass(bullet, 0), "mass was not overridden");
        assertEquals(38.5F, override.resolveBulletSpeed(bullet, 0, 0F), 1.0E-4F);
        assertEquals(188F, override.resolvePenetrationAt100m(bullet, 0));
    }

    @Test
    void aReplacementBeltDisplacesTheAmmunitionsOwnBelt()
    {
        BulletType bullet = bullet("RoundsPerItem 2",
            "AddRound AP 1 162 0 800 45",
            "AddRound HE 1 135 0.016 835 0");
        assertTrue(bullet.hasDifferentRounds());
        assertEquals(162F, bullet.getMass(0));

        AmmoOverride override = overrides(
            "AddRoundForAmmo belt Ball 1 46 0 900 12");

        assertEquals(46F, override.resolveMass(bullet, 0));
        assertEquals(46F, override.resolveMass(bullet, 1),
            "the ammunition's own second round must not show through");
        assertEquals(45F, override.resolveBulletSpeed(bullet, 1, 0F), 1.0E-4F);
        assertEquals(0F, override.resolveExplosiveMass(bullet, 1));
        assertEquals(12F, override.resolvePenetrationAt100m(bullet, 1));
    }

    @Test
    void aScalarOverrideBeatsItsOwnReplacementBelt()
    {
        BulletType bullet = bullet("Mass 100");
        AmmoOverride override = overrides(
            "AddRoundForAmmo belt AP 1 162 0 800 45",
            "AmmoMass belt 999");

        assertEquals(999F, override.resolveMass(bullet, 0));
        assertEquals(45F, override.resolvePenetrationAt100m(bullet, 0),
            "the belt still supplies the fields the scalar override leaves alone");
    }

    @Test
    void theWeaponVelocityRemainsTheLastFallback()
    {
        BulletType bullet = bullet("Mass 9");
        assertEquals(7F, AmmoOverride.EMPTY.resolveBulletSpeed(bullet, 0, 7F), 1.0E-4F,
            "an ammunition with no velocity of its own keeps taking the weapon's");

        AmmoOverride override = overrides("AmmoMuzzleVelocity round 400");
        assertEquals(20F, override.resolveBulletSpeed(bullet, 0, 7F), 1.0E-4F,
            "an overridden velocity must win over the weapon's");
    }

    @Test
    void anOverriddenVelocityStillHonoursTheAmmunitionsSpeedMultiplier()
    {
        BulletType bullet = bullet("Mass 9", "BulletSpeedMultiplier 2.0");
        AmmoOverride override = overrides("AmmoMuzzleVelocity round 400");
        assertEquals(40F, override.resolveBulletSpeed(bullet, 0, 0F), 1.0E-4F);
    }

    @Test
    void anOverrideCanGiveMassToAMasslessRoundAndSoEngageKineticDamage()
    {
        BulletType bullet = bullet("Damage 5");
        assertFalse(bullet.useKineticDamageSystem());
        assertEquals(0F, AmmoOverride.EMPTY.resolveMass(bullet, 0));

        AmmoOverride override = overrides("AmmoMass laser 44");
        assertEquals(44F, override.resolveMass(bullet, 0),
            "a positive resolved mass is what puts the shot on the kinetic scale");
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static AmmoOverride overrides(String... lines)
    {
        AmmoOverrides.Result result = AmmoOverrides.read(
            new TypeFile("syntheticWeapon", EnumType.GUN, PACK, List.of(lines)));
        assertTrue(result.warnings().isEmpty(), () -> "unexpected warnings: " + result.warnings());
        assertEquals(1, result.overrides().asMap().size());
        return result.overrides().asMap().values().iterator().next();
    }
}
