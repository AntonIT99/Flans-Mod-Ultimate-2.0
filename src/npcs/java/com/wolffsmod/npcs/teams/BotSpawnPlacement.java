package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.api.teams.FlansTeams;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

/** Bounded, loaded-chunk-only placement against terrain and complete vehicle footprints. Server thread only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BotSpawnPlacement
{
    static final String SOURCE_SPAWNER = "wolffsmodnpcs_source_spawner";
    private static final String DEPLOYMENT = "wolffsmodnpcs_deployment";
    private static final int VEHICLE_RADIUS = 64;
    private static final int SOLDIER_RADIUS = 8;
    private static final int[] HEIGHTS = {0, 1, -1, 2, -2, 3, -3, 4, -4};

    static Optional<Vec3> vehicle(ServerLevel level, FlanModelEntityType type, Vec3 origin, float yaw)
    {
        boolean ship = type.getInfoType() instanceof IDriveableType driveable && driveable.isShip();
        Vec3 start = ship ? FlansTeams.findShipSpawn(level, origin).orElse(null) : origin;
        if (start == null)
            return Optional.empty();
        return find(level, start, origin, vehicleBounds(type, yaw), ship, VEHICLE_RADIUS, 4);
    }

    static Optional<Vec3> soldier(ServerLevel level, Vec3 origin)
    {
        return find(level, origin, origin, new AABB(-0.3D, 0D, -0.3D, 0.3D, 1.95D, 0.3D), false, SOLDIER_RADIUS, 1);
    }

    static boolean stagingOccupied(ServerLevel level, UUID spawner, Vec3 origin)
    {
        for (Entity entity : level.getAllEntities())
        {
            var data = entity.getPersistentData();
            if (!entity.isAlive() || !data.hasUUID(SOURCE_SPAWNER) || !spawner.equals(data.getUUID(SOURCE_SPAWNER)))
                continue;
            var deployment = data.getCompound(DEPLOYMENT);
            Vec3 deployed = deployment.isEmpty() ? origin : new Vec3(deployment.getDouble("x"), deployment.getDouble("y"), deployment.getDouble("z"));
            AABB staging = new AABB(BlockPos.containing(deployed)).inflate(8D);
            if (obstacleBounds(entity).intersects(staging))
                return true;
        }
        return false;
    }

    static void recordSpawner(Entity entity, UUID spawner)
    {
        var data = entity.getPersistentData();
        data.putUUID(SOURCE_SPAWNER, spawner);
        var deployment = new net.minecraft.nbt.CompoundTag();
        deployment.putDouble("x", entity.getX());
        deployment.putDouble("y", entity.getY());
        deployment.putDouble("z", entity.getZ());
        data.put(DEPLOYMENT, deployment);
    }

    /** Successful usage takes priority over tactical distance; ties start at a random origin. */
    static List<Vec3> order(List<Vec3> origins, Map<Vec3, Integer> uses, @Nullable Vec3 target, RandomGenerator random)
    {
        List<Vec3> result = new ArrayList<>(new LinkedHashSet<>(origins));
        for (int index = result.size() - 1; index > 0; index--)
            Collections.swap(result, index, random.nextInt(index + 1));
        result.sort(Comparator.<Vec3>comparingInt(origin -> uses.getOrDefault(origin, 0)).thenComparingDouble(origin -> target == null ? 0D : origin.distanceToSqr(target)));
        return result;
    }

    static AABB vehicleBounds(FlanModelEntityType type, float yaw)
    {
        AABB bounds = type.getShape().dimensions().makeBoundingBox(Vec3.ZERO);
        if (type.getInfoType() instanceof IDriveableType driveable && driveable.getBounds().isPresent())
        {
            AABB hull = driveable.getBounds().orElseThrow().move(0D, type.getShape().modelHeight(), 0D);
            bounds = bounds.minmax(transform(hull, point -> point.yRot((float) Math.toRadians(180F - yaw))));
        }
        return bounds;
    }

    private static AABB obstacleBounds(Entity entity)
    {
        FlanModelEntity model = NpcModels.flan(entity);
        if (model != null && entity instanceof EntityNPCInterface npc && model.getInfoType() instanceof IDriveableType driveable && driveable.getBounds().isPresent())
            return entity.getBoundingBox().minmax(transform(driveable.getBounds().orElseThrow(), point -> NpcModels.world(npc, model, point, npc.yBodyRot)));
        // A driveable's vanilla box is deliberately capped for movement; reserve its full hull too.
        return FlansEntityTypes.getDriveableType(entity).flatMap(IDriveableType::getBounds).map(hull ->
        {
            double radius = Math.hypot(Math.max(Math.abs(hull.minX), Math.abs(hull.maxX)), Math.max(Math.abs(hull.minZ), Math.abs(hull.maxZ)));
            return entity.getBoundingBox()
                .minmax(new AABB(entity.getX() - radius, entity.getY() + hull.minY, entity.getZ() - radius, entity.getX() + radius, entity.getY() + hull.maxY, entity.getZ() + radius));
        }).orElseGet(entity::getBoundingBox);
    }

    private static AABB transform(AABB box, UnaryOperator<Vec3> transform)
    {
        AABB result = null;
        for (double x : new double[]{box.minX, box.maxX})
            for (double y : new double[]{box.minY, box.maxY})
                for (double z : new double[]{box.minZ, box.maxZ})
                {
                    Vec3 point = transform.apply(new Vec3(x, y, z));
                    AABB corner = new AABB(point, point);
                    result = result == null ? corner : result.minmax(corner);
                }
        return Objects.requireNonNull(result);
    }

    private static Optional<Vec3> find(ServerLevel level, Vec3 start, Vec3 origin, AABB local, boolean ship, int radius, int step)
    {
        List<AABB> obstacles = obstacles(level);
        for (int ring = 0; ring <= radius; ring += step)
            for (Vec3 column : ring(start, ring, step))
            {
                Optional<Vec3> position = column(level, column, origin, local, ship, obstacles);
                if (position.isPresent())
                    return position;
            }
        return Optional.empty();
    }

    private static List<AABB> obstacles(ServerLevel level)
    {
        List<AABB> obstacles = new ArrayList<>();
        for (Entity entity : level.getAllEntities())
            if (!entity.isRemoved() && !entity.isSpectator() && (entity instanceof LivingEntity && entity.isAlive() || entity.canBeCollidedWith() || FlansEntityTypes.isWreck(entity)))
                obstacles.add(obstacleBounds(entity));
        return obstacles;
    }

    private static List<Vec3> ring(Vec3 center, int radius, int step)
    {
        if (radius == 0)
            return List.of(center);
        List<Vec3> positions = new ArrayList<>();
        for (int edge = -radius; edge < radius; edge += step)
        {
            positions.add(center.add(edge, 0D, -radius));
            positions.add(center.add(radius, 0D, edge));
            positions.add(center.add(-edge, 0D, radius));
            positions.add(center.add(-radius, 0D, -edge));
        }
        return positions;
    }

    private static Optional<Vec3> column(ServerLevel level, Vec3 column, Vec3 origin, AABB local, boolean ship, List<AABB> obstacles)
    {
        for (int offset : HEIGHTS)
        {
            OptionalDouble surface = surface(level, column, offset, ship);
            if (surface.isPresent())
            {
                Vec3 feet = new Vec3(column.x, surface.getAsDouble() - (ship ? 0D : Math.min(0D, local.minY)), column.z);
                // A ship may start its search at water away from the plate, but still stays within the original 64-block limit.
                if ((!ship || feet.distanceToSqr(origin) <= (double) VEHICLE_RADIUS * VEHICLE_RADIUS) && fits(level, local.move(feet).deflate(0.01D), ship, obstacles))
                    return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static OptionalDouble surface(ServerLevel level, Vec3 column, int offset, boolean ship)
    {
        BlockPos support = BlockPos.containing(column.x, column.y + offset - 0.1D, column.z);
        if (!level.getChunkSource().hasChunk(support.getX() >> 4, support.getZ() >> 4) || level.isOutsideBuildHeight(support))
            return OptionalDouble.empty();
        var state = level.getBlockState(support);
        var shape = state.getCollisionShape(level, support);
        if (ship)
            return state.getFluidState().is(FluidTags.WATER) && shape.isEmpty() && level.getFluidState(support.above()).isEmpty() ? OptionalDouble.of(support.getY() + 1D) : OptionalDouble.empty();
        return !shape.isEmpty() && state.getFluidState().isEmpty() ? OptionalDouble.of(support.getY() + shape.max(net.minecraft.core.Direction.Axis.Y)) : OptionalDouble.empty();
    }

    private static boolean fits(ServerLevel level, AABB body, boolean ship, List<AABB> obstacles)
    {
        return loaded(level, body) && level.getWorldBorder().isWithinBounds(body) && (ship || !level.containsAnyLiquid(body)) && level.noCollision(body)
            && obstacles.stream().noneMatch(body.inflate(0.25D, 0D, 0.25D)::intersects);
    }

    private static boolean loaded(ServerLevel level, AABB body)
    {
        if (body.minY < level.getMinBuildHeight() || body.maxY >= level.getMaxBuildHeight())
            return false;
        for (int x = BlockPos.containing(body.minX, body.minY, body.minZ).getX() >> 4; x <= BlockPos.containing(body.maxX, body.maxY, body.maxZ).getX() >> 4; x++)
            for (int z = BlockPos.containing(body.minX, body.minY, body.minZ).getZ() >> 4; z <= BlockPos.containing(body.maxX, body.maxY, body.maxZ).getZ() >> 4; z++)
                if (!level.getChunkSource().hasChunk(x, z))
                    return false;
        return true;
    }
}
