package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.flansmodultimate.api.projectiles.ProjectileParameters;
import com.flansmodultimate.common.driveables.*;
import com.flansmodultimate.common.driveables.weapons.ShootPoint;
import com.flansmodultimate.common.entity.geometry.AAGunBarrelGeometry;
import com.flansmodultimate.common.types.*;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalProjectileSupportTest
{
    @Test
    void vehicleAndAircraftMuzzlesUseTheSameFrameAsTheirStaticModels()
    {
        Vector3f point = new Vector3f(4F, 2F, -1F);
        Vec3 vehicle = ExternalProjectileSupport.getMuzzles(new TestVehicle(point), false, 0, true).get(0).offset();
        Vec3 plane = ExternalProjectileSupport.getMuzzles(new TestPlane(point), false, 0, true).get(0).offset();
        Vec3 vehicleLocal = LegacyDriveableCoordinates.toLocal(point);
        vehicleLocal = new Vec3(-vehicleLocal.x, vehicleLocal.y, vehicleLocal.z);
        Vec3 planeLocal = LegacyDriveableCoordinates.applyPlaneModelFacing(LegacyDriveableCoordinates.toLocal(point));
        planeLocal = new Vec3(-planeLocal.x, planeLocal.y, planeLocal.z);
        assertVector(LegacyDriveableCoordinates.modelLocalToWorldDirection(vehicleLocal, 90F, 0F, 0F), vehicle);
        assertVector(LegacyDriveableCoordinates.modelLocalToWorldDirection(planeLocal, 270F, 0F, 0F), plane);
    }

    @Test
    void authoredAaBarrelsAlternateAndTheirMeasuredFallbackIsUsedWhenAvailable()
    {
        TestAA type = new TestAA();
        List<WeaponMuzzle> first = ExternalProjectileSupport.getMuzzles(type, false, 0, true);
        List<WeaponMuzzle> second = ExternalProjectileSupport.getMuzzles(type, false, 1, true);
        assertEquals(1, first.size());
        assertVector(AAGunBarrelGeometry.legacyBarrelOffset(16D, 8D, 4D, 180F, 0F), first.get(0).offset());
        assertVector(AAGunBarrelGeometry.legacyBarrelOffset(16D, 8D, -4D, 180F, 0F), second.get(0).offset());
        assertEquals(2, ExternalProjectileSupport.getMuzzles(type, false, 0, false).size());
        type.setMeasuredBarrels(new Vec3[]{new Vec3(0D, 8D, 0D), new Vec3(0D, 8D, 0D)}, new Vec3[]{new Vec3(20D, 0D, 4D), new Vec3(20D, 0D, -4D)});
        assertVector(AAGunBarrelGeometry.modelBarrelOffset(new Vec3(0D, 8D, 0D), new Vec3(20D, 0D, 4D), 180F, 0F), ExternalProjectileSupport.getMuzzles(type, false, 0, true).get(0).offset());
    }

    @Test
    void turretMuzzlesTurnAndElevateExactlyAsTheDriveableEntityCarriesThem()
    {
        Vector3f muzzle = new Vector3f(4F, 2F, 0.5F);
        Vector3f origin = new Vector3f(1F, 1.5F, 0F);
        TestVehicle type = new TestVehicle(new Vector3f());
        type.turret(origin, muzzle);
        MountAim aim = new MountAim(35F, -12F);
        Vec3 aimed = ExternalProjectileSupport.getMuzzles(type, false, 0, true, aim).get(0).offset();
        // The entity works in its mirrored attachment frame: pitch round the pivot, then yaw, then out to the world.
        Vec3 point = mirrored(muzzle);
        Vec3 pivot = mirrored(origin);
        Vec3 turned = LegacyDriveableCoordinates.rotateTurretLocal(point.subtract(pivot), aim.yaw(), aim.pitch()).add(pivot);
        assertVector(LegacyDriveableCoordinates.modelLocalToWorldDirection(turned, 90F, 0F, 0F), aimed);
        // A turn to the right moves a muzzle ahead of the pivot to the right, and elevation lifts it.
        Vec3 rest = ExternalProjectileSupport.getMuzzles(type, false, 0, true).get(0).offset();
        assertVector(rest, ExternalProjectileSupport.getMuzzles(type, false, 0, true, MountAim.REST).get(0).offset());
        assertEquals(true, ExternalProjectileSupport.getMuzzles(type, false, 0, true, new MountAim(35F, 0F)).get(0).offset().x > rest.x);
        assertEquals(true, ExternalProjectileSupport.getMuzzles(type, false, 0, true, new MountAim(0F, -12F)).get(0).offset().y > rest.y);
        // Muzzles on the hull stay where they are, whatever the turret does.
        assertVector(ExternalProjectileSupport.getMuzzles(type, true, 0, true).get(0).offset(), ExternalProjectileSupport.getMuzzles(type, true, 0, true, aim).get(0).offset());
    }

    @Test
    void anAaGunCarriesItsBarrelsRoundAndUpWithItsAim()
    {
        TestAA type = new TestAA();
        MountAim aim = new MountAim(40F, -25F);
        assertVector(AAGunBarrelGeometry.legacyBarrelOffset(16D, 8D, 4D, 220F, -25F), ExternalProjectileSupport.getMuzzles(type, false, 0, true, aim).get(0).offset());
        type.setMeasuredBarrels(new Vec3[]{new Vec3(0D, 8D, 0D), new Vec3(0D, 8D, 0D)}, new Vec3[]{new Vec3(20D, 0D, 4D), new Vec3(20D, 0D, -4D)});
        Vec3 rest = ExternalProjectileSupport.getMuzzles(type, false, 0, true).get(0).offset();
        Vec3 aimed = ExternalProjectileSupport.getMuzzles(type, false, 0, true, aim).get(0).offset();
        assertVector(AAGunBarrelGeometry.modelBarrelOffset(new Vec3(0D, 8D, 0D), new Vec3(20D, 0D, 4D), 220F, -25F), aimed);
        assertEquals(true, aimed.y > rest.y);
    }

    private static Vec3 mirrored(Vector3f legacy)
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(legacy);
        return new Vec3(-local.x, local.y, local.z);
    }

    @Test
    void primaryAndSecondaryBanksKeepTheirOwnMuzzlesAndParticles()
    {
        TestVehicle type = new TestVehicle(new Vector3f(4F, 2F, -1F));
        List<WeaponMuzzle> primary = ExternalProjectileSupport.getMuzzles(type, false, 0, true);
        List<WeaponMuzzle> secondary = ExternalProjectileSupport.getMuzzles(type, true, 0, true);
        assertEquals(new Vec3(2D, 1D, -3D), secondary.get(0).offset());
        assertEquals("smoke", secondary.get(0).particles().get(0).name());
        assertEquals(0, primary.get(0).particles().size());
        assertThrows(UnsupportedOperationException.class, secondary::clear);
        List<WeaponMuzzle.Particle> particles = secondary.get(0).particles();
        assertThrows(UnsupportedOperationException.class, particles::clear);
    }

    @Test
    void mountedGunsKeepBankDamageModifiersUnlessTheVehicleUsesPureGunStatistics()
    {
        TestVehicle platform = new TestVehicle(new Vector3f());
        TestGun gun = new TestGun();
        var nativeStats = new ProjectileParameters(0, true, 99F, 88F, 77F);
        FireableGun primary = ExternalProjectileSupport.resolveWeapon(gun, null, null, platform, false, nativeStats);
        FireableGun secondary = ExternalProjectileSupport.resolveWeapon(gun, null, null, platform, true, nativeStats);
        assertEquals(10F, primary.getDamage());
        assertEquals(15F, secondary.getDamage());
        platform.usePureGunStats();
        assertEquals(5F, ExternalProjectileSupport.resolveWeapon(gun, null, null, platform, false, nativeStats).getDamage());
        var npcStats = new ProjectileParameters(0, false, 99F, 88F, 77F);
        FireableGun fallback = ExternalProjectileSupport.resolveWeapon(gun, null, null, platform, false, npcStats);
        assertEquals(99F, fallback.getDamage());
        assertEquals(88F, fallback.getSpread());
        assertEquals(77F, fallback.getBulletSpeed());
    }

    @Test
    void muzzleFlashAvailabilityUsesGunModelsOrConfiguredGunAndShellFallbacks()
    {
        TestVehicle platform = new TestVehicle(new Vector3f());
        TestGun gun = new TestGun();
        WeaponMuzzle bare = new WeaponMuzzle(Vec3.ZERO, Optional.empty(), List.of());
        WeaponMuzzle mounted = new WeaponMuzzle(Vec3.ZERO, Optional.of(gun), List.of());
        assertFalse(FlansProjectiles.hasMuzzleFlash(platform, mounted, false));
        gun.flash("DefaultFlash");
        assertTrue(FlansProjectiles.hasMuzzleFlash(platform, mounted, false));
        gun.flash("");
        platform.flashes();
        assertTrue(FlansProjectiles.hasMuzzleFlash(platform, bare, false));
        assertFalse(FlansProjectiles.hasMuzzleFlash(platform, bare, true));
        assertTrue(FlansProjectiles.hasMuzzleFlash(platform, mounted, true));
    }

    private static ShootPoint point(Vector3f position)
    {
        return new ShootPoint(new DriveablePosition(position, EnumDriveablePart.CORE), new Vector3f());
    }

    private static void assertVector(Vec3 expected, Vec3 actual)
    {
        assertEquals(expected.x, actual.x, 1.0E-6D);
        assertEquals(expected.y, actual.y, 1.0E-6D);
        assertEquals(expected.z, actual.z, 1.0E-6D);
    }

    private static final class TestVehicle extends VehicleType
    {
        TestVehicle(Vector3f position)
        {
            damageMultiplierPrimary = 2F;
            damageMultiplierSecondary = 3F;
            shootPointsPrimary.add(point(position));
            shootPointsSecondary.add(point(new Vector3f(3F, 1F, 2F)));
            shootParticlesSecondary.add(new ShootParticle("smoke", 0.1F, 0F, 0F));
        }

        private void flashes()
        {
            defaultMuzzleFlash = true;
            primary = EnumWeaponType.SHELL;
            secondary = EnumWeaponType.MISSILE;
        }

        private void usePureGunStats()
        {
            readWeaponsFromGunTypes = true;
        }

        /** Replaces the primary point by one on the turret. */
        private void turret(Vector3f origin, Vector3f muzzle)
        {
            turretOrigin = origin;
            shootPointsPrimary.clear();
            shootPointsPrimary.add(new ShootPoint(new DriveablePosition(muzzle, EnumDriveablePart.TURRET), new Vector3f()));
        }
    }

    private static final class TestPlane extends PlaneType
    {
        TestPlane(Vector3f position)
        {
            shootPointsPrimary.add(point(position));
        }
    }

    private static final class TestAA extends AAGunType
    {
        TestAA()
        {
            numBarrels = 2;
            fireAlternately = true;
            barrelX = new float[]{16F, 16F};
            barrelY = new float[]{8F, 8F};
            barrelZ = new float[]{4F, -4F};
        }
    }

    private static final class TestGun extends GunType
    {
        TestGun()
        {
            damage = 5F;
        }

        private void flash(String model)
        {
            flashModelName = model;
        }

        // Pin neutral config modifiers without starting a Minecraft registry or server.
        @Override
        public float getDamage(ItemStack stack)
        {
            return damage;
        }

        @Override
        public float getSpread(ItemStack stack, EnumMovement movement, boolean airborne)
        {
            return bulletSpread;
        }
    }
}
