package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void theLegacyExplosiveMassKeyIsNoLongerRead()
    {
        assertEquals(0F, bullet("ExplosiveMass 0.029").getExplosiveMass());
    }

    @Test
    void anAddRoundExplosiveColumnIsAuthoredInGrams()
    {
        BulletType belt = bullet("RoundsPerItem 2",
            "AddRound AP 1 162 0 800 45",
            "AddRound HE 1 135 16 835 0");

        assertEquals(0F, belt.statsForShot(0).explosiveMass());
        assertEquals(0.016F, belt.statsForShot(1).explosiveMass(), 1.0E-6F);
    }

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.load(new TypeFile("syntheticBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }
}
