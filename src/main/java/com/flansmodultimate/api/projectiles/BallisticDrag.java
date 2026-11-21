/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/**
 * Speed-dependent air drag of rounds.
 *
 * <p>
 * Besides the constant per-tick retention every round has, a round can have a quadratic drag coefficient {@code k}, in
 * inverse blocks. Each tick in air its velocity is then multiplied by {@code 1 / (1 + k * speed)} before gravity is
 * applied. Without gravity this is exactly {@code speed(n) = v0 / (1 + n * k * v0)} after {@code n} ticks, and the speed
 * after a distance {@code d} is very nearly {@code v0 * exp(-k * d)}.
 *
 * <p>
 * The aim solvers of this mod assume a constant retention. {@link #effectiveRetention} gives the constant retention that
 * covers a given distance in the same time as the quadratic drag does, so those solvers stay accurate at that range.
 */
@ApiStatus.Experimental
public final class BallisticDrag
{
    private static final int SOLVE_ITERATIONS = 40;

    private BallisticDrag()
    {}

    /** Share of its velocity a round at {@code speed} blocks per tick keeps over one tick of quadratic drag. */
    public static double retention(double quadraticDrag, double speed)
    {
        if (!positive(quadraticDrag) || !positive(speed))
            return 1D;
        return 1D / (1D + quadraticDrag * speed);
    }

    /** Speed in blocks per tick after flying {@code distance} blocks, ignoring gravity. */
    public static double speedAt(double speed, double quadraticDrag, double distance)
    {
        if (!positive(quadraticDrag) || !positive(distance))
            return speed;
        return speed * Math.exp(-quadraticDrag * distance);
    }

    /** Ticks a round launched at {@code speed} blocks per tick takes to fly {@code distance} blocks, ignoring gravity. */
    public static double flightTicks(double speed, double quadraticDrag, double distance)
    {
        if (!positive(speed) || !positive(distance))
            return 0D;
        if (!positive(quadraticDrag))
            return distance / speed;
        return Math.expm1(quadraticDrag * distance) / (quadraticDrag * speed);
    }

    /**
     * The constant per-tick retention that, combined with {@code retention}, carries a round launched at {@code speed} over
     * {@code distance} blocks in the same time as quadratic drag {@code quadraticDrag} does.
     *
     * @param retention
     *            the round's constant per-tick retention, 0 to 1
     * @return {@code retention} itself when there is no quadratic drag or nothing to fit
     */
    public static double effectiveRetention(double retention, double quadraticDrag, double speed, double distance)
    {
        if (!positive(quadraticDrag) || !positive(speed) || !positive(distance))
            return retention;
        double ticks = flightTicks(speed, quadraticDrag, distance);
        double target = distance / speed;
        if (ticks <= target + 1.0E-9D)
            return retention;
        // Distance over speed covered in t ticks at constant retention r is (1 - r^t) / (1 - r), increasing in r.
        double low = 0D;
        double high = 1D;
        for (int i = 0; i < SOLVE_ITERATIONS; i++)
        {
            double middle = (low + high) * 0.5D;
            if (coveredTicks(middle, ticks) < target)
                low = middle;
            else
                high = middle;
        }
        return Math.max(0D, Math.min(1D, retention)) * (low + high) * 0.5D;
    }

    /** False for zero, negative and NaN values alike. */
    private static boolean positive(double value)
    {
        return value > 0D;
    }

    private static double coveredTicks(double retention, double ticks)
    {
        if (retention > 1D - 1.0E-9D)
            return ticks;
        return (1D - Math.pow(retention, ticks)) / (1D - retention);
    }
}
