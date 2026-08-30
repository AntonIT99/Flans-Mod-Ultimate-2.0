package com.flansmodultimate.common.driveables;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.hooks.ClientHooks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Per-entity runtime state for shaped driveable collision and moving decks.
 * Geometry storage is reused each tick; candidate discovery is a bounded
 * spatial query and never scans the world's loaded-entity list.
 */
public final class DriveableCollisionHelper
{
    private static final int MAX_CANDIDATES = 128;
    private static final double MAX_QUERY_RADIUS = 96D;
    private static final double QUERY_MARGIN = 1.5D;
    private static final double MIN_DECK_NORMAL_Y = 0.35D;
    private static final double SUPPORT_ABOVE = 0.45D;
    private static final double SUPPORT_BELOW = 0.22D;
    private static final double LANDING_BELOW = 0.3D;
    private static final double MAX_SWEPT_LANDING_BELOW = 1.5D;
    private static final double MAX_PLATFORM_DELTA = 3D;
    private static final double MAX_SEPARATION_PER_TICK = 0.75D;
    private static final double GEOMETRY_EPSILON = 1.0E-7D;

    private final DriveableCollisionProfile profile;
    private final double[][] currentVertices;
    private final double[][] previousVertices;
    private final double[][] currentPlanes;
    private final double[][] currentBounds;
    private final boolean[] active;
    private final boolean[] planesValid;
    private final Pose currentPose = new Pose();
    private final Pose previousPose = new Pose();
    private final double[] planeScratch = new double[4];
    private final double[] barycentricScratch = new double[3];
    private final SupportResult supportScratch = new SupportResult();

    public DriveableCollisionHelper(DriveableCollisionProfile profile)
    {
        this.profile = profile == null ? DriveableCollisionProfile.compile(null) : profile;
        int shapeCount = this.profile.getShapes().size();
        currentVertices = new double[shapeCount][24];
        previousVertices = new double[shapeCount][24];
        currentPlanes = new double[shapeCount][DriveableCollisionProfile.FACE_QUADS.length * 4];
        currentBounds = new double[shapeCount][6];
        active = new boolean[shapeCount];
        planesValid = new boolean[shapeCount];
    }

    public boolean matches(DriveableCollisionProfile candidate)
    {
        return profile == candidate;
    }

    public void tick(Driveable driveable)
    {
        if (driveable == null || driveable.isRemoved() || profile.isEmpty())
            return;

        DriveableType type = driveable.getConfigType();
        if (type == null)
            return;
        currentPose.set(driveable.getX(), driveable.getY(), driveable.getZ(), driveable.getYaw(),
            driveable.getPitch(), driveable.getRoll());
        previousPose.set(driveable.xo, driveable.yo, driveable.zo, driveable.getPrevYaw(),
            driveable.getPrevPitch(), driveable.getPrevRoll());

        boolean discontinuity = driveable.tickCount <= 1
            || squaredDistance(currentPose.x, currentPose.y, currentPose.z,
                previousPose.x, previousPose.y, previousPose.z) > MAX_PLATFORM_DELTA * MAX_PLATFORM_DELTA;
        if (discontinuity)
            previousPose.copyFrom(currentPose);

        Vec3 pivot = type.getTurretOrigin() == null ? Vec3.ZERO
            : LegacyDriveableCoordinates.toLocal(type.getTurretOrigin());
        Vec3 offset = type.getTurretOriginOffset() == null ? Vec3.ZERO
            : LegacyDriveableCoordinates.toLocal(type.getTurretOriginOffset());
        double pivotX = pivot.x;
        double pivotY = pivot.y;
        double pivotZ = pivot.z;
        double offsetX = offset.x;
        double offsetY = offset.y;
        double offsetZ = offset.z;

        double queryMinX = Double.POSITIVE_INFINITY;
        double queryMinY = Double.POSITIVE_INFINITY;
        double queryMinZ = Double.POSITIVE_INFINITY;
        double queryMaxX = Double.NEGATIVE_INFINITY;
        double queryMaxY = Double.NEGATIVE_INFINITY;
        double queryMaxZ = Double.NEGATIVE_INFINITY;
        List<DriveableCollisionProfile.Shape> shapes = profile.getShapes();
        for (int index = 0; index < shapes.size(); index++)
        {
            DriveableCollisionProfile.Shape shape = shapes.get(index);
            active[index] = driveable.isPartIntact(shape.getPart());
            if (!active[index])
                continue;

            float currentTurretPitch = shape.isBarrel() ? driveable.getTurretPitch() : 0F;
            float previousTurretPitch = shape.isBarrel() ? driveable.getPrevTurretPitch() : 0F;
            transform(shape, currentVertices[index], currentPose, driveable.getTurretYaw(), currentTurretPitch,
                pivotX, pivotY, pivotZ, offsetX, offsetY, offsetZ);
            transform(shape, previousVertices[index], previousPose,
                discontinuity ? driveable.getTurretYaw() : driveable.getPrevTurretYaw(),
                discontinuity ? currentTurretPitch : previousTurretPitch,
                pivotX, pivotY, pivotZ, offsetX, offsetY, offsetZ);
            updateBounds(currentVertices[index], currentBounds[index]);
            updatePlanes(index);

            for (int vertex = 0; vertex < 8; vertex++)
            {
                int point = vertex * 3;
                queryMinX = Math.min(queryMinX, Math.min(currentVertices[index][point], previousVertices[index][point]));
                queryMinY = Math.min(queryMinY, Math.min(currentVertices[index][point + 1], previousVertices[index][point + 1]));
                queryMinZ = Math.min(queryMinZ, Math.min(currentVertices[index][point + 2], previousVertices[index][point + 2]));
                queryMaxX = Math.max(queryMaxX, Math.max(currentVertices[index][point], previousVertices[index][point]));
                queryMaxY = Math.max(queryMaxY, Math.max(currentVertices[index][point + 1], previousVertices[index][point + 1]));
                queryMaxZ = Math.max(queryMaxZ, Math.max(currentVertices[index][point + 2], previousVertices[index][point + 2]));
            }
        }
        if (!Double.isFinite(queryMinX) || !Double.isFinite(queryMaxX))
            return;

        queryMinX = Math.max(queryMinX, driveable.getX() - MAX_QUERY_RADIUS);
        queryMinY = Math.max(queryMinY, driveable.getY() - MAX_QUERY_RADIUS);
        queryMinZ = Math.max(queryMinZ, driveable.getZ() - MAX_QUERY_RADIUS);
        queryMaxX = Math.min(queryMaxX, driveable.getX() + MAX_QUERY_RADIUS);
        queryMaxY = Math.min(queryMaxY, driveable.getY() + MAX_QUERY_RADIUS);
        queryMaxZ = Math.min(queryMaxZ, driveable.getZ() + MAX_QUERY_RADIUS);
        if (queryMinX > queryMaxX || queryMinY > queryMaxY || queryMinZ > queryMaxZ)
            return;

        AABB query = new AABB(queryMinX, queryMinY, queryMinZ, queryMaxX, queryMaxY, queryMaxZ)
            .inflate(QUERY_MARGIN);
        boolean clientSide = driveable.level().isClientSide;
        List<LivingEntity> candidates = driveable.level().getEntitiesOfClass(LivingEntity.class, query,
            candidate -> candidate.isAlive() && !candidate.isSpectator() && !candidate.noPhysics
                && !candidate.isPassenger() && !driveable.isPartOfThis(candidate)
                && (!clientSide || ClientHooks.PLAYER.isLocalPlayer(candidate)));
        int count = Math.min(MAX_CANDIDATES, candidates.size());
        for (int index = 0; index < count; index++)
            handleCandidate(driveable, type, candidates.get(index));
    }

    private void updatePlanes(int shapeIndex)
    {
        // Legacy shape boxes may have slightly non-planar quads. Average each
        // quad's two triangle normals into a conservative separating hull;
        // only truly degenerate faces disable side separation.
        planesValid[shapeIndex] = true;
        double[] points = currentVertices[shapeIndex];
        double centreX = 0D;
        double centreY = 0D;
        double centreZ = 0D;
        for (int vertex = 0; vertex < 8; vertex++)
        {
            centreX += points[vertex * 3];
            centreY += points[vertex * 3 + 1];
            centreZ += points[vertex * 3 + 2];
        }
        centreX /= 8D;
        centreY /= 8D;
        centreZ /= 8D;
        for (int face = 0; face < DriveableCollisionProfile.FACE_QUADS.length; face++)
        {
            if (!DriveableCollisionProfile.facePlane(points, DriveableCollisionProfile.FACE_QUADS[face],
                centreX, centreY, centreZ, planeScratch))
            {
                planesValid[shapeIndex] = false;
                return;
            }
            System.arraycopy(planeScratch, 0, currentPlanes[shapeIndex], face * 4, 4);
        }
    }

    private void handleCandidate(Driveable driveable, DriveableType type, LivingEntity candidate)
    {
        supportScratch.reset();
        findSupport(candidate, supportScratch);
        if (supportScratch.found)
        {
            candidate.move(MoverType.SHULKER,
                new Vec3(supportScratch.deltaX, supportScratch.deltaY, supportScratch.deltaZ));
            candidate.setOnGround(true);
            candidate.fallDistance = 0F;
            Vec3 motion = candidate.getDeltaMovement();
            if (motion.y < 0D)
                candidate.setDeltaMovement(motion.x, 0D, motion.z);
            return;
        }

        AABB box = candidate.getBoundingBox();
        double centreX = (box.minX + box.maxX) * 0.5D;
        double centreY = (box.minY + box.maxY) * 0.5D;
        double centreZ = (box.minZ + box.maxZ) * 0.5D;
        double halfX = (box.maxX - box.minX) * 0.5D;
        double halfY = (box.maxY - box.minY) * 0.5D;
        double halfZ = (box.maxZ - box.minZ) * 0.5D;
        double bestDepth = Double.POSITIVE_INFINITY;
        double pushX = 0D;
        double pushY = 0D;
        double pushZ = 0D;
        for (int shape = 0; shape < active.length; shape++)
        {
            if (!active[shape] || !planesValid[shape] || !intersects(box, currentBounds[shape]))
                continue;
            double shapeDepth = Double.POSITIVE_INFINITY;
            double shapeNormalX = 0D;
            double shapeNormalY = 0D;
            double shapeNormalZ = 0D;
            boolean overlap = true;
            for (int face = 0; face < DriveableCollisionProfile.FACE_QUADS.length; face++)
            {
                int plane = face * 4;
                double normalX = currentPlanes[shape][plane];
                double normalY = currentPlanes[shape][plane + 1];
                double normalZ = currentPlanes[shape][plane + 2];
                double signed = normalX * centreX + normalY * centreY + normalZ * centreZ
                    + currentPlanes[shape][plane + 3];
                double radius = Math.abs(normalX) * halfX + Math.abs(normalY) * halfY + Math.abs(normalZ) * halfZ;
                if (signed - radius > GEOMETRY_EPSILON)
                {
                    overlap = false;
                    break;
                }
                double depth = radius - signed;
                if (depth < shapeDepth)
                {
                    shapeDepth = depth;
                    shapeNormalX = normalX;
                    shapeNormalY = normalY;
                    shapeNormalZ = normalZ;
                }
            }
            if (overlap && shapeDepth < bestDepth)
            {
                bestDepth = shapeDepth;
                pushX = shapeNormalX;
                pushY = shapeNormalY;
                pushZ = shapeNormalZ;
            }
        }
        if (Double.isFinite(bestDepth))
        {
            double distance = Math.min(MAX_SEPARATION_PER_TICK, Math.max(0D, bestDepth + 1.0E-4D));
            candidate.move(MoverType.SHULKER, new Vec3(pushX * distance, pushY * distance, pushZ * distance));
            if (!driveable.level().isClientSide && Math.abs(pushY) < 0.6D)
                applyConfiguredImpactDamage(driveable, type, candidate);
        }
    }

    private static void applyConfiguredImpactDamage(Driveable driveable, DriveableType type, LivingEntity candidate)
    {
        float throttle = Math.abs(driveable.getThrottle());
        if (!type.isCollisionDamageEnable() || throttle <= Math.max(0F, type.getCollisionDamageThrottle())
            || !canDamageCandidate(driveable, candidate))
            return;
        float amount = throttle * Math.max(0F, type.getCollisionDamageTimes());
        if (amount <= 0F)
            return;
        Entity controller = driveable.getControllingEntity();
        DamageSource source = controller instanceof Player player
            ? driveable.level().damageSources().playerAttack(player)
            : controller instanceof LivingEntity living
                ? driveable.level().damageSources().mobAttack(living)
                : driveable.level().damageSources().flyIntoWall();
        candidate.hurt(source, amount);
    }

    private static boolean canDamageCandidate(Driveable driveable, LivingEntity candidate)
    {
        Entity controller = driveable.getControllingEntity();
        if (controller == null)
            return true;
        if (candidate instanceof ServerPlayer victim && controller instanceof ServerPlayer attacker)
        {
            try
            {
                return FlansMod.teamsManager.getCurrentGameType()
                    .map(gameType -> gameType.canPlayerBeAttacked(victim, attacker))
                    .orElseGet(() -> !victim.isAlliedTo(attacker));
            }
            catch (RuntimeException ignored)
            {
                // Teams state is optional outside an active server round.
            }
        }
        return !candidate.isAlliedTo(controller) && !controller.isAlliedTo(candidate);
    }

    private void findSupport(LivingEntity candidate, SupportResult result)
    {
        AABB box = candidate.getBoundingBox();
        double centreX = (box.minX + box.maxX) * 0.5D;
        double centreZ = (box.minZ + box.maxZ) * 0.5D;
        double sampleX = Math.max(0D, (box.maxX - box.minX) * 0.4D);
        double sampleZ = Math.max(0D, (box.maxZ - box.minZ) * 0.4D);
        double foot = box.minY;
        double verticalMotion = candidate.getDeltaMovement().y;
        // The old ellipsoid collision accepted contact anywhere under a
        // player's feet. Sampling the centre alone lets a player fall through
        // narrow hull edges even while most of their footprint is supported.
        for (int sample = 0; sample < 5; sample++)
        {
            double x = centreX;
            double z = centreZ;
            if (sample > 0)
            {
                x += (sample == 1 || sample == 2) ? sampleX : -sampleX;
                z += (sample == 1 || sample == 3) ? sampleZ : -sampleZ;
            }
            findSupportAt(x, z, foot, verticalMotion, candidate.onGround(), sample == 0 ? 0D : 0.02D, result);
        }
    }

    private void findSupportAt(double x, double z, double foot, double verticalMotion, boolean onGround,
                               double samplePenalty, SupportResult result)
    {
        double landingBelow = sweptLandingTolerance(verticalMotion);
        for (int shape = 0; shape < active.length; shape++)
        {
            if (!active[shape])
                continue;
            for (int[] triangle : DriveableCollisionProfile.TOP_TRIANGLES)
            {
                if (normalY(currentVertices[shape], triangle) < MIN_DECK_NORMAL_Y)
                    continue;

                if (barycentricXZ(previousVertices[shape], triangle, x, z, barycentricScratch))
                {
                    double previousY = interpolate(previousVertices[shape], triangle, 1, barycentricScratch);
                    double gap = foot - previousY;
                    if (gap >= -SUPPORT_BELOW && gap <= SUPPORT_ABOVE
                        && verticalMotion <= 0.5D && (onGround || gap <= 0.14D || verticalMotion <= 0D))
                    {
                        double previousX = interpolate(previousVertices[shape], triangle, 0, barycentricScratch);
                        double previousZ = interpolate(previousVertices[shape], triangle, 2, barycentricScratch);
                        double currentX = interpolate(currentVertices[shape], triangle, 0, barycentricScratch);
                        double currentY = interpolate(currentVertices[shape], triangle, 1, barycentricScratch);
                        double currentZ = interpolate(currentVertices[shape], triangle, 2, barycentricScratch);
                        double deltaX = currentX - previousX;
                        // Gravity can move the entity slightly into a stationary
                        // deck before this helper ticks. Carrying it only by the
                        // deck's transform (zero in that case) lets that error
                        // accumulate until the entity falls through. Snap its
                        // feet back to the current material surface as legacy
                        // rider collision did.
                        double deltaY = supportVerticalCorrection(currentY, foot);
                        double deltaZ = currentZ - previousZ;
                        double deltaLength = squaredDistance(0D, 0D, 0D, deltaX, deltaY, deltaZ);
                        double score = Math.abs(gap) + samplePenalty;
                        if (deltaLength <= MAX_PLATFORM_DELTA * MAX_PLATFORM_DELTA && score < result.score)
                            result.set(deltaX, deltaY, deltaZ, score);
                    }
                }

                // Catch an entity landing on a deck that was not supporting it
                // during the previous transform. This correction is vertical;
                // subsequent ticks carry the entity with the material point.
                if (verticalMotion <= 0.1D
                    && barycentricXZ(currentVertices[shape], triangle, x, z, barycentricScratch))
                {
                    double surfaceY = interpolate(currentVertices[shape], triangle, 1, barycentricScratch);
                    double gap = foot - surfaceY;
                    double score = Math.abs(gap) + 0.5D + samplePenalty;
                    if (gap >= -landingBelow && gap <= SUPPORT_ABOVE && score < result.score)
                        result.set(0D, -gap + 1.0E-4D, 0D, score);
                }
            }
        }
    }

    static double sweptLandingTolerance(double verticalMotion)
    {
        if (!Double.isFinite(verticalMotion))
            return LANDING_BELOW;
        return Math.min(MAX_SWEPT_LANDING_BELOW, Math.max(LANDING_BELOW, -verticalMotion + 0.1D));
    }

    static double supportVerticalCorrection(double surfaceY, double footY)
    {
        if (!Double.isFinite(surfaceY) || !Double.isFinite(footY))
            return 0D;
        return surfaceY - footY + 1.0E-4D;
    }

    static boolean barycentricXZ(double[] points, int[] triangle, double x, double z, double[] output)
    {
        int a = triangle[0] * 3;
        int b = triangle[1] * 3;
        int c = triangle[2] * 3;
        double ax = points[a];
        double az = points[a + 2];
        double bx = points[b];
        double bz = points[b + 2];
        double cx = points[c];
        double cz = points[c + 2];
        double denominator = (bz - cz) * (ax - cx) + (cx - bx) * (az - cz);
        if (!Double.isFinite(denominator) || Math.abs(denominator) < GEOMETRY_EPSILON)
            return false;
        double aWeight = ((bz - cz) * (x - cx) + (cx - bx) * (z - cz)) / denominator;
        double bWeight = ((cz - az) * (x - cx) + (ax - cx) * (z - cz)) / denominator;
        double cWeight = 1D - aWeight - bWeight;
        if (aWeight < -GEOMETRY_EPSILON || bWeight < -GEOMETRY_EPSILON || cWeight < -GEOMETRY_EPSILON
            || aWeight > 1D + GEOMETRY_EPSILON || bWeight > 1D + GEOMETRY_EPSILON
            || cWeight > 1D + GEOMETRY_EPSILON)
            return false;
        output[0] = aWeight;
        output[1] = bWeight;
        output[2] = cWeight;
        return true;
    }

    private static double interpolate(double[] points, int[] triangle, int axis, double[] weights)
    {
        return points[triangle[0] * 3 + axis] * weights[0]
            + points[triangle[1] * 3 + axis] * weights[1]
            + points[triangle[2] * 3 + axis] * weights[2];
    }

    private static double normalY(double[] points, int[] triangle)
    {
        int a = triangle[0] * 3;
        int b = triangle[1] * 3;
        int c = triangle[2] * 3;
        double abX = points[b] - points[a];
        double abY = points[b + 1] - points[a + 1];
        double abZ = points[b + 2] - points[a + 2];
        double acX = points[c] - points[a];
        double acY = points[c + 1] - points[a + 1];
        double acZ = points[c + 2] - points[a + 2];
        double normalX = abY * acZ - abZ * acY;
        double normalY = abZ * acX - abX * acZ;
        double normalZ = abX * acY - abY * acX;
        double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        return length < GEOMETRY_EPSILON ? 0D : normalY / length;
    }

    private static void transform(DriveableCollisionProfile.Shape shape, double[] output, Pose pose,
                                  float turretYawDegrees, float turretPitchDegrees,
                                  double pivotX, double pivotY, double pivotZ,
                                  double offsetX, double offsetY, double offsetZ)
    {
        double yaw = turretYawDegrees * Math.PI / 180D;
        double yawCos = Math.cos(yaw);
        double yawSin = Math.sin(yaw);
        double rotatedOffsetX = offsetX * yawCos + offsetZ * yawSin;
        double rotatedOffsetZ = -offsetX * yawSin + offsetZ * yawCos;
        double[] source = shape.coordinates();
        for (int vertex = 0; vertex < 8; vertex++)
        {
            int point = vertex * 3;
            double localX = source[point];
            double localY = source[point + 1];
            double localZ = source[point + 2];
            if (shape.isTurret())
            {
                Vec3 relative = new Vec3(localX - pivotX, localY - pivotY, localZ - pivotZ);
                if (shape.isBarrel())
                    relative = LegacyDriveableCoordinates.rotateBarrelPitchLocal(relative, turretPitchDegrees);
                Vec3 rotated = LegacyDriveableCoordinates.rotateTurretYawLocal(relative, turretYawDegrees);
                localX = rotated.x + pivotX + rotatedOffsetX;
                localY = rotated.y + pivotY + offsetY;
                localZ = rotated.z + pivotZ + rotatedOffsetZ;
            }
            pose.toWorld(localX, localY, localZ, output, point);
        }
    }

    private static void updateBounds(double[] points, double[] bounds)
    {
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
    }

    private static boolean intersects(AABB box, double[] bounds)
    {
        return box.maxX >= bounds[0] && box.minX <= bounds[3]
            && box.maxY >= bounds[1] && box.minY <= bounds[4]
            && box.maxZ >= bounds[2] && box.minZ <= bounds[5];
    }

    private static double squaredDistance(double ax, double ay, double az, double bx, double by, double bz)
    {
        double x = ax - bx;
        double y = ay - by;
        double z = az - bz;
        return x * x + y * y + z * z;
    }

    private static final class SupportResult
    {
        private boolean found;
        private double deltaX;
        private double deltaY;
        private double deltaZ;
        private double score;

        private void reset()
        {
            found = false;
            deltaX = deltaY = deltaZ = 0D;
            score = Double.POSITIVE_INFINITY;
        }

        private void set(double x, double y, double z, double score)
        {
            found = true;
            deltaX = x;
            deltaY = y;
            deltaZ = z;
            this.score = score;
        }
    }

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
            double yaw = yawDegrees * Math.PI / 180D;
            double pitch = pitchDegrees * Math.PI / 180D;
            double roll = rollDegrees * Math.PI / 180D;
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

        private void copyFrom(Pose source)
        {
            x = source.x;
            y = source.y;
            z = source.z;
            forwardX = source.forwardX;
            forwardY = source.forwardY;
            forwardZ = source.forwardZ;
            upX = source.upX;
            upY = source.upY;
            upZ = source.upZ;
            rightX = source.rightX;
            rightY = source.rightY;
            rightZ = source.rightZ;
        }

        private void toWorld(double localX, double localY, double localZ, double[] output, int offset)
        {
            output[offset] = x + forwardX * localX + upX * localY + rightX * localZ;
            output[offset + 1] = y + forwardY * localX + upY * localY + rightY * localZ;
            output[offset + 2] = z + forwardZ * localX + upZ * localY + rightZ * localZ;
        }
    }
}
