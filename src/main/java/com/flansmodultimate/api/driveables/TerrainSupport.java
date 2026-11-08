/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * One wheel sample of a driveable model: the tilt it is drawn at and what the terrain beneath it actually carries.
 * Both come out of the same probes, so that a caller needing each pays for one pass.
 *
 * @param attitude
 *            the tilt and lift the supported wheels give the model
 * @param support
 *            how much of the wheel footprint that sample found support for
 */
@ApiStatus.Experimental
public record TerrainSupport(TerrainAttitude attitude, WheelSupport support)
{
    /** A model resting level on ground that was not sampled. */
    public static final TerrainSupport LEVEL = new TerrainSupport(TerrainAttitude.LEVEL, WheelSupport.UNKNOWN);
}
