package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulletTypeAmmoModelTest
{
    private static final IContentProvider PACK = new ContentPack("test", Path.of("build", "test-packs", "ammomodels"));

    private static BulletType bullet(String... lines)
    {
        BulletType type = new BulletType();
        type.read(new TypeFile("testBullet", EnumType.BULLET, PACK, List.of(lines)));
        return type;
    }

    private static GunType gun(String shortName)
    {
        GunType type = new GunType();
        type.read(new TypeFile(shortName, EnumType.GUN, PACK, List.of("ShortName " + shortName)));
        return type;
    }

    @Test
    void picksTheVariantNamedForTheGunIgnoringCase()
    {
        BulletType bullet = bullet("ShortName testBullet", "AmmoModelType Kar98k 2", "AmmoModelType mp40 3");

        assertEquals(2, bullet.getAmmoModelTypeForGun(gun("kar98k")));
        assertEquals(3, bullet.getAmmoModelTypeForGun(gun("MP40")));
    }

    @Test
    void defaultsToThePlainAmmoModelAndClampsVariants()
    {
        BulletType bullet = bullet("ShortName testBullet", "AmmoModelType Kar98k 42", "AmmoModelType mp40 many");

        assertEquals(10, bullet.getAmmoModelTypeForGun(gun("Kar98k")));
        assertEquals(1, bullet.getAmmoModelTypeForGun(gun("mp40")));
        assertEquals(1, bullet.getAmmoModelTypeForGun(gun("m16a1")));
    }
}
