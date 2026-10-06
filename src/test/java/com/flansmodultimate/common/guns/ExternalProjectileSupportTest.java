package com.flansmodultimate.common.guns;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.api.ProjectileParameters;
import com.flansmodultimate.api.WeaponMuzzle;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.driveables.ShootPoint;
import com.flansmodultimate.common.entity.AAGunBarrelGeometry;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.EnumMovement;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertVector(AAGunBarrelGeometry.modelBarrelOffset(new Vec3(0D, 8D, 0D), new Vec3(20D, 0D, 4D), 180F, 0F),
            ExternalProjectileSupport.getMuzzles(type, false, 0, true).get(0).offset());
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

        private void usePureGunStats()
        {
            readWeaponsFromGunTypes = true;
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
