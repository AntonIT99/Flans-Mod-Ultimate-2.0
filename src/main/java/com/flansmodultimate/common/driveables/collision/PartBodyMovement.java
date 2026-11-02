package com.flansmodultimate.common.driveables.collision;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Moves a posed part body through voxel boxes using the hull's exact convex collision tests. */
final class PartBodyMovement
{
    private PartBodyMovement()
    {}

    static Vec3 collide(DriveableHullGeometry hull, List<AABB> obstacles, Vec3 offset, Vec3 movement, double stepHeight, boolean onGround)
    {
        Vec3 result = sweep(hull, obstacles, offset, movement);
        boolean grounded = onGround || movement.y < 0D && result.y != movement.y;
        if (stepHeight > 0D && grounded && (result.x != movement.x || result.z != movement.z))
        {
            Vec3 rise = sweep(hull, obstacles, offset, new Vec3(0D, stepHeight, 0D));
            Vec3 across = sweep(hull, obstacles, offset.add(rise), new Vec3(movement.x, 0D, movement.z));
            Vec3 stepped = rise.add(across);
            if (stepped.horizontalDistanceSqr() > result.horizontalDistanceSqr())
                return stepped.add(sweep(hull, obstacles, offset.add(stepped), new Vec3(0D, movement.y - stepped.y, 0D)));
        }
        return result;
    }

    private static Vec3 sweep(DriveableHullGeometry hull, List<AABB> obstacles, Vec3 offset, Vec3 movement)
    {
        double y = clip(hull, obstacles, offset, 1, movement.y);
        offset = offset.add(0D, y, 0D);
        double x = movement.x;
        double z = movement.z;
        if (Math.abs(x) < Math.abs(z))
        {
            z = clip(hull, obstacles, offset, 2, z);
            x = clip(hull, obstacles, offset.add(0D, 0D, z), 0, x);
        }
        else
        {
            x = clip(hull, obstacles, offset, 0, x);
            z = clip(hull, obstacles, offset.add(x, 0D, 0D), 2, z);
        }
        return new Vec3(x, y, z);
    }

    private static double clip(DriveableHullGeometry hull, List<AABB> obstacles, Vec3 offset, int axis, double desired)
    {
        // Relative motion: moving a hull past a voxel is the voxel moving the opposite way past the hull.
        for (AABB obstacle : obstacles)
        {
            AABB box = obstacle.move(-offset.x, -offset.y, -offset.z);
            desired = -hull.clip(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, -desired);
        }
        return desired;
    }
}
