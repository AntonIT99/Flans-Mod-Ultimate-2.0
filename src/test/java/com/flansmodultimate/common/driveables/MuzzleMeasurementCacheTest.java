package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.IContentProvider;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cache has to hand a restarted game exactly what measuring gave the last
 * one, and nothing when it was stored for other content.
 */
class MuzzleMeasurementCacheTest
{
    private static final IContentProvider PACK = new ContentPack("cachePack", Path.of("build", "test-packs", "cache"));

    private static VehicleType vehicle()
    {
        VehicleType vehicle = new VehicleType();
        vehicle.load(new TypeFile("cacheVehicle", EnumType.VEHICLE, PACK, List.of("ShortName cacheVehicle", "Driver 0 0 0", "BarrelPosition 10 20 0")));
        return vehicle;
    }

    private static AAGunType aaGun()
    {
        AAGunType aaGun = new AAGunType();
        aaGun.load(new TypeFile("cacheAaGun", EnumType.AA_GUN, PACK, List.of("ShortName cacheAaGun", "NumBarrels 2")));
        return aaGun;
    }

    private static GunType deployedGun()
    {
        GunType gun = new GunType();
        gun.load(new TypeFile("cacheDeployedGun", EnumType.GUN, PACK, List.of("ShortName cacheDeployedGun", "Deployable True")));
        return gun;
    }

    @Test
    void storedResultsReplayOntoFreshlyReadTypes()
    {
        MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        MuzzleMeasurementCache.recordMove(results, vehicle(), new MuzzleMeasurementCache.Move(false, false, 0, 30F, 22F, -4F));
        MuzzleMeasurementCache.recordBarrels(results, aaGun(), new Vec3[]{new Vec3(1, 2, 3), new Vec3(1, 2, -3)}, new Vec3[]{new Vec3(20, 2, 3), new Vec3(20, 2, -3)});
        MuzzleMeasurementCache.recordDeployedGunMuzzle(results, deployedGun(), new Vec3(0, 6, 0), new Vec3(0, 6, 20));

        MuzzleMeasurementCache.Results restored = MuzzleMeasurementCache.fromJson(MuzzleMeasurementCache.toJson("key", results), "key");
        VehicleType vehicle = vehicle();
        AAGunType aaGun = aaGun();
        GunType deployedGun = deployedGun();
        int moved = MuzzleMeasurementCache.apply(restored, List.<InfoType>of(vehicle, aaGun, deployedGun));

        assertEquals(1, moved);
        ShootPoint point = vehicle.shootPoints(false).get(0);
        Vector3f root = point.getRootPos().getPosition();
        assertEquals(30F, (root.x + point.getOffPos().x) * 16F, 1.0E-4F);
        assertEquals(-4F, (root.z + point.getOffPos().z) * 16F, 1.0E-4F);
        assertTrue(aaGun.hasMeasuredBarrels());
        assertEquals(new Vec3(20, 2, -3), aaGun.getMeasuredBarrelMuzzles()[1]);
        assertTrue(deployedGun.hasMeasuredDeployableMuzzle());
        assertEquals(new Vec3(0, 6, 20), deployedGun.getMeasuredDeployableMuzzle());
    }

    @Test
    void storedBarrelsReplayAndFollowTheirPointWhenItMoves()
    {
        MuzzleMeasurementCache.Results results = new MuzzleMeasurementCache.Results();
        MuzzleMeasurementCache.recordSpread(results, vehicle(), new MuzzleMeasurementCache.Spread(false, false, 0, List.of(new Vector3f(0F, -2F, 6F), new Vector3f(0F, 2F, -6F))));
        MuzzleMeasurementCache.recordMove(results, vehicle(), new MuzzleMeasurementCache.Move(false, false, 0, 30F, 22F, 0F));

        MuzzleMeasurementCache.Results restored = MuzzleMeasurementCache.fromJson(MuzzleMeasurementCache.toJson("key", results), "key");
        VehicleType vehicle = vehicle();
        MuzzleMeasurementCache.apply(restored, List.<InfoType>of(vehicle));

        assertEquals(1, restored.mountCount());
        ShootPoint point = vehicle.shootPoints(false).get(0);
        assertEquals(2, point.getBarrelCount());
        Vector3f root = point.getRootPos().getPosition();
        Vector3f second = point.getBarrelOffPos(1);
        assertEquals(30F, (root.x + second.x) * 16F, 1.0E-4F);
        assertEquals(24F, (root.y + second.y) * 16F, 1.0E-4F);
        assertEquals(-6F, (root.z + second.z) * 16F, 1.0E-4F);

        // A later move, such as a debug override, carries the barrels with the point.
        vehicle.setDebugShootPoint(false, 0, new Vector3f(40F, 22F, 0F));
        assertEquals(2, vehicle.shootPoints(false).get(0).getBarrelCount());
    }

    @Test
    void resultsStoredForOtherContentAreIgnored()
    {
        String json = MuzzleMeasurementCache.toJson("before", new MuzzleMeasurementCache.Results());

        assertNull(MuzzleMeasurementCache.fromJson(json, "after"));
        assertNotNull(MuzzleMeasurementCache.fromJson(json, "before"));
    }
}
