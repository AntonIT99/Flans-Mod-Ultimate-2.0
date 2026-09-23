package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.armor.ResolvedVehicleArmor;
import com.flansmodultimate.common.driveables.armor.VehicleArmorResolver;
import com.flansmodultimate.common.driveables.armor.VehicleArmorSpec;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DriveableProjectileCollisionTest
{
    private static final AABB TURRET_BOX = new AABB(-1D, -1D, -4D, 1D, 1D, -3D);

    @Test
    void turretFrontTracksYawZeroNinetyAndOneEighty()
    {
        assertFront(0F, new Vec3(0D, 0D, -10D), new Vec3(0D, 0D, 20D));
        assertFront(90F, new Vec3(-10D, 0D, 0D), new Vec3(20D, 0D, 0D));
        assertFront(180F, new Vec3(0D, 0D, 10D), new Vec3(0D, 0D, -20D));
    }

    @Test
    void turretSideAlsoRotatesRatherThanRemainingAttachedToHullForward()
    {
        DriveableProjectileCollision.LocalHit hit = DriveableProjectileCollision.trace(TURRET_BOX,
            new Vec3(-3.5D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumDriveablePart.TURRET,
            90F, 0F, Vec3.ZERO, Vec3.ZERO);
        assertNotNull(hit);
        assertEquals(EnumArmorFacing.RIGHT, hit.facing());
        assertEquals(1D, hit.outwardNormal().x, 1.0E-9D);
    }

    @Test
    void rotatedCollisionSelectsTheTurretPartAndItsOwnSemanticArmour()
    {
        AABB separatedHull = new AABB(-1D, -4D, -1D, 1D, -3D, 1D);
        EnumMap<EnumArmorFacing, ArmorPlate> hull = new EnumMap<>(EnumArmorFacing.class);
        hull.put(EnumArmorFacing.FRONT, new ArmorPlate(20F, 0F));
        EnumMap<EnumArmorFacing, ArmorPlate> turret = new EnumMap<>(EnumArmorFacing.class);
        turret.put(EnumArmorFacing.FRONT, new ArmorPlate(120F, 0F));
        turret.put(EnumArmorFacing.RIGHT, new ArmorPlate(40F, 0F));
        ResolvedVehicleArmor armor = VehicleArmorResolver.resolve(
            new VehicleArmorSpec(hull, turret, Map.of()),
            List.of(EnumDriveablePart.CORE, EnumDriveablePart.TURRET));

        assertTurretSelection(separatedHull, armor, 0F,
            new Vec3(0D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumArmorFacing.FRONT, 120F);
        assertTurretSelection(separatedHull, armor, 90F,
            new Vec3(-10D, 0D, 0D), new Vec3(20D, 0D, 0D), EnumArmorFacing.FRONT, 120F);
        assertTurretSelection(separatedHull, armor, 180F,
            new Vec3(0D, 0D, 10D), new Vec3(0D, 0D, -20D), EnumArmorFacing.FRONT, 120F);
        assertTurretSelection(separatedHull, armor, 90F,
            new Vec3(-3.5D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumArmorFacing.RIGHT, 40F);
    }

    @Test
    void barrelPitchUsesTheDamageableBarrelCollisionFrame()
    {
        DriveableProjectileCollision.LocalHit hit = DriveableProjectileCollision.trace(TURRET_BOX,
            new Vec3(0D, -10D, 0D), new Vec3(0D, 20D, 0D), EnumDriveablePart.BARREL,
            0F, 90F, Vec3.ZERO, Vec3.ZERO);
        assertNotNull(hit);
        assertEquals(EnumArmorFacing.FRONT, hit.facing());
    }

    @Test
    void hullGeometryIgnoresTurretAngles()
    {
        DriveableProjectileCollision.LocalHit hit = DriveableProjectileCollision.trace(TURRET_BOX,
            new Vec3(0D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumDriveablePart.CORE,
            90F, 45F, Vec3.ZERO, Vec3.ZERO);
        assertNotNull(hit);
        assertEquals(EnumArmorFacing.FRONT, hit.facing());
    }

    @Test
    void fractionsRemainComparableAcrossHullAndTurretTransforms()
    {
        AABB hull = new AABB(-1D, -1D, -6D, 1D, 1D, -5D);
        DriveableProjectileCollision.LocalHit hullHit = DriveableProjectileCollision.trace(hull,
            new Vec3(0D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumDriveablePart.CORE,
            0F, 0F, Vec3.ZERO, Vec3.ZERO);
        DriveableProjectileCollision.LocalHit turretHit = DriveableProjectileCollision.trace(TURRET_BOX,
            new Vec3(0D, 0D, -10D), new Vec3(0D, 0D, 20D), EnumDriveablePart.TURRET,
            0F, 0F, Vec3.ZERO, Vec3.ZERO);
        assertNotNull(hullHit);
        assertNotNull(turretHit);
        assertTrue(hullHit.fraction() < turretHit.fraction());
    }

    private static void assertFront(float yaw, Vec3 origin, Vec3 motion)
    {
        DriveableProjectileCollision.LocalHit hit = DriveableProjectileCollision.trace(TURRET_BOX,
            origin, motion, EnumDriveablePart.TURRET, yaw, 0F, Vec3.ZERO, Vec3.ZERO);
        assertNotNull(hit);
        assertEquals(EnumArmorFacing.FRONT, hit.facing());
        assertEquals(-1D, hit.outwardNormal().z, 1.0E-9D);
    }

    private static void assertTurretSelection(AABB hull, ResolvedVehicleArmor armor, float yaw,
                                              Vec3 origin, Vec3 motion, EnumArmorFacing facing,
                                              float expectedThicknessMm)
    {
        DriveableProjectileCollision.LocalHit hullHit = DriveableProjectileCollision.trace(hull,
            origin, motion, EnumDriveablePart.CORE, yaw, 0F, Vec3.ZERO, Vec3.ZERO);
        DriveableProjectileCollision.LocalHit turretHit = DriveableProjectileCollision.trace(TURRET_BOX,
            origin, motion, EnumDriveablePart.TURRET, yaw, 0F, Vec3.ZERO, Vec3.ZERO);
        assertNull(hullHit);
        assertNotNull(turretHit);
        assertEquals(facing, turretHit.facing());
        assertEquals(expectedThicknessMm,
            armor.plate(EnumDriveablePart.TURRET, turretHit.facing()).authored().thicknessMm(), 1.0E-6F);
    }
}
