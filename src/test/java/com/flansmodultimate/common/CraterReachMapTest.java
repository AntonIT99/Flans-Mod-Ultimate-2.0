package com.flansmodultimate.common;

import com.flansmodultimate.common.explosions.CraterReachMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reach map is what keeps a crater round: whichever direction a block lies in, it is tested
 * against a reach interpolated from the rays around it, so the ray pattern cannot show through.
 */
class CraterReachMapTest
{
    @Test
    void aUniformReachIsTheSameInEveryDirection()
    {
        // Open, even ground: every ray reaches the crater radius, so the crater must be a sphere
        // of exactly that radius - no longer along the diagonals than along the axes.
        CraterReachMap map = new CraterReachMap(16);
        for (int cell = 0; cell < map.cellCount(); cell++)
            map.setReach(cell, 40F);

        for (double[] dir : new double[][] {{1, 0, 0}, {0, -1, 0}, {1, 1, 0}, {1, -1, 1}, {0.3, -0.9, 0.2}, {-2, 0.01, 1.99}})
            assertEquals(40F, map.reachTowards(dir[0], dir[1], dir[2]), 1e-4F);
    }

    @Test
    void eachRayDirectionReadsBackItsOwnReach()
    {
        CraterReachMap map = new CraterReachMap(8);
        for (int cell = 0; cell < map.cellCount(); cell++)
            map.setReach(cell, cell);

        double[] dir = new double[3];
        for (int cell = 0; cell < map.cellCount(); cell++)
        {
            map.cellDirection(cell, dir);
            assertEquals(cell, map.reachTowards(dir[0], dir[1], dir[2]), 1e-3F);
        }
    }

    @Test
    void theReachVariesSmoothlyBetweenRays()
    {
        // Rays pointing down reach less than rays pointing up (the ground absorbs them). A
        // direction between two rays gets a reach between theirs rather than jumping.
        CraterReachMap map = new CraterReachMap(16);
        double[] dir = new double[3];
        for (int cell = 0; cell < map.cellCount(); cell++)
        {
            map.cellDirection(cell, dir);
            map.setReach(cell, (float) (30D + 10D * dir[1]));
        }

        for (double y = -0.95D; y <= 0.95D; y += 0.05D)
        {
            double x = Math.sqrt(1D - y * y);
            assertEquals(30D + 10D * y, map.reachTowards(x, y, 0D), 0.5D);
        }
    }
}
