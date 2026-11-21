/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import com.flansmodultimate.api.projectiles.BallisticDrag;
import org.jetbrains.annotations.ApiStatus;

/**
 * How what a weapon fires flies. Each tick a projectile moves by its velocity, then its velocity is multiplied by the drag
 * and by its quadratic drag retention ({@link BallisticDrag#retention}), and lowered by the gravity.
 *
 * @param speed
 *            launch speed in blocks per tick along the weapon's direction; 0 for a shot that hits along its line at once
 * @param gravity
 *            blocks per tick taken off the vertical velocity each tick
 * @param drag
 *            share of its velocity a projectile keeps each tick regardless of its speed, from 0 to 1
 * @param dropped
 *            whether it is dropped, as a bomb is, leaving at the velocity of what drops it rather than at its speed
 * @param quadraticDrag
 *            speed-dependent drag coefficient in inverse blocks, 0 for none; see {@link BallisticDrag}
 */
@ApiStatus.Experimental
public record WeaponBallistics(double speed, double gravity, double drag, boolean dropped, double quadraticDrag)
{
    /** Ballistics without speed-dependent drag. */
    public WeaponBallistics(double speed, double gravity, double drag, boolean dropped)
    {
        this(speed, gravity, drag, dropped, 0D);
    }

    /**
     * The constant per-tick retention to aim with at a target {@code distance} blocks away, folding the quadratic drag in;
     * see {@link BallisticDrag#effectiveRetention}.
     */
    public double drag(double distance)
    {
        return BallisticDrag.effectiveRetention(drag, quadraticDrag, speed, distance);
    }
}
