/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

/**
 * The motion of an aircraft coming down, carried from one tick to the next.
 *
 * @param velocity
 *            world velocity in blocks a tick
 * @param rotation
 *            how the aircraft turns this tick, as a world-space rotation vector: its direction is the axis, by the
 *            right-hand rule, and its length the angle in degrees
 */
@ApiStatus.Experimental
public record CrashMotion(Vec3 velocity, Vec3 rotation)
{
    /** Starts a fall with the velocity the aircraft had and no turn of its own yet. */
    public static CrashMotion of(Vec3 velocity)
    {
        return new CrashMotion(velocity, Vec3.ZERO);
    }
}
