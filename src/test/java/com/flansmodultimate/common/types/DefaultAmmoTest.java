package com.flansmodultimate.common.types;

import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAmmoTest
{
    private static final IContentProvider LOCAL = new ContentPack("defaultLocal", Path.of("build", "test-packs", "defaultLocal"));
    private static final IContentProvider OTHER = new ContentPack("defaultOther", Path.of("build", "test-packs", "defaultOther"));

    @Test
    void gunsAndAaGunsPreferLaterLocalAmmoOverEarlierForeignAmmo()
    {
        BulletType foreign = bullet("defaultForeign", OTHER);
        BulletType local = bullet("defaultLocal", LOCAL);
        BulletType laterLocal = bullet("defaultLaterLocal", LOCAL);
        List<String> lines = List.of("ShortName defaultWeapon", "Ammo defaultForeign", "Ammo defaultLocal", "Ammo defaultLaterLocal");
        GunType gun = new GunType();
        gun.contentPack = LOCAL;
        gun.read(new TypeFile("defaultGun", EnumType.GUN, LOCAL, lines));
        AAGunType aaGun = new AAGunType();
        aaGun.contentPack = LOCAL;
        aaGun.read(new TypeFile("defaultAa", EnumType.AA_GUN, LOCAL, lines));

        assertEquals(List.of(foreign, local, laterLocal), gun.getAmmoTypes());
        assertSame(local, gun.getDefaultAmmo().orElseThrow());
        assertSame(local, aaGun.getDefaultAmmo().orElseThrow());
    }

    @Test
    void missingFirstEntryFallsBackToCompatibleForeignAmmo()
    {
        BulletType foreign = bullet("defaultFallback", OTHER);
        GunType gun = new GunType();
        gun.contentPack = LOCAL;
        gun.read(new TypeFile("defaultFallbackGun", EnumType.GUN, LOCAL, List.of("ShortName defaultFallbackGun", "Ammo defaultMissing", "Ammo defaultFallback")));

        assertSame(foreign, gun.getDefaultAmmo().orElseThrow());
        assertTrue(ShootableType.findDefaultAmmo(List.of(), LOCAL).isEmpty());
    }

    private static BulletType bullet(String shortName, IContentProvider pack)
    {
        BulletType bullet = new BulletType();
        bullet.contentPack = pack;
        bullet.read(new TypeFile(shortName, EnumType.BULLET, pack, List.of("ShortName " + shortName)));
        bullet.onItemRegistration(shortName.toLowerCase(java.util.Locale.ROOT));
        return bullet;
    }
}
