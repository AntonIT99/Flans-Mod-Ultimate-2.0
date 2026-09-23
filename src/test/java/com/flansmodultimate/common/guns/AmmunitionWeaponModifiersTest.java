package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What loading a round does to the weapon firing it. These scale the weapon's
 * own numbers the way an attachment does, so the same round behaves identically
 * out of a held gun, an AA gun and a vehicle mount.
 */
class AmmunitionWeaponModifiersTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "ammo-modifiers"));
    private static final float EPSILON = 1.0E-4F;

    @Test
    void aRoundScalesTheWeaponsDamageAndSpread()
    {
        BulletType round = bullet("DamageMultiplier 1.5", "SpreadMultiplier 0.5");
        FireableGun gun = gun(round, 10F, 4F);

        gun.applyAmmunition(round);

        assertEquals(15F, gun.getDamage(), EPSILON);
        assertEquals(2F, gun.getSpread(), EPSILON);
    }

    @Test
    void aRoundDeclaringNoModifiersLeavesTheWeaponAlone()
    {
        BulletType round = bullet("Mass 9");
        FireableGun gun = gun(round, 10F, 4F);

        gun.applyAmmunition(round);

        assertEquals(10F, gun.getDamage(), EPSILON);
        assertEquals(4F, gun.getSpread(), EPSILON);
        assertEquals(1F, round.getRecoilMultiplier(), EPSILON);
        assertEquals(1F, round.getReloadTimeMultiplier(), EPSILON);
    }

    @Test
    void modifiersStackOnTopOfWhateverTheWeaponAlreadyResolved()
    {
        // The weapon arrives with its attachments already folded in, so the
        // round multiplies that rather than the raw authored value.
        BulletType round = bullet("DamageMultiplier 2");
        FireableGun gun = gun(round, 10F, 4F);
        gun.multiplyDamage(0.5F);

        gun.applyAmmunition(round);

        assertEquals(10F, gun.getDamage(), EPSILON);
    }

    @Test
    void aNonPositiveFactorIsRejectedRatherThanSilencingTheStat()
    {
        // Zero or negative would zero out or invert damage, which is never what
        // an author means by a multiplier, so the default stands.
        BulletType round = bullet("DamageMultiplier 0", "SpreadMultiplier -2", "RecoilMultiplier 0", "ReloadTimeMultiplier -1");

        assertEquals(1F, round.getDamageMultiplier(), EPSILON);
        assertEquals(1F, round.getSpreadMultiplier(), EPSILON);
        assertEquals(1F, round.getRecoilMultiplier(), EPSILON);
        assertEquals(1F, round.getReloadTimeMultiplier(), EPSILON);
    }

    @Test
    void applyingAmmunitionIsWhatBakesTheModifiersIn()
    {
        // A projectile saves the resolved weapon values and rebuilds them on
        // load, so applying twice would compound. This pins that the modifiers
        // live in applyAmmunition and not in the FiredShot constructor.
        BulletType round = bullet("DamageMultiplier 2");
        FireableGun gun = gun(round, 10F, 4F);

        FiredShot shot = new FiredShot(gun, round, null, null, 0);

        assertEquals(10F, shot.getFireableGun().getDamage(), EPSILON,
            "the general constructor must not apply modifiers of its own");
    }

    @Test
    void aRoundThatDictatesItsOwnSpreadStillTakesItsSpreadMultiplier()
    {
        BulletType round = bullet("Spread 8", "SpreadMultiplier 0.25");
        assertEquals(8F, round.getBulletSpread(), EPSILON);
        assertEquals(0.25F, round.getSpreadMultiplier(), EPSILON);
    }

    // ------------------------------------------------------------- fixtures

    private static FireableGun gun(BulletType type, float damage, float spread)
    {
        return new FireableGun(type, damage, spread, 3F, EnumSpreadPattern.CIRCLE);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticRound", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
