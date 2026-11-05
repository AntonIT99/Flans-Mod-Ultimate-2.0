package com.wolffsmod.npcs.drivers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a driveable can drive straight from where it is to a point along its path without its hull running into
 * terrain on the way. The path search only promises the way along the path itself; steering for a point further along
 * cuts the corners in between, which takes a wide hull into the walls the path went round. No world access of its own.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DriveCorridor
{
    /** Blocks between samples along the corridor. */
    static final double STEP = 0.5D;
    /** Most blocks between samples across the corridor: less than a block, so no block across it slips between them. */
    static final double ACROSS = 0.75D;
    /** Blocks kept off each side of the hull, so that a corridor brushing the face of a wall does not count as blocked. */
    static final double INSET = 0.25D;
    /** Layers above the ground that have to be clear: a single block is stepped over, a wall two blocks high is not. */
    static final int LAYERS = 2;

    /** Whether the block at a position has something solid to run into. */
    @FunctionalInterface
    interface Solid
    {
        boolean at(int x, int y, int z);
    }

    /**
     * @param from
     *            where the hull stands, at the height of its feet
     * @param to
     *            the point it would steer for, at the height of the feet of the path there
     * @param width
     *            blocks the hull measures across
     * @return whether nothing solid stands between the two at the height of the hull, across its whole width
     */
    static boolean clear(Vec3 from, Vec3 to, double width, Solid solid)
    {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3D)
            return true;
        double sideX = -dz / length;
        double sideZ = dx / length;
        double half = Math.max(0D, width * 0.5D - INSET);
        int lanes = Math.max(1, Mth.ceil(half * 2D / ACROSS));
        int steps = Math.max(1, Mth.ceil(length / STEP));
        for (int lane = 0; lane <= lanes; lane++)
        {
            double offset = -half + half * 2D * lane / lanes;
            int lastX = Integer.MIN_VALUE;
            int lastZ = Integer.MIN_VALUE;
            int lastGround = Integer.MIN_VALUE;
            // The start is where the hull already stands, which it does not have to get past.
            for (int step = 1; step <= steps; step++)
            {
                double share = (double) step / steps;
                int x = Mth.floor(from.x + dx * share + sideX * offset);
                int z = Mth.floor(from.z + dz * share + sideZ * offset);
                int ground = Mth.floor(from.y + (to.y - from.y) * share + 1.0E-3D);
                if (x == lastX && z == lastZ && ground == lastGround)
                    continue;
                lastX = x;
                lastZ = z;
                lastGround = ground;
                for (int layer = 1; layer <= LAYERS; layer++)
                {
                    if (solid.at(x, ground + layer, z))
                        return false;
                }
            }
        }
        return true;
    }
}
