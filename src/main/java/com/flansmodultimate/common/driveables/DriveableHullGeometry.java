package com.flansmodultimate.common.driveables;

import org.jetbrains.annotations.NotNull;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * World-space collision hulls of one driveable, posed once per tick.
 *
 * <p>Every shape is treated as the convex hull of its eight corners and tested
 * against axis-aligned entity boxes with the separating axis theorem. The
 * candidate axes are the world axes, every face triangle normal and every hull
 * edge crossed with a world axis: the complete set for a convex polyhedron
 * against a box, so contacts are exact rather than approximated by bounds.
 * Nothing here depends on triangle winding, which the mirrored driveable basis
 * inverts.</p>
 *
 * <p>Two pose slots are kept. Movement and separation use the current pose;
 * support detection uses the pose of the previous update, which is the one an
 * entity riding the hull was resting on when it last moved.</p>
 */
public final class DriveableHullGeometry
{
    /** Overlap along an axis must exceed this before two surfaces count as touching. */
    static final double CONTACT_EPSILON = 1.0E-6D;
    /** Depth an entity may already be sunk into a hull while still being stopped from sinking deeper. */
    static final double MAX_RESTING_PENETRATION = 0.25D;
    /** Minimum Y of a surface normal that supports, and carries, entities standing on it. */
    static final double MIN_SUPPORT_NORMAL_Y = 0.35D;
    /** How far below its feet an entity's box looks for the surface it rests on. */
    static final double SUPPORT_PROBE = 0.08D;

    private static final double AXIS_EPSILON = 1.0E-9D;
    private static final double MOVEMENT_EPSILON = 1.0E-7D;
    private static final double FRAME_EPSILON = 1.0E-9D;
    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int MAX_AXES = 3 + DriveableCollisionProfile.FACE_QUADS.length * 2 + EDGES.length * 3;
    /** Corner tetrahedra tried as the affine frame that carries points rigidly with a shape. */
    private static final int[][] FRAMES = {{0, 1, 3, 4}, {6, 5, 7, 2}, {1, 0, 2, 5}, {7, 4, 6, 3}};

    private final List<DriveableCollisionProfile.Shape> shapes;
    private final Pose pose = new Pose();
    private final double[] axisScratch = new double[3];
    private Slot current;
    private Slot previous;
    private boolean posed;
    private boolean previousValid;
    private boolean previousIsCurrent;
    private double lastX;
    private double lastY;
    private double lastZ;
    private float lastYaw;
    private float lastPitch;
    private float lastRoll;
    private float lastTurretYaw;
    private float lastTurretPitch;

    public DriveableHullGeometry(@NotNull DriveableCollisionProfile profile)
    {
        shapes = profile.getShapes();
        current = new Slot(shapes.size());
        previous = new Slot(shapes.size());
    }

    /**
     * Poses the hulls for this tick.
     *
     * @param continuous whether the previous update was the immediately preceding
     *                   tick with no teleport since, so entities resting on the old
     *                   pose may be carried to the new one
     */
    public void update(double x, double y, double z, float yaw, float pitch, float roll, float turretYaw,
                       float turretPitch, @NotNull Vec3 turretPivot, @NotNull Vec3 turretOffset,
                       @NotNull Predicate<EnumDriveablePart> partIntact, boolean continuous)
    {
        if (posed && x == lastX && y == lastY && z == lastZ && yaw == lastYaw && pitch == lastPitch
            && roll == lastRoll && turretYaw == lastTurretYaw && turretPitch == lastTurretPitch)
        {
            // A parked driveable keeps its hulls, so the previous pose is the current one.
            previousIsCurrent = true;
            previousValid = continuous;
            refreshActive(current, partIntact);
            return;
        }

        if (posed)
        {
            Slot swap = previous;
            previous = current;
            current = swap;
        }
        previousIsCurrent = !posed;
        previousValid = posed && continuous;
        posed = true;
        lastX = x;
        lastY = y;
        lastZ = z;
        lastYaw = yaw;
        lastPitch = pitch;
        lastRoll = roll;
        lastTurretYaw = turretYaw;
        lastTurretPitch = turretPitch;

        pose.set(x, y, z, yaw, pitch, roll);
        double turretRadians = Math.toRadians(turretYaw);
        double turretCos = Math.cos(turretRadians);
        double turretSin = Math.sin(turretRadians);
        double offsetX = turretOffset.x * turretCos + turretOffset.z * turretSin;
        double offsetZ = -turretOffset.x * turretSin + turretOffset.z * turretCos;
        for (int index = 0; index < shapes.size(); index++)
        {
            DriveableCollisionProfile.Shape shape = shapes.get(index);
            transform(shape, current.vertices[index], turretYaw, shape.isBarrel() ? turretPitch : 0F,
                turretPivot, offsetX, turretOffset.y, offsetZ);
            build(current, index);
        }
        refreshActive(current, partIntact);
    }

    public boolean hasActiveShapes()
    {
        return posed && current.any;
    }

    /** Whether a box could touch any active hull in the current pose. */
    public boolean mayTouch(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        return hasActiveShapes() && overlaps(current.totalBounds, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Writes the bounds of the active hulls in both poses to {@code out}; false when there are none. */
    public boolean queryBounds(double @NotNull [] out)
    {
        out[0] = out[1] = out[2] = Double.POSITIVE_INFINITY;
        out[3] = out[4] = out[5] = Double.NEGATIVE_INFINITY;
        boolean found = mergeBounds(current, out);
        if (previousValid && !previousIsCurrent)
            found |= mergeBounds(previous, out);
        return found;
    }

    /**
     * Limits movement of a box along one world axis ({@code 0} X, {@code 1} Y,
     * {@code 2} Z) so it stops flush with the first hull surface in its path.
     *
     * <p>Mirrors vanilla voxel collision: surfaces a box is merely touching do
     * not hinder movement along them. A box already sunk deeper than
     * {@link #MAX_RESTING_PENETRATION} is left free, so it can escape a hull
     * that moved onto it instead of being trapped inside.</p>
     */
    public double clip(int axis, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                       double desired)
    {
        if (!hasActiveShapes() || axis < 0 || axis > 2 || !(Math.abs(desired) >= MOVEMENT_EPSILON))
            return desired;
        Slot slot = current;
        double sweptMinX = minX;
        double sweptMinY = minY;
        double sweptMinZ = minZ;
        double sweptMaxX = maxX;
        double sweptMaxY = maxY;
        double sweptMaxZ = maxZ;
        if (axis == 0)
        {
            if (desired < 0D)
                sweptMinX += desired;
            else
                sweptMaxX += desired;
        }
        else if (axis == 1)
        {
            if (desired < 0D)
                sweptMinY += desired;
            else
                sweptMaxY += desired;
        }
        else if (desired < 0D)
            sweptMinZ += desired;
        else
            sweptMaxZ += desired;
        if (!overlaps(slot.totalBounds, sweptMinX, sweptMinY, sweptMinZ, sweptMaxX, sweptMaxY, sweptMaxZ))
            return desired;

        double centreX = (minX + maxX) * 0.5D;
        double centreY = (minY + maxY) * 0.5D;
        double centreZ = (minZ + maxZ) * 0.5D;
        double halfX = (maxX - minX) * 0.5D;
        double halfY = (maxY - minY) * 0.5D;
        double halfZ = (maxZ - minZ) * 0.5D;
        for (int shape = 0; shape < shapes.size(); shape++)
        {
            if (!slot.active[shape]
                || !overlaps(slot.bounds[shape], sweptMinX, sweptMinY, sweptMinZ, sweptMaxX, sweptMaxY, sweptMaxZ))
                continue;
            desired = clipShape(slot, shape, axis, centreX, centreY, centreZ, halfX, halfY, halfZ, desired);
            if (Math.abs(desired) < MOVEMENT_EPSILON)
                return 0D;
        }
        return desired;
    }

    /**
     * Finds the shortest push that moves the box out of the deepest hull it
     * overlaps in the current pose. The unit direction is written to
     * {@code out[0..2]} and the distance to {@code out[3]}.
     *
     * @param upwardOnly only push along surfaces steep enough to stand on
     * @return whether such a push exists
     */
    public boolean findPenetration(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                   boolean upwardOnly, double @NotNull [] out)
    {
        if (!mayTouch(minX, minY, minZ, maxX, maxY, maxZ))
            return false;
        Slot slot = current;
        double centreX = (minX + maxX) * 0.5D;
        double centreY = (minY + maxY) * 0.5D;
        double centreZ = (minZ + maxZ) * 0.5D;
        double halfX = (maxX - minX) * 0.5D;
        double halfY = (maxY - minY) * 0.5D;
        double halfZ = (maxZ - minZ) * 0.5D;
        double deepest = -1D;
        for (int shape = 0; shape < shapes.size(); shape++)
        {
            if (!slot.active[shape] || !overlaps(slot.bounds[shape], minX, minY, minZ, maxX, maxY, maxZ))
                continue;
            double depth = penetration(slot, shape, centreX, centreY, centreZ, halfX, halfY, halfZ, upwardOnly,
                axisScratch);
            if (depth > deepest)
            {
                deepest = depth;
                out[0] = axisScratch[0];
                out[1] = axisScratch[1];
                out[2] = axisScratch[2];
                out[3] = depth;
            }
        }
        return deepest >= 0D;
    }

    /** Whether the box overlaps, rather than merely touches, any active hull in the current pose. */
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        if (!mayTouch(minX, minY, minZ, maxX, maxY, maxZ))
            return false;
        Slot slot = current;
        double centreX = (minX + maxX) * 0.5D;
        double centreY = (minY + maxY) * 0.5D;
        double centreZ = (minZ + maxZ) * 0.5D;
        double halfX = (maxX - minX) * 0.5D;
        double halfY = (maxY - minY) * 0.5D;
        double halfZ = (maxZ - minZ) * 0.5D;
        for (int shape = 0; shape < shapes.size(); shape++)
        {
            if (slot.active[shape] && overlaps(slot.bounds[shape], minX, minY, minZ, maxX, maxY, maxZ)
                && penetration(slot, shape, centreX, centreY, centreZ, halfX, halfY, halfZ, false, axisScratch) >= 0D)
                return true;
        }
        return false;
    }

    /**
     * Returns the shape the box was standing on in the previous pose, or
     * {@code -1}. Resting on, or having sunk slightly into, a walkable surface
     * counts; brushing against a wall does not.
     */
    public int findSupport(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        if (!previousValid)
            return -1;
        Slot slot = previousSlot();
        double probeMinY = minY - SUPPORT_PROBE;
        if (!slot.any || !overlaps(slot.totalBounds, minX, probeMinY, minZ, maxX, maxY, maxZ))
            return -1;
        double centreX = (minX + maxX) * 0.5D;
        double centreY = (probeMinY + maxY) * 0.5D;
        double centreZ = (minZ + maxZ) * 0.5D;
        double halfX = (maxX - minX) * 0.5D;
        double halfY = (maxY - probeMinY) * 0.5D;
        double halfZ = (maxZ - minZ) * 0.5D;
        int best = -1;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int shape = 0; shape < shapes.size(); shape++)
        {
            if (!slot.active[shape] || !overlaps(slot.bounds[shape], minX, probeMinY, minZ, maxX, maxY, maxZ))
                continue;
            double depth = penetration(slot, shape, centreX, centreY, centreZ, halfX, halfY, halfZ, true, axisScratch);
            if (depth < 0D || depth > SUPPORT_PROBE + MAX_RESTING_PENETRATION)
                continue;
            double score = Math.abs(depth - SUPPORT_PROBE);
            if (score < bestScore)
            {
                bestScore = score;
                best = shape;
            }
        }
        return best;
    }

    /** Moves a world point rigidly with a shape from the previous pose to the current one. */
    public boolean carryPoint(int shape, double x, double y, double z, double @NotNull [] out)
    {
        int frame = carryFrame(shape);
        if (frame < 0)
            return false;
        double[] from = previousSlot().frames[frame];
        double[] to = current.frames[frame];
        double relativeX = x - from[0];
        double relativeY = y - from[1];
        double relativeZ = z - from[2];
        double a = from[12] * relativeX + from[13] * relativeY + from[14] * relativeZ;
        double b = from[15] * relativeX + from[16] * relativeY + from[17] * relativeZ;
        double c = from[18] * relativeX + from[19] * relativeY + from[20] * relativeZ;
        out[0] = to[0] + to[3] * a + to[6] * b + to[9] * c;
        out[1] = to[1] + to[4] * a + to[7] * b + to[10] * c;
        out[2] = to[2] + to[5] * a + to[8] * b + to[11] * c;
        return true;
    }

    /** Horizontal turn, in vanilla yaw degrees, that a shape made since the previous pose. */
    public float carryYaw(int shape)
    {
        int frame = carryFrame(shape);
        if (frame < 0)
            return 0F;
        double[] from = previousSlot().frames[frame];
        double[] to = current.frames[frame];
        // World +X carried through the same affine map, without translation.
        double a = from[12];
        double b = from[15];
        double c = from[18];
        double x = to[3] * a + to[6] * b + to[9] * c;
        double z = to[5] * a + to[8] * b + to[11] * c;
        if (x * x + z * z < 1.0E-4D)
            return 0F;
        return Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(-x, z)) + 90D));
    }

    private Slot previousSlot()
    {
        return previousIsCurrent ? current : previous;
    }

    private int carryFrame(int shape)
    {
        if (!previousValid || shape < 0 || shape >= shapes.size())
            return -1;
        Slot from = previousSlot();
        if (from.frameValid[shape] && current.frameValid[shape])
            return shape;
        // A flat shape borrows the frame of any shape moved by the same rigid transform.
        DriveableCollisionProfile.Shape target = shapes.get(shape);
        for (int index = 0; index < shapes.size(); index++)
        {
            DriveableCollisionProfile.Shape other = shapes.get(index);
            if (other.isTurret() == target.isTurret() && other.isBarrel() == target.isBarrel()
                && from.frameValid[index] && current.frameValid[index])
                return index;
        }
        return -1;
    }

    private void refreshActive(Slot slot, Predicate<EnumDriveablePart> partIntact)
    {
        for (int index = 0; index < shapes.size(); index++)
            slot.active[index] = partIntact.test(shapes.get(index).getPart());
        slot.refreshTotals();
    }

    private void transform(DriveableCollisionProfile.Shape shape, double[] output, float turretYaw, float barrelPitch,
                           Vec3 pivot, double offsetX, double offsetY, double offsetZ)
    {
        double[] source = shape.coordinates();
        for (int vertex = 0; vertex < 8; vertex++)
        {
            int point = vertex * 3;
            double localX = source[point];
            double localY = source[point + 1];
            double localZ = source[point + 2];
            if (shape.isTurret())
            {
                Vec3 relative = new Vec3(localX - pivot.x, localY - pivot.y, localZ - pivot.z);
                if (shape.isBarrel())
                    relative = LegacyDriveableCoordinates.rotateBarrelPitchLocal(relative, barrelPitch);
                Vec3 rotated = LegacyDriveableCoordinates.rotateTurretYawLocal(relative, turretYaw);
                localX = rotated.x + pivot.x + offsetX;
                localY = rotated.y + pivot.y + offsetY;
                localZ = rotated.z + pivot.z + offsetZ;
            }
            pose.toWorld(localX, localY, localZ, output, point);
        }
    }

    private static void build(Slot slot, int index)
    {
        double[] points = slot.vertices[index];
        double[] bounds = slot.bounds[index];
        bounds[0] = bounds[1] = bounds[2] = Double.POSITIVE_INFINITY;
        bounds[3] = bounds[4] = bounds[5] = Double.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 8; vertex++)
        {
            int point = vertex * 3;
            bounds[0] = Math.min(bounds[0], points[point]);
            bounds[1] = Math.min(bounds[1], points[point + 1]);
            bounds[2] = Math.min(bounds[2], points[point + 2]);
            bounds[3] = Math.max(bounds[3], points[point]);
            bounds[4] = Math.max(bounds[4], points[point + 1]);
            bounds[5] = Math.max(bounds[5], points[point + 2]);
        }

        double[] axes = slot.axes[index];
        int count = addAxis(axes, 0, 1D, 0D, 0D);
        count = addAxis(axes, count, 0D, 1D, 0D);
        count = addAxis(axes, count, 0D, 0D, 1D);
        for (int[] face : DriveableCollisionProfile.FACE_QUADS)
        {
            count = addTriangleNormal(axes, count, points, face[0], face[1], face[2]);
            count = addTriangleNormal(axes, count, points, face[0], face[2], face[3]);
        }
        for (int[] edge : EDGES)
        {
            double edgeX = points[edge[1] * 3] - points[edge[0] * 3];
            double edgeY = points[edge[1] * 3 + 1] - points[edge[0] * 3 + 1];
            double edgeZ = points[edge[1] * 3 + 2] - points[edge[0] * 3 + 2];
            count = addAxis(axes, count, 0D, edgeZ, -edgeY);
            count = addAxis(axes, count, -edgeZ, 0D, edgeX);
            count = addAxis(axes, count, edgeY, -edgeX, 0D);
        }
        slot.axisCount[index] = count;

        double[] extents = slot.extents[index];
        for (int axis = 0; axis < count; axis++)
        {
            double normalX = axes[axis * 3];
            double normalY = axes[axis * 3 + 1];
            double normalZ = axes[axis * 3 + 2];
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 8; vertex++)
            {
                int point = vertex * 3;
                double projection = normalX * points[point] + normalY * points[point + 1] + normalZ * points[point + 2];
                min = Math.min(min, projection);
                max = Math.max(max, projection);
            }
            extents[axis * 2] = min;
            extents[axis * 2 + 1] = max;
        }
        slot.frameValid[index] = buildFrame(points, slot.frames[index]);
    }

    private static int addTriangleNormal(double[] axes, int count, double[] points, int a, int b, int c)
    {
        double abX = points[b * 3] - points[a * 3];
        double abY = points[b * 3 + 1] - points[a * 3 + 1];
        double abZ = points[b * 3 + 2] - points[a * 3 + 2];
        double acX = points[c * 3] - points[a * 3];
        double acY = points[c * 3 + 1] - points[a * 3 + 1];
        double acZ = points[c * 3 + 2] - points[a * 3 + 2];
        return addAxis(axes, count, abY * acZ - abZ * acY, abZ * acX - abX * acZ, abX * acY - abY * acX);
    }

    private static int addAxis(double[] axes, int count, double x, double y, double z)
    {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (!(length > AXIS_EPSILON) || count >= MAX_AXES)
            return count;
        x /= length;
        y /= length;
        z /= length;
        for (int axis = 0; axis < count; axis++)
        {
            int offset = axis * 3;
            if (Math.abs(axes[offset] * x + axes[offset + 1] * y + axes[offset + 2] * z) > 1D - 1.0E-9D)
                return count;
        }
        axes[count * 3] = x;
        axes[count * 3 + 1] = y;
        axes[count * 3 + 2] = z;
        return count + 1;
    }

    /**
     * Stores an affine frame (origin, edge columns, inverse rows) spanned by
     * four corners. A rigid transform preserves each point's coordinates in it,
     * which is what lets a riding entity be carried without inverting the
     * pose and turret rotations.
     */
    private static boolean buildFrame(double[] points, double[] frame)
    {
        int[] best = null;
        double bestDeterminant = 0D;
        for (int[] candidate : FRAMES)
        {
            double determinant = frameDeterminant(points, candidate);
            if (Math.abs(determinant) > Math.abs(bestDeterminant))
            {
                bestDeterminant = determinant;
                best = candidate;
            }
        }
        if (best == null || !(Math.abs(bestDeterminant) > FRAME_EPSILON))
            return false;

        int origin = best[0] * 3;
        frame[0] = points[origin];
        frame[1] = points[origin + 1];
        frame[2] = points[origin + 2];
        for (int column = 0; column < 3; column++)
        {
            int corner = best[column + 1] * 3;
            frame[3 + column * 3] = points[corner] - frame[0];
            frame[4 + column * 3] = points[corner + 1] - frame[1];
            frame[5 + column * 3] = points[corner + 2] - frame[2];
        }
        double e1X = frame[3];
        double e1Y = frame[4];
        double e1Z = frame[5];
        double e2X = frame[6];
        double e2Y = frame[7];
        double e2Z = frame[8];
        double e3X = frame[9];
        double e3Y = frame[10];
        double e3Z = frame[11];
        frame[12] = (e2Y * e3Z - e2Z * e3Y) / bestDeterminant;
        frame[13] = (e2Z * e3X - e2X * e3Z) / bestDeterminant;
        frame[14] = (e2X * e3Y - e2Y * e3X) / bestDeterminant;
        frame[15] = (e3Y * e1Z - e3Z * e1Y) / bestDeterminant;
        frame[16] = (e3Z * e1X - e3X * e1Z) / bestDeterminant;
        frame[17] = (e3X * e1Y - e3Y * e1X) / bestDeterminant;
        frame[18] = (e1Y * e2Z - e1Z * e2Y) / bestDeterminant;
        frame[19] = (e1Z * e2X - e1X * e2Z) / bestDeterminant;
        frame[20] = (e1X * e2Y - e1Y * e2X) / bestDeterminant;
        return true;
    }

    private static double frameDeterminant(double[] points, int[] frame)
    {
        int origin = frame[0] * 3;
        int a = frame[1] * 3;
        int b = frame[2] * 3;
        int c = frame[3] * 3;
        double e1X = points[a] - points[origin];
        double e1Y = points[a + 1] - points[origin + 1];
        double e1Z = points[a + 2] - points[origin + 2];
        double e2X = points[b] - points[origin];
        double e2Y = points[b + 1] - points[origin + 1];
        double e2Z = points[b + 2] - points[origin + 2];
        double e3X = points[c] - points[origin];
        double e3Y = points[c + 1] - points[origin + 1];
        double e3Z = points[c + 2] - points[origin + 2];
        return e1X * (e2Y * e3Z - e2Z * e3Y) + e1Y * (e2Z * e3X - e2X * e3Z) + e1Z * (e2X * e3Y - e2Y * e3X);
    }

    private static double clipShape(Slot slot, int shape, int axis, double centreX, double centreY, double centreZ,
                                    double halfX, double halfY, double halfZ, double desired)
    {
        double[] axes = slot.axes[shape];
        double[] extents = slot.extents[shape];
        // Interval of travel along the axis during which the box overlaps the hull.
        double lower = Double.NEGATIVE_INFINITY;
        double upper = Double.POSITIVE_INFINITY;
        double lowerSpeed = 1D;
        double upperSpeed = 1D;
        for (int index = 0; index < slot.axisCount[shape]; index++)
        {
            double normalX = axes[index * 3];
            double normalY = axes[index * 3 + 1];
            double normalZ = axes[index * 3 + 2];
            double radius = Math.abs(normalX) * halfX + Math.abs(normalY) * halfY + Math.abs(normalZ) * halfZ;
            double centre = normalX * centreX + normalY * centreY + normalZ * centreZ;
            double hullMin = extents[index * 2];
            double hullMax = extents[index * 2 + 1];
            double speed = axis == 0 ? normalX : axis == 1 ? normalY : normalZ;
            if (Math.abs(speed) < AXIS_EPSILON)
            {
                if (centre + radius <= hullMin + CONTACT_EPSILON || centre - radius >= hullMax - CONTACT_EPSILON)
                    return desired;
                continue;
            }
            double first = (hullMin + CONTACT_EPSILON - centre - radius) / speed;
            double second = (hullMax - CONTACT_EPSILON - centre + radius) / speed;
            double enter = speed > 0D ? first : second;
            double exit = speed > 0D ? second : first;
            if (enter > lower)
            {
                lower = enter;
                lowerSpeed = Math.abs(speed);
            }
            if (exit < upper)
            {
                upper = exit;
                upperSpeed = Math.abs(speed);
            }
            if (lower >= upper)
                return desired;
        }

        boolean positive = desired > 0D;
        double enter = positive ? lower : -upper;
        double exit = positive ? upper : -lower;
        double distance = Math.abs(desired);
        if (exit <= 0D || enter >= distance)
            return desired;
        if (enter < -Math.min(MAX_RESTING_PENETRATION, (exit - enter) * 0.5D))
            return desired;
        // The interval was shrunk by the contact epsilon; stop at the surface itself.
        double contact = enter - CONTACT_EPSILON / (positive ? lowerSpeed : upperSpeed);
        return Math.copySign(Math.max(0D, contact), desired);
    }

    /** Shortest push out of one shape, or {@code -1} when the box does not overlap it. */
    private static double penetration(Slot slot, int shape, double centreX, double centreY, double centreZ,
                                      double halfX, double halfY, double halfZ, boolean upwardOnly, double[] direction)
    {
        double[] axes = slot.axes[shape];
        double[] extents = slot.extents[shape];
        double best = Double.POSITIVE_INFINITY;
        for (int index = 0; index < slot.axisCount[shape]; index++)
        {
            double normalX = axes[index * 3];
            double normalY = axes[index * 3 + 1];
            double normalZ = axes[index * 3 + 2];
            double radius = Math.abs(normalX) * halfX + Math.abs(normalY) * halfY + Math.abs(normalZ) * halfZ;
            double centre = normalX * centreX + normalY * centreY + normalZ * centreZ;
            double alongNormal = extents[index * 2 + 1] - (centre - radius);
            double againstNormal = centre + radius - extents[index * 2];
            if (alongNormal <= CONTACT_EPSILON || againstNormal <= CONTACT_EPSILON)
                return -1D;
            if ((!upwardOnly || normalY >= MIN_SUPPORT_NORMAL_Y) && alongNormal < best)
            {
                best = alongNormal;
                direction[0] = normalX;
                direction[1] = normalY;
                direction[2] = normalZ;
            }
            if ((!upwardOnly || -normalY >= MIN_SUPPORT_NORMAL_Y) && againstNormal < best)
            {
                best = againstNormal;
                direction[0] = -normalX;
                direction[1] = -normalY;
                direction[2] = -normalZ;
            }
        }
        return Double.isFinite(best) ? best : -1D;
    }

    private static boolean mergeBounds(Slot slot, double[] out)
    {
        if (!slot.any)
            return false;
        for (int axis = 0; axis < 3; axis++)
        {
            out[axis] = Math.min(out[axis], slot.totalBounds[axis]);
            out[axis + 3] = Math.max(out[axis + 3], slot.totalBounds[axis + 3]);
        }
        return true;
    }

    private static boolean overlaps(double[] bounds, double minX, double minY, double minZ, double maxX, double maxY,
                                    double maxZ)
    {
        return maxX >= bounds[0] && minX <= bounds[3]
            && maxY >= bounds[1] && minY <= bounds[4]
            && maxZ >= bounds[2] && minZ <= bounds[5];
    }

    private static final class Slot
    {
        private final double[][] vertices;
        private final boolean[] active;
        private final double[][] bounds;
        private final double[][] axes;
        private final double[][] extents;
        private final int[] axisCount;
        private final double[][] frames;
        private final boolean[] frameValid;
        private final double[] totalBounds = new double[6];
        private boolean any;

        private Slot(int shapeCount)
        {
            vertices = new double[shapeCount][24];
            active = new boolean[shapeCount];
            bounds = new double[shapeCount][6];
            axes = new double[shapeCount][MAX_AXES * 3];
            extents = new double[shapeCount][MAX_AXES * 2];
            axisCount = new int[shapeCount];
            frames = new double[shapeCount][21];
            frameValid = new boolean[shapeCount];
        }

        private void refreshTotals()
        {
            any = false;
            totalBounds[0] = totalBounds[1] = totalBounds[2] = Double.POSITIVE_INFINITY;
            totalBounds[3] = totalBounds[4] = totalBounds[5] = Double.NEGATIVE_INFINITY;
            for (int shape = 0; shape < active.length; shape++)
            {
                if (!active[shape])
                    continue;
                any = true;
                for (int axis = 0; axis < 3; axis++)
                {
                    totalBounds[axis] = Math.min(totalBounds[axis], bounds[shape][axis]);
                    totalBounds[axis + 3] = Math.max(totalBounds[axis + 3], bounds[shape][axis + 3]);
                }
            }
        }
    }

    /** The driveable basis shared with {@code Driveable#localDirectionToWorld}; note it is mirrored. */
    private static final class Pose
    {
        private double x;
        private double y;
        private double z;
        private double forwardX;
        private double forwardY;
        private double forwardZ;
        private double upX;
        private double upY;
        private double upZ;
        private double rightX;
        private double rightY;
        private double rightZ;

        private void set(double x, double y, double z, float yawDegrees, float pitchDegrees, float rollDegrees)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            double yaw = Math.toRadians(yawDegrees);
            double pitch = Math.toRadians(pitchDegrees);
            double roll = Math.toRadians(rollDegrees);
            double sinYaw = Math.sin(yaw);
            double cosYaw = Math.cos(yaw);
            double sinPitch = Math.sin(pitch);
            double cosPitch = Math.cos(pitch);
            double sinRoll = Math.sin(roll);
            double cosRoll = Math.cos(roll);

            forwardX = -sinYaw * cosPitch;
            forwardY = -sinPitch;
            forwardZ = cosYaw * cosPitch;
            double horizontalRightX = cosYaw;
            double horizontalRightZ = sinYaw;
            double unrolledUpX = -sinPitch * sinYaw;
            double unrolledUpY = cosPitch;
            double unrolledUpZ = sinPitch * cosYaw;
            rightX = horizontalRightX * cosRoll + unrolledUpX * sinRoll;
            rightY = unrolledUpY * sinRoll;
            rightZ = horizontalRightZ * cosRoll + unrolledUpZ * sinRoll;
            upX = unrolledUpX * cosRoll - horizontalRightX * sinRoll;
            upY = unrolledUpY * cosRoll;
            upZ = unrolledUpZ * cosRoll - horizontalRightZ * sinRoll;
        }

        private void toWorld(double localX, double localY, double localZ, double[] output, int offset)
        {
            output[offset] = x + forwardX * localX + upX * localY + rightX * localZ;
            output[offset + 1] = y + forwardY * localX + upY * localY + rightY * localZ;
            output[offset + 2] = z + forwardZ * localX + upZ * localY + rightZ * localZ;
        }
    }
}
