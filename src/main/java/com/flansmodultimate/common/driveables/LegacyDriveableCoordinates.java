package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

    /** Minecraft view yaw that points along the rendered plane's nose. */
    public static float planeForwardYaw(float driveableYaw)
    {
        return renderedForwardYaw(driveableYaw, true);
    }

    /** Converts the simulation yaw to the vanilla entity yaw of the rendered nose/front. */
    public static float renderedForwardYaw(float driveableYaw, boolean planeModelFacing)
    {
        return Mth.wrapDegrees(driveableYaw + (planeModelFacing ? -90F : 90F));
    }

    /** Inverse used when vanilla movement packets carry the aligned entity yaw. */
    public static float driveableYawFromRenderedForward(float entityYaw, boolean planeModelFacing)
    {
        return Mth.wrapDegrees(entityYaw + (planeModelFacing ? 90F : -90F));
    }

    /** Vanilla look pitch matching the rendered model's longitudinal axis. */
    public static float renderedForwardPitch(float driveablePitch, boolean planeModelFacing)
    {
        return planeModelFacing ? driveablePitch : -driveablePitch;
    }

    /** Pitch conversion is its own inverse. */
    public static float driveablePitchFromRenderedForward(float entityPitch, boolean planeModelFacing)
    {
        return renderedForwardPitch(entityPitch, planeModelFacing);
    }

    /**
     * Screen roll that keeps a rider's horizon locked to the rendered driveable.
     *
     * <p>The renderer applies model roll around model X, which is the same axis
     * the camera looks along. A camera rolling with the driveable therefore has
     * to rotate the image the opposite way. Non-plane models face the mirrored
     * model X direction, which flips the sense of both their pitch and their
     * roll relative to the view, exactly like {@link #renderedForwardPitch}.</p>
     */
    public static float renderedViewRoll(float driveableRoll, boolean planeModelFacing)
    {
        return planeModelFacing ? -driveableRoll : driveableRoll;
    }

    /**
     * Composes a rider's local look rotation with the driveable orientation and
     * returns the resulting vanilla camera angles. Adding the two sets of Euler
     * angles instead only matches while the driveable is level; composing the
     * rotations is what keeps first and third person aligned once it banks.
     */
    public static ViewAngles mountedViewAngles(float driveableYaw, float driveablePitch, float driveableRoll,
                                                float aimYaw, float aimPitch, boolean planeModelFacing)
    {
        // Seat aim is authored in vanilla view space, so it needs the same basis
        // conversion as the composed result before the two are multiplied.
        RotatedAxes look = new RotatedAxes(aimYaw, renderedForwardPitch(aimPitch, planeModelFacing), 0F);
        RotatedAxes global = new RotatedAxes(driveableYaw, driveablePitch, driveableRoll)
            .findLocalAxesGlobally(look);
        return new ViewAngles(renderedForwardYaw(global.getYaw(), planeModelFacing),
            Mth.clamp(renderedForwardPitch(global.getPitch(), planeModelFacing), -89.9F, 89.9F),
            Mth.wrapDegrees(renderedViewRoll(global.getRoll(), planeModelFacing)));
    }

    /** Vanilla camera angles: yaw and pitch aim the view, roll tilts the screen. */
    public record ViewAngles(float yaw, float pitch, float roll) {}

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
