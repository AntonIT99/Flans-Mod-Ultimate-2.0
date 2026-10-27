package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Makes the part boxes worn by an entity that is not a driveable solid, with the movement geometry driveable hulls use:
 * other entities walk into them, stand on them and are carried along as the wearer moves and turns.
 *
 * <p>
 * The wearer itself and whoever rides it pass through, so it still moves by its own bounding box. Boxes are posed where
 * the hit detection of the body has them, at the size the body is drawn at.
 * </p>
 */
public final class PartBodyCollisionHelper
{
    private static final int MAX_CANDIDATES = 128;
    private static final double MAX_QUERY_RADIUS = 96D;
    private static final double CONTACT_SKIN = 1.0E-4D;
    private static final double MAX_CARRY = 3D;
    private static final double MAX_SEPARATION_PER_TICK = 0.75D;

    private final Entity owner;
    private final BooleanSupplier live;
    private final double[] bounds = new double[6];
    private final double[] vector = new double[4];
    @Nullable
    private DriveableHullGeometry geometry;
    private int shapeSignature;
    @Nullable
    private DriveableCollisionWorld.LevelHulls registry;
    private int lastTick = Integer.MIN_VALUE;
    private Vec3 lastOrigin = Vec3.ZERO;

    /**
     * @param live
     *            whether the owner still wears the body these boxes belong to and is alive
     */
    public PartBodyCollisionHelper(Entity owner, BooleanSupplier live)
    {
        this.owner = owner;
        this.live = live;
    }

    Entity owner()
    {
        return owner;
    }

    boolean isLive()
    {
        return !owner.isRemoved() && live.getAsBoolean();
    }

    /** @return the posed boxes, or null before the first tick */
    @Nullable
    DriveableHullGeometry geometry()
    {
        return geometry;
    }

    /** The wearer does not collide with its own boxes, and neither do its riders or what it rides. */
    boolean passes(@Nullable Entity entity)
    {
        return entity != null && (entity == owner || owner.hasIndirectPassenger(entity) || entity.hasIndirectPassenger(owner));
    }

    void forgetRegistry()
    {
        registry = null;
    }

    /** Stops entities colliding with these boxes. */
    public void unregister()
    {
        if (registry != null)
            registry.remove(this);
        registry = null;
    }

    /**
     * Poses the boxes for this tick, carries what stands on them and pushes out what they moved into. Either side, once
     * per tick, after the owner has moved.
     *
     * @param boxes
     *            the boxes that are solid, in the frame type files author them in
     * @param signature
     *            a number that changes whenever a box or the scale does
     * @param origin
     *            world position of the model origin
     * @param yaw
     *            the vanilla yaw the model faces
     * @param pitch
     *            degrees the nose is raised, as a driveable pitches
     * @param roll
     *            degrees the model is banked to the right, as a driveable rolls
     * @param scale
     *            factor the model is drawn larger or smaller by
     * @param turretPivot
     *            the point turret boxes turn around, in the model-local frame, unscaled
     * @param turretOffset
     *            the offset turret boxes carry round with them, in the model-local frame, unscaled
     * @param standing
     *            whether a part is still there to collide with
     */
    // Every argument is one fact of the pose; a holder would only be built to be taken apart again.
    @SuppressWarnings("java:S107")
    public void tick(Map<EnumDriveablePart, CollisionBox> boxes, int signature, Vec3 origin, float yaw, float pitch, float roll, float scale, float turretYaw, float turretPitch, Vec3 turretPivot,
        Vec3 turretOffset, Predicate<EnumDriveablePart> standing)
    {
        if (!isLive())
        {
            unregister();
            return;
        }
        boolean rebuilt = geometry == null || signature != shapeSignature;
        if (rebuilt)
        {
            geometry = new DriveableHullGeometry(profile(boxes, scale));
            shapeSignature = signature;
        }
        DriveableHullGeometry current = geometry;
        boolean continuous = !rebuilt && lastTick == owner.tickCount - 1 && lastOrigin.distanceToSqr(origin) <= MAX_CARRY * MAX_CARRY;
        lastTick = owner.tickCount;
        lastOrigin = origin;
        pose(current, origin, yaw, pitch, roll, scale, turretYaw, turretPitch, turretPivot, turretOffset, standing, continuous);
        register(owner.level());
        if (current.queryBounds(bounds))
            handleEntities(current);
    }

    /** Puts the boxes where the hit detection of the body has them. */
    // Every argument is one fact of the pose; a holder would only be built to be taken apart again.
    @SuppressWarnings("java:S107")
    static void pose(DriveableHullGeometry geometry, Vec3 origin, float yaw, float pitch, float roll, float scale, float turretYaw, float turretPitch, Vec3 turretPivot, Vec3 turretOffset,
        Predicate<EnumDriveablePart> standing, boolean continuous)
    {
        // The model faces the vanilla yaw a quarter turn on from the yaw driveables simulate with.
        geometry.update(origin.x, origin.y, origin.z, yaw - 90F, pitch, roll, turretYaw, turretPitch, turretPivot.scale(scale), turretOffset.scale(scale), standing, continuous);
    }

    /** The boxes as convex shapes at the size the body is drawn at. */
    static DriveableCollisionProfile profile(Map<EnumDriveablePart, CollisionBox> boxes, float scale)
    {
        List<DriveableCollisionProfile.Shape> shapes = new ArrayList<>(boxes.size());
        boxes.forEach((part, box) ->
        {
            DriveableCollisionProfile.Shape shape = DriveableCollisionProfile.compilePartBox(part, CollisionBox.inWorldUnits(box.getHealth(), box.getX() * scale, box.getY() * scale,
                box.getZ() * scale, box.getWidth() * scale, box.getHeight() * scale, box.getDepth() * scale, box.getPenetrationResistance(), box.getCrewDamageMultiplier()));
            if (shape != null)
                shapes.add(shape);
        });
        return DriveableCollisionProfile.of(shapes);
    }

    private void register(Level level)
    {
        DriveableCollisionWorld.LevelHulls hulls = DriveableCollisionWorld.hulls(level);
        if (hulls == registry)
            return;
        unregister();
        if (hulls != null)
        {
            hulls.add(this);
            registry = hulls;
        }
    }

    private void handleEntities(DriveableHullGeometry current)
    {
        Level level = owner.level();
        AABB query = new AABB(Math.max(bounds[0], owner.getX() - MAX_QUERY_RADIUS), Math.max(bounds[1], owner.getY() - MAX_QUERY_RADIUS), Math.max(bounds[2], owner.getZ() - MAX_QUERY_RADIUS),
            Math.min(bounds[3], owner.getX() + MAX_QUERY_RADIUS), Math.min(bounds[4], owner.getY() + MAX_QUERY_RADIUS), Math.min(bounds[5], owner.getZ() + MAX_QUERY_RADIUS)).inflate(1D);
        // Players are simulated by their own client, against the pose it sees.
        List<Entity> candidates = level.getEntities(owner, query, entity -> DriveableCollisionWorld.collidesWithHulls(entity) && !passes(entity) && DriveableCollisionWorld.isSimulatedHere(entity));
        int count = Math.min(MAX_CANDIDATES, candidates.size());
        for (int index = 0; index < count; index++)
            handleEntity(current, candidates.get(index));
    }

    private void handleEntity(DriveableHullGeometry current, Entity entity)
    {
        AABB box = entity.getBoundingBox();
        int support = current.findSupport(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (support >= 0)
        {
            carry(current, entity, support);
            box = entity.getBoundingBox();
        }
        if (!current.findPenetration(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, support >= 0, vector) || vector[3] <= CONTACT_SKIN)
            return;
        double distance = Math.min(MAX_SEPARATION_PER_TICK, vector[3] + CONTACT_SKIN);
        DriveableCollisionWorld.moveIgnoringHulls(entity, vector[0] * distance, vector[1] * distance, vector[2] * distance);
        if (vector[1] >= DriveableHullGeometry.MIN_SUPPORT_NORMAL_Y)
        {
            entity.setOnGround(true);
            entity.resetFallDistance();
        }
    }

    /** Moves an entity with the surface it stood on, turning it with the wearer as well. */
    private void carry(DriveableHullGeometry current, Entity entity, int shape)
    {
        AABB box = entity.getBoundingBox();
        double footX = (box.minX + box.maxX) * 0.5D;
        double footZ = (box.minZ + box.maxZ) * 0.5D;
        if (!current.carryPoint(shape, footX, box.minY, footZ, vector))
            return;
        double dx = vector[0] - footX;
        double dy = vector[1] - box.minY;
        double dz = vector[2] - footZ;
        if (dx * dx + dy * dy + dz * dz > MAX_CARRY * MAX_CARRY)
            return;
        DriveableCollisionWorld.moveIgnoringHulls(entity, dx, dy, dz);
        float yaw = current.carryYaw(shape);
        if (Math.abs(yaw) > 1.0E-3F)
        {
            entity.setYRot(entity.getYRot() + yaw);
            entity.setYHeadRot(entity.getYHeadRot() + yaw);
            if (entity instanceof LivingEntity living)
                living.yBodyRot += yaw;
        }
        entity.resetFallDistance();
    }
}
