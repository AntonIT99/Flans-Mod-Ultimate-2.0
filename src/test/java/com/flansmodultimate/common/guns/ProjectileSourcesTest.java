package com.flansmodultimate.common.guns;

import com.flansmodultimate.api.ProjectileSources;
import com.flansmodultimate.api.ProjectileSources.Source;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.GrenadeType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileSourcesTest
{
    @Test
    void disablingWeaponStatsLeavesFallbacksEditableButKeepsKineticAmmoAuthoritative()
    {
        TestBullet ammo = new TestBullet(0F, 0F);
        GunType gun = new GunType();
        var defaults = sources(ammo, gun, false);
        assertEquals(Source.CALLER, defaults.damage());
        assertEquals(Source.CALLER, defaults.spread());
        assertEquals(Source.CALLER, defaults.speed());
        var weapon = sources(ammo, gun, true);
        assertEquals(Source.WEAPON, weapon.damage());
        assertEquals(Source.WEAPON, weapon.spread());
        assertEquals(Source.WEAPON, weapon.speed());
        var kinetic = sources(new TestBullet(9F, 20F), gun, false);
        assertEquals(Source.AMMUNITION, kinetic.damage());
        assertEquals(Source.CALLER, kinetic.spread());
        assertEquals(Source.AMMUNITION, kinetic.speed());
    }

    @Test
    void aaSpeedAndUnarmedStatsRemainFallbacksWhenAmmunitionDeclaresNoVelocity()
    {
        TestBullet ammo = new TestBullet(0F, 0F);
        var aa = sources(ammo, new AAGunType(), true);
        assertEquals(Source.WEAPON, aa.damage());
        assertEquals(Source.WEAPON, aa.spread());
        assertEquals(Source.CALLER, aa.speed());
        var unarmed = sources(ammo, ammo, true);
        assertEquals(Source.CALLER, unarmed.damage());
        assertEquals(Source.CALLER, unarmed.spread());
        assertEquals(Source.CALLER, unarmed.speed());
    }

    @Test
    void grenadesAlwaysUseNativeDamageAndSpeedAndOnlyWeaponSpreadCanReplaceAccuracy()
    {
        GrenadeType grenade = new GrenadeType();
        var unarmed = ExternalProjectileSupport.settingSources(grenade, grenade, null, null, false, true);
        assertEquals(Source.AMMUNITION, unarmed.damage());
        assertEquals(Source.CALLER, unarmed.spread());
        assertEquals(Source.AMMUNITION, unarmed.speed());
        assertTrue(unarmed.firingSound());
        var launcher = ExternalProjectileSupport.settingSources(grenade, new GunType(), null, null, false, true);
        assertEquals(Source.WEAPON, launcher.spread());
    }

    @Test
    void mixedBeltsKeepFallbacksEditableIfAnyRoundStillUsesThem()
    {
        TestBullet belt = new TestBullet(0F, 0F);
        belt.round(2, 9F, 20F);
        belt.round(1, 0F, 0F);
        var fallback = sources(belt, belt, true);
        assertEquals(Source.CALLER, fallback.damage());
        assertEquals(Source.CALLER, fallback.speed());
        var gun = sources(belt, new GunType(), true);
        assertEquals(Source.WEAPON, gun.damage());
        assertEquals(Source.WEAPON, gun.speed());
        TestBullet nativeBelt = new TestBullet(0F, 0F);
        nativeBelt.round(2, 9F, 20F);
        nativeBelt.round(1, 10F, 25F);
        assertEquals(Source.AMMUNITION, sources(nativeBelt, nativeBelt, false).damage());
        assertEquals(Source.AMMUNITION, sources(nativeBelt, nativeBelt, false).speed());
    }

    @Test
    void platformAmmoOverridesAndTheSelectedBankControlEditorAuthority()
    {
        TestBullet ammo = new TestBullet(0F, 0F);
        TestVehicle vehicle = new TestVehicle();
        var primary = ExternalProjectileSupport.settingSources(ammo, vehicle, null, null, false, true);
        var secondary = ExternalProjectileSupport.settingSources(ammo, vehicle, null, null, true, true);
        assertEquals(Source.CALLER, primary.speed());
        assertTrue(primary.firingSound());
        assertFalse(secondary.firingSound());
        vehicle.overrides("AmmoMass synthetic 9", "AmmoMuzzleVelocity synthetic 400");
        var mounted = ExternalProjectileSupport.settingSources(ammo, new GunType(), null, vehicle, false, false);
        assertEquals(Source.AMMUNITION, mounted.damage());
        assertEquals(Source.AMMUNITION, mounted.speed());
        assertEquals(Source.CALLER, mounted.spread());
    }

    @Test
    void replacementBeltVelocityFallbacksCheckAllOriginalBeltPhases()
    {
        TestBullet original = new TestBullet(0F, 0F);
        original.round(1, 9F, 20F);
        original.round(1, 9F, 0F);
        TestVehicle platform = new TestVehicle();
        platform.overrides("AddRoundForAmmo synthetic Fallback 1 9 0 0 0", "AddRoundForAmmo synthetic Fixed 1 9 0 400 0");
        assertEquals(Source.AMMUNITION, ExternalProjectileSupport.settingSources(original, platform, null, null, false, false).speed());
        // A period of three advances relative to the original two-round belt, exposing a later zero-velocity phase.
        platform.overrides("AddRoundForAmmo synthetic Fallback 1 9 0 0 0", "AddRoundForAmmo synthetic Fixed 2 9 0 400 0");
        assertEquals(Source.CALLER, ExternalProjectileSupport.settingSources(original, platform, null, null, false, false).speed());
    }

    private static ProjectileSources sources(BulletType ammo, InfoType weapon, boolean stats)
    {
        return ExternalProjectileSupport.settingSources(ammo, weapon, null, null, false, stats);
    }

    private static final class TestBullet extends BulletType
    {
        TestBullet(float mass, float speed)
        {
            originalShortName = "synthetic";
            this.mass = mass;
            bulletSpeed = speed;
        }

        void round(int count, float mass, float speed)
        {
            period.add(new RoundEntry("test", count, new RoundStats(mass, 0F, speed, 0F)));
            periodLength += count;
            roundsPerItem = Math.max(2, periodLength);
        }
    }

    private static final class TestVehicle extends VehicleType
    {
        TestVehicle()
        {
            bulletSpeed = 0F;
            shootSoundPrimary = "test:fire";
        }

        void overrides(String... lines)
        {
            ammoOverrides = AmmoOverrides.read(new TypeFile("syntheticPlatform", EnumType.VEHICLE, new ContentPack("test", Path.of("build", "test-packs", "sources")), List.of(lines))).overrides();
        }
    }
}
