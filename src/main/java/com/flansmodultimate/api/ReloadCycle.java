/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalInt;

/**
 * How an AA gun or a driveable weapon bank alternates between firing and reloading with a given ammunition, following the
 * same rules as the placed AA gun or driveable.
 *
 * @param volleys
 *            triggers before a reload: one trigger fires every barrel the definition fires together, so an alternating AA
 *            gun counts one barrel per trigger; at least one
 * @param shotDelay
 *            ticks between triggers within the magazine, possibly fractional
 * @param reloadTicks
 *            ticks spent reloading after the last trigger, already including the ammunition's reload time multiplier and
 *            never shorter than {@code shotDelay}
 * @param chamberSoundTick
 *            remaining ticks before a trigger at which the definition plays its chambering sound
 *            ({@code ShootReloadSound}), or empty when it declares none
 */
@ApiStatus.Experimental
public record ReloadCycle(int volleys, double shotDelay, double reloadTicks, OptionalInt chamberSoundTick)
{
    /** Clamps the values into their documented ranges. */
    public ReloadCycle
    {
        volleys = Math.max(1, volleys);
        shotDelay = Double.isFinite(shotDelay) ? Math.max(0D, shotDelay) : 0D;
        reloadTicks = Double.isFinite(reloadTicks) ? Math.max(shotDelay, reloadTicks) : shotDelay;
    }
}
