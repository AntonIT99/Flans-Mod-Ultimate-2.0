package com.flansmodultimate.common.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared model-pixel transforms for deployed-gun firing and camera placement. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeployedGunMuzzleGeometry
{
    private static final double MODEL_PIXELS_PER_BLOCK = 16D;
    private static final double CAMERA_CLEARANCE_PIXELS = 8D;
    private static final double MIN_CAMERA_HEIGHT = 1D;
    private static final double MAX_CAMERA_HEIGHT = 1.75D;

    /** Model muzzle offset from the entity origin after pitch, yaw and ModelScale. */
    public static Vec3 modelMuzzleOffset(Vec3 pivot, Vec3 muzzle, float modelScale, float yawDeg, float pitchDeg)
    {
        Vec3 fromPivot = muzzle.subtract(pivot);
        double pitch = -pitchDeg * Mth.DEG_TO_RAD;
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        Vec3 aimed = pivot.add(fromPivot.x,
            fromPivot.y * cosPitch - fromPivot.z * sinPitch,
            fromPivot.y * sinPitch + fromPivot.z * cosPitch);

        double yaw = (180D - yawDeg) * Mth.DEG_TO_RAD;
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double scale = validScale(modelScale) / MODEL_PIXELS_PER_BLOCK;
        return new Vec3(
            (aimed.x * cosYaw + aimed.z * sinYaw) * scale,
            aimed.y * scale,
            (-aimed.x * sinYaw + aimed.z * cosYaw) * scale);
    }

    /**
     * A clear first-person eye height derived from the level muzzle. The bounds
     * prevent low legacy pivots from hiding the view and extreme models from
     * placing the camera implausibly far above the operator.
     */
    public static double recommendedCameraHeight(Vec3 muzzle, float modelScale)
    {
        double height = (muzzle.y + CAMERA_CLEARANCE_PIXELS) * validScale(modelScale) / MODEL_PIXELS_PER_BLOCK;
        return Mth.clamp(height, MIN_CAMERA_HEIGHT, MAX_CAMERA_HEIGHT);
    }

    private static double validScale(float modelScale)
    {
        return Float.isFinite(modelScale) && modelScale > 0F ? modelScale : 1D;
    }
}
