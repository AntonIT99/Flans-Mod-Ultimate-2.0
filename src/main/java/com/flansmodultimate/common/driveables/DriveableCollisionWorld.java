package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.hooks.ClientHooks;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes driveable hulls solid to the movement of ordinary entities.
 *
 * <p>Vanilla movement collides only with blocks and with entity bounding boxes,
 * neither of which can describe a rotated, shaped hull. Corrections applied
 * after an entity has moved always come too late: it was never
 * {@code onGround}, so it kept falling, could not jump and slipped through.
 * Here the entity's own movement treats nearby hulls like terrain, with the
 * same axis order and step-up as vanilla, which also makes jumping, sneaking
 * and fall damage behave on a deck exactly as they do on the ground.</p>
 */
public final class DriveableCollisionWorld
{
    private static final double REACH_MARGIN = 1.0E-3D;
    private static final double MOVEMENT_EPSILON = 1.0E-7D;
    // The probe ServerGamePacketListenerImpl#noBlocksAround uses for its floating check.
    private static final double FLOATING_PROBE_INFLATE = 0.0625D;
    private static final double FLOATING_PROBE_DEPTH = 0.55D;

    private DriveableCollisionWorld() {}

    /** Whether an entity's movement treats driveable hulls as solid at all. */
    public static boolean collidesWithHulls(@Nullable Entity entity)
    {
        return entity != null && !entity.noPhysics && !entity.isSpectator() && !entity.isPassenger()
            && !(entity instanceof Driveable) && !(entity instanceof Seat) && !(entity instanceof Wheel)
            && !(entity instanceof Shootable) && !(entity instanceof Projectile)
            && !(entity instanceof AbstractMinecart) && !(entity instanceof HangingEntity)
            && !(entity instanceof FallingBlockEntity);
    }

    /**
     * Whether this side simulates the entity's movement. A server replays
     * player-driven movement from packets and must not collide it against its
     * own hull pose, which lags or leads what that player's client saw and
     * would reject the move as wrong. The client simulates its own player,
     * whatever that player controls, and dropped items.
     */
    public static boolean isSimulatedHere(@NotNull Entity entity)
    {
        LivingEntity controller = entity.getControllingPassenger();
        if (!entity.level().isClientSide)
            return !(entity instanceof Player) && !(controller instanceof Player);
        return entity instanceof ItemEntity || ClientHooks.PLAYER.isLocalPlayer(entity)
            || controller != null && ClientHooks.PLAYER.isLocalPlayer(controller);
    }

    /**
     * Resolves an entity's movement against blocks, entities and nearby
     * driveable hulls, or returns {@code null} to leave it to vanilla when no
     * hull is in reach. A hull the entity is trapped inside is not one of them,
     * so it can move out again; see {@link DriveableHullEscape}.
     */
    @Nullable
    public static Vec3 collide(@NotNull Entity entity, @NotNull Vec3 movement)
    {
        Level level = entity.level();
        LevelHulls hulls = hulls(level);
        if (hulls == null || hulls.isEmpty() || !collidesWithHulls(entity)
            || !level.isClientSide && !isSimulatedHere(entity))
            return null;
        AABB box = entity.getBoundingBox();
        AABB reach = box.expandTowards(movement).expandTowards(0D, Math.max(0F, entity.getStepHeight()), 0D)
            .inflate(REACH_MARGIN);
        List<DriveableHullGeometry> nearby = hulls.collect(entity, reach);
        return nearby.isEmpty() ? null : collide(entity, movement, box, nearby);
    }

    /** Whether a box overlaps a hull the entity collides with, for vanilla probes that only look for blocks. */
    public static boolean intersectsHull(@NotNull Entity entity, @NotNull AABB box)
    {
        LevelHulls hulls = hulls(entity.level());
        return hulls != null && !hulls.isEmpty() && collidesWithHulls(entity) && hulls.intersects(entity, box);
    }

    /** Whether a box overlaps any active hull in the level, regardless of which entity would occupy it. */
    public static boolean intersectsAnyHull(@NotNull Level level, @NotNull AABB box)
    {
        LevelHulls hulls = hulls(level);
        return hulls != null && !hulls.isEmpty() && hulls.intersects(null, box);
    }

    /** Whether the entity stands on a hull, by the same probe the server's floating check uses for blocks. */
    public static boolean isStandingOnHull(@NotNull Entity entity)
    {
        return intersectsHull(entity, entity.getBoundingBox().inflate(FLOATING_PROBE_INFLATE)
            .expandTowards(0D, -FLOATING_PROBE_DEPTH, 0D));
    }

    /** Moves an entity against blocks only, for a displacement the hull itself imposes. */
    public static void moveIgnoringHulls(@NotNull Entity entity, double x, double y, double z)
    {
        Vec3 allowed = Entity.collideBoundingBox(entity, new Vec3(x, y, z), entity.getBoundingBox(), entity.level(),
            List.of());
        if (allowed.lengthSqr() > 0D)
            entity.setPos(entity.getX() + allowed.x, entity.getY() + allowed.y, entity.getZ() + allowed.z);
    }

    @Nullable
    static LevelHulls hulls(@Nullable Level level)
    {
        return level instanceof DriveableHullLevel access ? access.flansmodultimate$getDriveableHulls() : null;
    }

    /** {@code Entity#collide} with the hulls clipped after the voxel shapes on every axis pass. */
    private static Vec3 collide(Entity entity, Vec3 movement, AABB box, List<DriveableHullGeometry> hulls)
    {
        Level level = entity.level();
        List<VoxelShape> entityShapes = level.getEntityCollisions(entity, box.expandTowards(movement));
        Vec3 result = movement.lengthSqr() == 0D ? movement
            : collideBoundingBox(entity, movement, box, level, entityShapes, hulls);
        boolean blockedX = movement.x != result.x;
        boolean blockedY = movement.y != result.y;
        boolean blockedZ = movement.z != result.z;
        boolean grounded = entity.onGround() || blockedY && movement.y < 0D;
        float stepHeight = entity.getStepHeight();
        if (stepHeight > 0F && grounded && (blockedX || blockedZ))
        {
            Vec3 stepped = collideBoundingBox(entity, new Vec3(movement.x, stepHeight, movement.z), box, level,
                entityShapes, hulls);
            Vec3 rise = collideBoundingBox(entity, new Vec3(0D, stepHeight, 0D),
                box.expandTowards(movement.x, 0D, movement.z), level, entityShapes, hulls);
            if (rise.y < stepHeight)
            {
                Vec3 across = collideBoundingBox(entity, new Vec3(movement.x, 0D, movement.z), box.move(rise), level,
                    entityShapes, hulls).add(rise);
                if (across.horizontalDistanceSqr() > stepped.horizontalDistanceSqr())
                    stepped = across;
            }
            if (stepped.horizontalDistanceSqr() > result.horizontalDistanceSqr())
                return stepped.add(collideBoundingBox(entity, new Vec3(0D, -stepped.y + movement.y, 0D),
                    box.move(stepped), level, entityShapes, hulls));
        }
        return result;
    }

    private static Vec3 collideBoundingBox(Entity entity, Vec3 movement, AABB box, Level level,
                                           List<VoxelShape> entityShapes, List<DriveableHullGeometry> hulls)
    {
        AABB swept = box.expandTowards(movement);
        ImmutableList.Builder<VoxelShape> shapes = ImmutableList.builderWithExpectedSize(entityShapes.size() + 1);
        shapes.addAll(entityShapes);
        WorldBorder border = level.getWorldBorder();
        if (border.isInsideCloseToBorder(entity, swept))
            shapes.add(border.getCollisionShape());
        shapes.addAll(level.getBlockCollisions(entity, swept));
        return collideWithShapes(movement, box, shapes.build(), hulls);
    }

    private static Vec3 collideWithShapes(Vec3 movement, AABB box, List<VoxelShape> shapes,
                                          List<DriveableHullGeometry> hulls)
    {
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        if (y != 0D)
        {
            y = collideAxis(Direction.Axis.Y, box, shapes, hulls, y);
            if (y != 0D)
                box = box.move(0D, y, 0D);
        }
        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0D)
        {
            z = collideAxis(Direction.Axis.Z, box, shapes, hulls, z);
            if (z != 0D)
                box = box.move(0D, 0D, z);
        }
        if (x != 0D)
        {
            x = collideAxis(Direction.Axis.X, box, shapes, hulls, x);
            if (!zFirst && x != 0D)
                box = box.move(x, 0D, 0D);
        }
        if (!zFirst && z != 0D)
            z = collideAxis(Direction.Axis.Z, box, shapes, hulls, z);
        return new Vec3(x, y, z);
    }

    private static double collideAxis(Direction.Axis axis, AABB box, List<VoxelShape> shapes,
                                      List<DriveableHullGeometry> hulls, double desired)
    {
        double allowed = shapes.isEmpty() ? desired : Shapes.collide(axis, box, shapes, desired);
        int index = axis == Direction.Axis.X ? 0 : axis == Direction.Axis.Y ? 1 : 2;
        for (DriveableHullGeometry hull : hulls)
        {
            if (Math.abs(allowed) < MOVEMENT_EPSILON)
                return 0D;
            allowed = hull.clip(index, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, allowed);
        }
        return allowed;
    }

    /** Driveables with collision hulls in one level. Only that level's thread touches it. */
    public static final class LevelHulls
    {
        private final List<DriveableCollisionHelper> helpers = new ArrayList<>();

        boolean isEmpty()
        {
            return helpers.isEmpty();
        }

        void add(DriveableCollisionHelper helper)
        {
            if (!helpers.contains(helper))
                helpers.add(helper);
        }

        void remove(DriveableCollisionHelper helper)
        {
            helpers.remove(helper);
        }

        private List<DriveableHullGeometry> collect(Entity entity, AABB reach)
        {
            List<DriveableHullGeometry> nearby = List.of();
            for (int index = helpers.size() - 1; index >= 0; index--)
            {
                DriveableCollisionHelper helper = liveHelper(index);
                if (helper == null || !helper.geometry().mayTouch(reach.minX, reach.minY, reach.minZ, reach.maxX,
                    reach.maxY, reach.maxZ) || helper.owner().isPartOfThis(entity)
                    || helper.escape().isSuspended(entity))
                    continue;
                if (nearby.isEmpty())
                    nearby = new ArrayList<>(2);
                nearby.add(helper.geometry());
            }
            return nearby;
        }

        private boolean intersects(@Nullable Entity entity, AABB box)
        {
            for (int index = helpers.size() - 1; index >= 0; index--)
            {
                DriveableCollisionHelper helper = liveHelper(index);
                if (helper != null && helper.geometry().intersects(box.minX, box.minY, box.minZ, box.maxX, box.maxY,
                    box.maxZ) && !helper.owner().isPartOfThis(entity))
                    return true;
            }
            return false;
        }

        /** Returns the helper at {@code index}, dropping it instead when its driveable is gone. */
        @Nullable
        private DriveableCollisionHelper liveHelper(int index)
        {
            DriveableCollisionHelper helper = helpers.get(index);
            Driveable owner = helper.owner();
            if (owner != null && !owner.isRemoved())
                return helper;
            helpers.remove(index);
            helper.forgetRegistry();
            return null;
        }
    }
}
