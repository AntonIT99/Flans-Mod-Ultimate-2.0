package com.flansmodultimate.client.distant;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.DriveableProjectileCollision;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the simplified shape a driveable is drawn with on the far terrain from its damageable part boxes,
 * posed the way projectile tracing poses them. The far-terrain renderer only draws axis-aligned boxes, so
 * each part is cut along its length into a few pieces whose turned bounds stay close to the part, rather
 * than one box spanning a diagonal fuselage. A part is always cut into the same number of pieces, so a
 * driveable keeps its box count as it turns.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantProxyShapes
{
    /** Most boxes one driveable is drawn with. */
    public static final int MAX_BOXES = 96;
    /** Most pieces one part is cut into. */
    static final int MAX_SEGMENTS = 6;
    /** Shortest piece worth cutting, in blocks. */
    private static final double MIN_SEGMENT_LENGTH = 0.75D;
    /** Thinnest a part or its bounds count as when comparing their volumes, in blocks. */
    private static final double MIN_THICKNESS = 0.1D;
    /** Least a piece's turned bounds are shrunk to, per axis. */
    private static final double MIN_SHRINK = 0.6D;

    /** Turns a point of a part, in the model-local frame, into an offset from the driveable's position. */
    @FunctionalInterface
    public interface PartTransform
    {
        Vec3 toWorld(EnumDriveablePart part, Vec3 modelLocal);
    }

    /**
     * The shape of a driveable known only from its type and orientation, as for one the client does not track.
     * Turrets are drawn facing forwards.
     *
     * @param yaw   the driveable's own yaw in degrees, in the legacy model basis
     * @param pitch the driveable's own pitch in degrees
     * @param roll  the driveable's own roll in degrees
     */
    public static List<DistantBox> forType(DriveableType type, float yaw, float pitch, float roll,
                                           Set<EnumDriveablePart> destroyed, int argb)
    {
        PartTransform transform = (part, local) -> LegacyDriveableCoordinates.modelLocalToWorldDirection(local, yaw, pitch, roll);
        List<DistantBox> boxes = new ArrayList<>();
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : type.getHealth().entrySet())
        {
            if (entry.getValue() != null && !destroyed.contains(entry.getKey()))
                appendPart(boxes, entry.getKey(), Driveable.partBoxToModelLocal(entry.getValue()), transform, argb);
        }
        return boxes;
    }

    /** The shape of a driveable the client tracks, with its turret turned. */
    public static List<DistantBox> forEntity(Driveable driveable, int argb)
    {
        DriveableData data = driveable.getDriveableData();
        if (data == null)
            return List.of();

        Vec3 turretPivot = driveable.getCollisionTurretPivot();
        Vec3 turretOffset = driveable.getCollisionTurretOffset();
        float turretYaw = driveable.getTurretYaw();
        float turretPitch = driveable.getTurretPitch();
        PartTransform transform = (part, local) -> driveable.modelLocalDirectionToWorld(
            DriveableProjectileCollision.partPointToHullLocal(local, part, turretYaw, turretPitch, turretPivot, turretOffset));

        List<DistantBox> boxes = new ArrayList<>();
        for (DriveablePart part : data.getParts().values())
        {
            CollisionBox box = part.getBox();
            if (box != null && !part.isDestroyed())
                appendPart(boxes, part.getType(), driveable.partBoxModelLocal(box), transform, argb);
        }
        return boxes;
    }

    /** Adds the pieces of one part, given in the model-local frame, until the shape holds {@link #MAX_BOXES}. */
    public static void appendPart(List<DistantBox> out, EnumDriveablePart part, AABB modelLocal, PartTransform transform, int argb)
    {
        double[] min = {modelLocal.minX, modelLocal.minY, modelLocal.minZ};
        double[] max = {modelLocal.maxX, modelLocal.maxY, modelLocal.maxZ};
        double[] size = {max[0] - min[0], max[1] - min[1], max[2] - min[2]};
        double[] sorted = size.clone();
        Arrays.sort(sorted);
        if (sorted[2] <= 0D)
            return;

        int axis;
        if (size[0] >= size[1] && size[0] >= size[2])
            axis = 0;
        else if (size[1] >= size[2])
            axis = 1;
        else
            axis = 2;

        int segments = segmentsFor(sorted[2], sorted[1]);
        for (int i = 0; i < segments && out.size() < MAX_BOXES; i++)
        {
            double[] sliceMin = min.clone();
            double[] sliceMax = max.clone();
            sliceMin[axis] = min[axis] + size[axis] * i / segments;
            sliceMax[axis] = min[axis] + size[axis] * (i + 1) / segments;
            out.add(turnedBounds(part, sliceMin, sliceMax, transform, argb));
        }
    }

    /** Pieces a part of the given longest and middle extents is cut into. */
    static int segmentsFor(double longest, double middle)
    {
        return Mth.clamp((int) Math.ceil(longest / Math.max(middle, MIN_SEGMENT_LENGTH)), 1, MAX_SEGMENTS);
    }

    private static DistantBox turnedBounds(EnumDriveablePart part, double[] min, double[] max, PartTransform transform, int argb)
    {
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int corner = 0; corner < 8; corner++)
        {
            Vec3 world = transform.toWorld(part, new Vec3((corner & 1) == 0 ? min[0] : max[0],
                (corner & 2) == 0 ? min[1] : max[1], (corner & 4) == 0 ? min[2] : max[2]));
            double[] point = {world.x, world.y, world.z};
            for (int axis = 0; axis < 3; axis++)
            {
                low[axis] = Math.min(low[axis], point[axis]);
                high[axis] = Math.max(high[axis], point[axis]);
            }
        }

        // Bounds of a turned piece hold more than the piece: shrink them toward its volume
        double pieceVolume = 1D;
        double boundsVolume = 1D;
        for (int axis = 0; axis < 3; axis++)
        {
            pieceVolume *= Math.max(max[axis] - min[axis], MIN_THICKNESS);
            boundsVolume *= Math.max(high[axis] - low[axis], MIN_THICKNESS);
        }
        double shrink = Mth.clamp(Math.cbrt(pieceVolume / boundsVolume), MIN_SHRINK, 1D);

        float[] lowOut = new float[3];
        float[] highOut = new float[3];
        for (int axis = 0; axis < 3; axis++)
        {
            double centre = (low[axis] + high[axis]) * 0.5D;
            double half = (high[axis] - low[axis]) * 0.5D * shrink;
            lowOut[axis] = (float) (centre - half);
            highOut[axis] = (float) (centre + half);
        }
        return new DistantBox(lowOut[0], lowOut[1], lowOut[2], highOut[0], highOut[1], highOut[2], argb);
    }
}
