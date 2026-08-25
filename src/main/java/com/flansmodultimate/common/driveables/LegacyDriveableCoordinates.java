package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Converts coordinates authored for the 1.7.10 model basis into the modern
 * driveable basis. Legacy type files use model X as forward and model Z as
 * lateral; modern entity physics uses local X as forward and local Z as right.
 */
public final class LegacyDriveableCoordinates
{
    private LegacyDriveableCoordinates() {}

    public static Vec3 toLocal(@NotNull Vec3 legacy)
    {
        return new Vec3(legacy.z, legacy.y, -legacy.x);
    }

    public static Vec3 toLocal(@NotNull Vector3f legacy)
    {
        return new Vec3(legacy.z, legacy.y, -legacy.x);
    }

    /** Legacy type-file X is the front/back coordinate after basis conversion. */
    public static double legacyForwardCoordinate(@NotNull Vec3 local)
    {
        return -local.z;
    }

    /** Legacy type-file Z is the left/right coordinate after basis conversion. */
    public static double legacyRightCoordinate(@NotNull Vec3 local)
    {
        return local.x;
    }

    /**
     * Plane type files use the legacy flight-facing basis, while their model
     * geometry faces the opposite X direction. Apply the horizontal half-turn
     * already used by plane movement before passing an anchor to the shared
     * driveable model transform.
     */
    public static Vec3 applyPlaneModelFacing(@NotNull Vec3 local)
    {
        return new Vec3(-local.x, local.y, -local.z);
    }

    /** Legacy model Z pitch becomes rotation around local X after basis conversion. */
    public static Vec3 rotateBarrelPitchLocal(@NotNull Vec3 vector, float pitchDegrees)
    {
        double radians = Math.toRadians(pitchDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3(vector.x,
            vector.y * cosine + vector.z * sine,
            vector.z * cosine - vector.y * sine);
    }

    public static Vec3 rotateTurretYawLocal(@NotNull Vec3 vector, float yawDegrees)
    {
        double radians = Math.toRadians(yawDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3(vector.x * cosine + vector.z * sine, vector.y,
            -vector.x * sine + vector.z * cosine);
    }

    /** Applies barrel pitch before turret yaw, matching the rendered vehicle hierarchy. */
    public static Vec3 rotateTurretLocal(@NotNull Vec3 vector, float yawDegrees, float pitchDegrees)
    {
        return rotateTurretYawLocal(rotateBarrelPitchLocal(vector, pitchDegrees), yawDegrees);
    }

    /**
     * Applies the exact outer transform used by DriveableRenderer:
     * model roll (X), model pitch (Z), then entity yaw (Y).
     */
    public static Vec3 modelLocalToWorldDirection(@NotNull Vec3 local, float yawDegrees,
                                                   float pitchDegrees, float rollDegrees)
    {
        // Undo toLocal so the rotations operate on the legacy model axes.
        double legacyX = -local.z;
        double legacyY = local.y;
        // Legacy models are mirrored on Z by flipAll() before rendering.
        double legacyZ = -local.x;

        double roll = Math.toRadians(rollDegrees);
        double rollCos = Math.cos(roll);
        double rollSin = Math.sin(roll);
        double rolledY = legacyY * rollCos - legacyZ * rollSin;
        double rolledZ = legacyY * rollSin + legacyZ * rollCos;

        double pitch = Math.toRadians(pitchDegrees);
        double pitchCos = Math.cos(pitch);
        double pitchSin = Math.sin(pitch);
        double pitchedX = legacyX * pitchCos - rolledY * pitchSin;
        double pitchedY = legacyX * pitchSin + rolledY * pitchCos;

        double modelYaw = Math.toRadians(180F - yawDegrees);
        double yawCos = Math.cos(modelYaw);
        double yawSin = Math.sin(modelYaw);
        return new Vec3(pitchedX * yawCos + rolledZ * yawSin, pitchedY,
            -pitchedX * yawSin + rolledZ * yawCos);
    }

    /** Applies the basis conversion to packed xyz vertices in place. */
    public static void toLocalVertices(double @NotNull [] vertices)
    {
        for (int index = 0; index + 2 < vertices.length; index += 3)
        {
            double legacyX = vertices[index];
            vertices[index] = vertices[index + 2];
            vertices[index + 2] = -legacyX;
        }
    }
}
