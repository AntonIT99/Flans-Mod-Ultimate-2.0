package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.common.entity.AAGun;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A single rotating AA gun box using the same movement geometry as driveable hulls. */
public final class AAGunCollisionHelper
{
    private static final double CONTACT_SKIN = 1.0E-4D;
    private static final double MAX_CARRY = 3D;

    private final DriveableHullGeometry geometry;
    private final double[] bounds = new double[6];
    private final double[] vector = new double[4];
    private final AAGun owner;
    @Nullable
    private DriveableCollisionWorld.LevelHulls registry;
    private int lastTick = Integer.MIN_VALUE;

    public AAGunCollisionHelper(AAGun owner, float width, float height)
    {
        this.owner = owner;
        geometry = new DriveableHullGeometry(DriveableCollisionProfile.aaGun(width, height));
    }

    AAGun owner()
    {
        return owner;
    }

    DriveableHullGeometry geometry()
    {
        return geometry;
    }

    public Vec3 clipSegment(Vec3 start, Vec3 end, double inflation)
    {
        if (!geometry.hasActiveShapes())
            geometry.update(owner.getX(), owner.getY(), owner.getZ(), owner.getGunYaw(), 0F, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, false);
        return geometry.clipSegment(start, end, inflation);
    }

    public double[] copyWorldVertices()
    {
        return geometry.copyCurrentVertices(0);
    }

    void forgetRegistry()
    {
        registry = null;
    }

    public void unregister()
    {
        if (registry != null)
            registry.remove(this);
        registry = null;
    }

    public void tick()
    {
        if (owner.isRemoved())
            return;
        boolean continuous = lastTick == owner.tickCount - 1;
        lastTick = owner.tickCount;
        geometry.update(owner.getX(), owner.getY(), owner.getZ(), owner.getGunYaw(), 0F, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> true, continuous);
        DriveableCollisionWorld.LevelHulls target = DriveableCollisionWorld.hulls(owner.level());
        if (registry != target)
        {
            unregister();
            if (target != null)
            {
                target.add(this);
                registry = target;
            }
        }
        if (!geometry.queryBounds(bounds))
            return;
        Level level = owner.level();
        AABB query = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]).inflate(1D);
        List<Entity> candidates = level.getEntities(owner, query,
            entity -> DriveableCollisionWorld.collidesWithHulls(entity) && !owner.isPassengerOfSameVehicle(entity) && (!level.isClientSide || DriveableCollisionWorld.isSimulatedHere(entity)));
        for (Entity entity : candidates)
            updateEntity(entity);
    }

    private void updateEntity(Entity entity)
    {
        AABB box = entity.getBoundingBox();
        int support = geometry.findSupport(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (support >= 0 && DriveableCollisionWorld.isSimulatedHere(entity))
        {
            double footX = (box.minX + box.maxX) * 0.5D;
            double footZ = (box.minZ + box.maxZ) * 0.5D;
            if (geometry.carryPoint(support, footX, box.minY, footZ, vector))
            {
                double dx = vector[0] - footX;
                double dy = vector[1] - box.minY;
                double dz = vector[2] - footZ;
                if (dx * dx + dy * dy + dz * dz <= MAX_CARRY * MAX_CARRY)
                {
                    DriveableCollisionWorld.moveIgnoringHulls(entity, dx, dy, dz);
                    float yaw = geometry.carryYaw(support);
                    entity.setYRot(entity.getYRot() + yaw);
                    entity.setYHeadRot(entity.getYHeadRot() + yaw);
                    entity.resetFallDistance();
                }
            }
            box = entity.getBoundingBox();
        }
        if (!DriveableCollisionWorld.isSimulatedHere(entity) || !geometry.findPenetration(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, support >= 0, vector)
            || vector[3] <= CONTACT_SKIN)
            return;
        double distance = Math.min(0.75D, vector[3] + CONTACT_SKIN);
        DriveableCollisionWorld.moveIgnoringHulls(entity, vector[0] * distance, vector[1] * distance, vector[2] * distance);
        if (vector[1] >= DriveableHullGeometry.MIN_SUPPORT_NORMAL_Y)
        {
            entity.setOnGround(true);
            entity.resetFallDistance();
        }
    }
}
