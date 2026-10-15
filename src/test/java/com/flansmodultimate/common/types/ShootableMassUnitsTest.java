package com.flansmodultimate.common.types;

import com.flansmodultimate.common.guns.FiredShot;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two authoring scales of the same stored stat. Projectile mass is stored in grams and
 * accepts {@code Mass} or {@code MassKg}; explosive mass is stored in kg TNT equivalent and
 * accepts {@code ExplosiveMassTNTg} or {@code ExplosiveMassTNTKg}.
 */
class ShootableMassUnitsTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "units"));

    @Test
    void massIsAuthoredInGrams()
    {
        assertEquals(6800F, bullet("Mass 6800").getMass());
    }

    @Test
    void massKgIsConvertedToGrams()
    {
        assertEquals(6800F, bullet("MassKg 6.8").getMass(), 1.0E-3F);
    }

    @Test
    void explosiveMassInGramsIsStoredInKilograms()
    {
        assertEquals(0.029F, bullet("ExplosiveMassTNTg 29").getExplosiveMass(), 1.0E-6F);
    }

    @Test
    void explosiveMassInKilogramsIsStoredUnchanged()
    {
        assertEquals(17.7F, bullet("ExplosiveMassTNTKg 17.7").getExplosiveMass(), 1.0E-4F);
    }

    @Test
    void theKilogramKeyWinsWhenBothScalesAreDeclared()
    {
        assertEquals(3F, bullet("ExplosiveMassTNTg 29", "ExplosiveMassTNTKg 3").getExplosiveMass(), 1.0E-4F);
    }

    @Test
    void anAddRoundExplosiveColumnIsAuthoredInGrams()
    {
        BulletType belt = bullet("RoundsPerItem 2", "AddRound AP 1 162 0 800 45", "AddRound HE 1 135 16 835 0");

        assertEquals(0F, belt.statsForShot(0).explosiveMass());
        assertEquals(0.016F, belt.statsForShot(1).explosiveMass(), 1.0E-6F);
    }

    @Test
    void casingPeakIsIndependentOfChargeMass()
    {
        BulletType small = bullet("ExplosiveMassTNTg 60", "FragType STD_FRAG");
        BulletType large = bullet("ExplosiveMassTNTKg 5", "FragType STD_FRAG");

        assertEquals(25F, small.explosionFragDamage.getDamage(), 1.0E-3F);
        assertEquals(25F, large.explosionFragDamage.getDamage(), 1.0E-3F);
        assertTrue(large.fragRadius > small.fragRadius);
    }

    @Test
    void aBeltCasingKeepsItsPeakWhenChargeIsSuppliedByIndividualRounds()
    {
        BulletType belt = bullet("RoundsPerItem 2", "FragType HE_SHELL", "AddRound AP 1 162 0 800 45", "AddRound HE 1 135 16 835 0");

        assertEquals(32.5F, belt.explosionFragDamage.getDamage(), 1.0E-3F);
        assertEquals(0F, belt.fragRadius);
    }

    @Test
    void mixedBeltUsesEachRoundsOwnMetalMass()
    {
        BulletType belt = bullet("RoundsPerItem 2", "FragType HE_SHELL", "AddRound LightHE 1 135 16 835 0", "AddRound HeavyHE 1 250 16 835 0");
        FiredShot lightShot = new FiredShot(null, belt, null, null, 0);
        FiredShot heavyShot = new FiredShot(null, belt, null, null, 1);
        var light = belt.fragmentationFor(lightShot.getExplosiveMass(), lightShot.getProjectileMass());
        var heavy = belt.fragmentationFor(heavyShot.getExplosiveMass(), heavyShot.getProjectileMass());
        assertTrue(heavy.fragmentCount() > light.fragmentCount());
        assertEquals(light.peakDamage(), heavy.peakDamage());
    }

    @Test
    void fragmentConstructionAndDamageCanBeAuthored()
    {
        BulletType mine = bullet("ExplosiveMassTNTg 182", "FragType PREFORMED", "FragPattern HORIZONTAL_BAND", "FragCount 350", "FragMetalMassg 350", "FragBurstHeight 1",
            "FragDamage 29", "FragDamageVsPlayer 31");
        assertTrue(mine.fragRadius > 30F);
        assertEquals(1F, mine.getFragBurstHeight(), 1.0E-3F);
        var fragments = mine.fragmentationFor(mine.getExplosiveMass(), mine.getMass());
        assertEquals(350D, fragments.fragmentCount(), 1.0E-3D);
        assertEquals(com.flansmodultimate.common.explosions.FragmentationModel.Pattern.HORIZONTAL_BAND, fragments.pattern());
        assertEquals(29F, mine.explosionFragDamage.getDamage(), 1.0E-3F);
        assertEquals(31F, mine.explosionFragDamage.getDamageVsPlayer(), 1.0E-3F);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
