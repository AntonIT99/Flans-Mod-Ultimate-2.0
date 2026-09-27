package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where an AA gun's barrels put their rounds, from the loaded model or from the
 * type file's {@code Barrel} lines. Kept apart from {@link AAGun} so the
 * arithmetic can be used and tested without an entity.
 */
public final class AAGunBarrelGeometry
{
    /** Legacy sentries fire from 1.5 blocks above their authored barrel. */
    public static final double SENTRY_ORIGIN_Y_OFFSET = 1.5D;

    private AAGunBarrelGeometry() {}

    /**
     * Offset of a measured barrel muzzle from the gun's position: the muzzle
     * pitched round the model's barrel pivot, then the whole gun yawed.
     */
    public static Vec3 modelBarrelOffset(Vec3 pivot, Vec3 muzzle, float gunYaw, float gunPitch)
    {
        double pitch = -gunPitch * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double modelX = pivot.x + muzzle.x * cosPitch - muzzle.y * sinPitch;
        double modelY = pivot.y + muzzle.x * sinPitch + muzzle.y * cosPitch;
        double modelZ = pivot.z + muzzle.z;

        double yaw = (270D - gunYaw) * Mth.DEG_TO_RAD;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        double x = modelX * cosYaw + modelZ * sinYaw;
        double z = -modelX * sinYaw + modelZ * cosYaw;

        return new Vec3(x / 16D, modelY / 16D, z / 16D);
    }

    /**
     * Offset, in blocks, that a type-file {@code Barrel x y z} line gives at
     * the gun's yaw and pitch.
     *
     * <p>The resting position is the legacy transform's, so authored lines land
     * where they always did. The legacy transform cannot pitch it, though: it
     * tilts only one of the two diagonal axes its 45 degree remap spreads a
     * barrel over, so a barrel laid forward sank as the gun elevated.
     * Elevation is instead applied round the horizontal axis across the gun's
     * aim, still pivoting on the gun's position.</p>
     */
    public static Vec3 legacyBarrelOffset(double legacyX, double legacyY, double legacyZ, float gunYaw, float gunPitch)
    {
        // Map legacy position to actual position
        double barrelX = legacyZ;
        double barrelY = legacyY;
        double barrelZ = -legacyX;

        double x = (barrelX - barrelZ) / 16D;
        double y = barrelY / 16D;
        double z = (barrelX + barrelZ) / 16D;

        return pitchAboutGun(rotate(x, y, z, 0D, gunYaw), gunYaw, gunPitch);
    }

    /**
     * Tilts a resting offset round the gun's position the way
     * {@code ModUtils#getDirectionFromPitchAndYaw} tilts the aim: positive
     * pitch lowers what lies ahead of the gun.
     */
    static Vec3 pitchAboutGun(Vec3 rest, float gunYaw, float gunPitch)
    {
        double yaw = gunYaw * Mth.DEG_TO_RAD;
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double forward = rest.x * forwardX + rest.z * forwardZ;
        double sideX = rest.x - forward * forwardX;
        double sideZ = rest.z - forward * forwardZ;

        double pitch = gunPitch * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double pitchedForward = forward * cosPitch + rest.y * sinPitch;
        double pitchedY = rest.y * cosPitch - forward * sinPitch;

        return new Vec3(sideX + pitchedForward * forwardX, pitchedY, sideZ + pitchedForward * forwardZ);
    }

    /**
     * The {@code Barrel x y z} line whose legacy transform lands on
     * {@code offset}, in blocks, with the gun at rest. The resting legacy
     * transform is kept exactly as 1.7.10 wrote it, radian slip included, so
     * it is inverted as the linear map it is rather than by hand.
     *
     * <p>Legacy barrels pitch round the gun's position and model barrels round
     * their own pivot, so the two agree at rest and part once the gun elevates.</p>
     */
    public static Vector3f legacyBarrelFor(Vec3 offset, boolean sentry)
    {
        Vec3 target = sentry ? offset.subtract(0D, SENTRY_ORIGIN_Y_OFFSET, 0D) : offset;
        Vec3 columnX = legacyBarrelOffset(1D, 0D, 0D, 0F, 0F);
        Vec3 columnY = legacyBarrelOffset(0D, 1D, 0D, 0F, 0F);
        Vec3 columnZ = legacyBarrelOffset(0D, 0D, 1D, 0F, 0F);
        // Cramer's rule on [columnX columnY columnZ] * legacy = target.
        double determinant = columnX.dot(columnY.cross(columnZ));
        if (Math.abs(determinant) < 1.0E-12D)
            return new Vector3f();
        return new Vector3f(
            (float) (target.dot(columnY.cross(columnZ)) / determinant),
            (float) (columnX.dot(target.cross(columnZ)) / determinant),
            (float) (columnX.dot(columnY.cross(target)) / determinant));
    }

    public static Vec3 rotate(double x, double y, double z, double gunPitch, double gunYaw)
    {
        double yaw = 180D - gunYaw * Mth.DEG_TO_RAD;
        double pitch = gunPitch * Mth.DEG_TO_RAD;

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double newX = x * cosYaw + (y * sinPitch + z * cosPitch) * sinYaw;
        double newY = y * cosPitch - z * sinPitch;
        double newZ = -x * sinYaw + (y * sinPitch + z * cosPitch) * cosYaw;

        return new Vec3(newX, newY, newZ);
    }
}
