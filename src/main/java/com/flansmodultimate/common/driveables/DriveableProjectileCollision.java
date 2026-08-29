package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Pure collision-space transforms and slab tracing for hull, turret and barrel part boxes. */
public final class DriveableProjectileCollision
{
    private static final double EPSILON = 1.0E-10D;

    private DriveableProjectileCollision() {}

    public record LocalHit(float fraction, Vec3 position, Vec3 projectileDirection,
                           Vec3 outwardNormal, EnumArmorFacing facing) {}

    public record ClosestSurface(Vec3 position, Vec3 outwardNormal,
                                 EnumArmorFacing facing, double distance) {}

    @Nullable
    public static LocalHit trace(AABB box, Vec3 hullLocalOrigin, Vec3 hullLocalMotion,
                                 EnumDriveablePart part, float turretYawDeg, float turretPitchDeg,
                                 Vec3 turretPivot, Vec3 turretOffset)
    {
        if (box == null || hullLocalOrigin == null || hullLocalMotion == null
            || hullLocalMotion.lengthSqr() < EPSILON)
            return null;
        boolean turret = DriveableCollisionProfile.isTurretMountedPart(part);
        boolean barrel = DriveableCollisionProfile.isBarrelPart(part);
        Vec3 origin = turret ? inversePoint(hullLocalOrigin, turretYawDeg,
            barrel ? turretPitchDeg : 0F, turretPivot, turretOffset) : hullLocalOrigin;
        Vec3 motion = turret ? inverseDirection(hullLocalMotion, turretYawDeg,
            barrel ? turretPitchDeg : 0F) : hullLocalMotion;
        return traceLocal(box, origin, motion);
    }

    @Nullable
    static LocalHit traceLocal(AABB box, Vec3 origin, Vec3 motion)
    {
        double entry = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        Vec3 entryNormal = Vec3.ZERO;
        double[] origins = {origin.x, origin.y, origin.z};
        double[] directions = {motion.x, motion.y, motion.z};
        double[] minima = {box.minX, box.minY, box.minZ};
        double[] maxima = {box.maxX, box.maxY, box.maxZ};
        Vec3[] negative = {new Vec3(-1D, 0D, 0D), new Vec3(0D, -1D, 0D), new Vec3(0D, 0D, -1D)};
        Vec3[] positive = {new Vec3(1D, 0D, 0D), new Vec3(0D, 1D, 0D), new Vec3(0D, 0D, 1D)};

        for (int axis = 0; axis < 3; axis++)
        {
            double direction = directions[axis];
            if (Math.abs(direction) < EPSILON)
            {
                if (origins[axis] < minima[axis] || origins[axis] > maxima[axis])
                    return null;
                continue;
            }
            double first = (minima[axis] - origins[axis]) / direction;
            double second = (maxima[axis] - origins[axis]) / direction;
            Vec3 normal = negative[axis];
            if (first > second)
            {
                double swap = first;
                first = second;
                second = swap;
                normal = positive[axis];
            }
            if (first > entry)
            {
                entry = first;
                entryNormal = normal;
            }
            exit = Math.min(exit, second);
            if (entry > exit)
                return null;
        }
        if (exit < 0D || entry > 1D)
            return null;
        double fraction = Mth.clamp(entry, 0D, 1D);
        if (entry < 0D)
            entryNormal = dominantOpposingNormal(motion);
        Vec3 position = origin.add(motion.scale(fraction));
        return new LocalHit((float) fraction, position, motion, entryNormal,
            EnumArmorFacing.fromOutwardNormal(entryNormal));
    }

    public static ClosestSurface closestSurface(AABB box, Vec3 hullLocalPoint,
                                                EnumDriveablePart part, float turretYawDeg,
                                                float turretPitchDeg, Vec3 turretPivot, Vec3 turretOffset)
    {
        boolean turret = DriveableCollisionProfile.isTurretMountedPart(part);
        boolean barrel = DriveableCollisionProfile.isBarrelPart(part);
        Vec3 point = turret ? inversePoint(hullLocalPoint, turretYawDeg,
            barrel ? turretPitchDeg : 0F, turretPivot, turretOffset) : hullLocalPoint;
        double x = Mth.clamp(point.x, box.minX, box.maxX);
        double y = Mth.clamp(point.y, box.minY, box.maxY);
        double z = Mth.clamp(point.z, box.minZ, box.maxZ);
        Vec3 closest = new Vec3(x, y, z);
        Vec3 outward = point.subtract(closest);
        if (outward.lengthSqr() < EPSILON)
            outward = nearestInsideNormal(box, point);
        else
            outward = dominantNormal(outward);
        return new ClosestSurface(closest, outward, EnumArmorFacing.fromOutwardNormal(outward),
            point.distanceTo(closest));
    }

    static Vec3 transformPointFromPartFrame(Vec3 point, float yaw, float pitch, Vec3 pivot, Vec3 offset)
    {
        Vec3 safePivot = pivot == null ? Vec3.ZERO : pivot;
        Vec3 safeOffset = offset == null ? Vec3.ZERO : offset;
        Vec3 relative = LegacyDriveableCoordinates.rotateBarrelPitchLocal(point.subtract(safePivot), pitch);
        return safePivot.add(LegacyDriveableCoordinates.rotateTurretYawLocal(relative, yaw))
            .add(LegacyDriveableCoordinates.rotateTurretYawLocal(safeOffset, yaw));
    }

    private static Vec3 inversePoint(Vec3 point, float yaw, float pitch, Vec3 pivot, Vec3 offset)
    {
        Vec3 safePivot = pivot == null ? Vec3.ZERO : pivot;
        Vec3 safeOffset = offset == null ? Vec3.ZERO : offset;
        Vec3 rotatedOffset = LegacyDriveableCoordinates.rotateTurretYawLocal(safeOffset, yaw);
        Vec3 relative = LegacyDriveableCoordinates.rotateTurretYawLocal(
            point.subtract(safePivot).subtract(rotatedOffset), -yaw);
        return safePivot.add(LegacyDriveableCoordinates.rotateBarrelPitchLocal(relative, -pitch));
    }

    private static Vec3 inverseDirection(Vec3 direction, float yaw, float pitch)
    {
        return LegacyDriveableCoordinates.rotateBarrelPitchLocal(
            LegacyDriveableCoordinates.rotateTurretYawLocal(direction, -yaw), -pitch);
    }

    private static Vec3 nearestInsideNormal(AABB box, Vec3 point)
    {
        double[] distances = {point.x - box.minX, box.maxX - point.x, point.y - box.minY,
            box.maxY - point.y, point.z - box.minZ, box.maxZ - point.z};
        Vec3[] normals = {new Vec3(-1D, 0D, 0D), new Vec3(1D, 0D, 0D), new Vec3(0D, -1D, 0D),
            new Vec3(0D, 1D, 0D), new Vec3(0D, 0D, -1D), new Vec3(0D, 0D, 1D)};
        int best = 0;
        for (int index = 1; index < distances.length; index++)
        {
            if (distances[index] < distances[best])
                best = index;
        }
        return normals[best];
    }

    private static Vec3 dominantOpposingNormal(Vec3 motion)
    {
        return dominantNormal(motion.scale(-1D));
    }

    private static Vec3 dominantNormal(Vec3 vector)
    {
        double x = Math.abs(vector.x);
        double y = Math.abs(vector.y);
        double z = Math.abs(vector.z);
        if (y >= x && y >= z)
            return new Vec3(0D, Math.copySign(1D, vector.y), 0D);
        if (x >= z)
            return new Vec3(Math.copySign(1D, vector.x), 0D, 0D);
        return new Vec3(0D, 0D, Math.copySign(1D, vector.z));
    }
}
