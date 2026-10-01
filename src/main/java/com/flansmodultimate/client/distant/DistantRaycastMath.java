package com.flansmodultimate.client.distant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.Vec3;

/** Turns a far-terrain raycast hit, which names a terrain column rather than a point, into a distance. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantRaycastMath
{
    /**
     * How far a ray from {@code origin} travels before it enters the column of terrain at
     * ({@code blockX}, {@code blockZ}) spanning {@code bottomY} to {@code topY}.
     *
     * <p>A Distant Horizons raycast steps one block at a time and rounds its position, so the column it reports
     * can lie up to a block beside the ray: the column is widened by that much. When the ray still misses it,
     * the ray's closest approach to the column's centre is used instead.</p>
     *
     * @param direction unit vector
     * @return distance in blocks, never negative
     */
    public static double entryDistance(Vec3 origin, Vec3 direction, int blockX, int bottomY, int topY, int blockZ)
    {
        double minY = Math.min(bottomY, topY);
        double maxY = Math.max(minY + 1D, Math.max(bottomY, topY));
        double[] min = {blockX - 0.5D, minY, blockZ - 0.5D};
        double[] max = {blockX + 1.5D, maxY, blockZ + 1.5D};
        double[] start = {origin.x, origin.y, origin.z};
        double[] step = {direction.x, direction.y, direction.z};

        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        boolean missed = false;
        for (int axis = 0; axis < 3 && !missed; axis++)
        {
            if (Math.abs(step[axis]) < 1.0E-9D)
            {
                missed = start[axis] < min[axis] || start[axis] > max[axis];
                continue;
            }
            double near = (min[axis] - start[axis]) / step[axis];
            double far = (max[axis] - start[axis]) / step[axis];
            enter = Math.max(enter, Math.min(near, far));
            exit = Math.min(exit, Math.max(near, far));
        }

        if (!missed && enter <= exit && exit >= 0D)
            return Math.max(0D, enter);

        Vec3 centre = new Vec3(blockX + 0.5D, (minY + maxY) * 0.5D, blockZ + 0.5D);
        return Math.max(0D, centre.subtract(origin).dot(direction));
    }
}
