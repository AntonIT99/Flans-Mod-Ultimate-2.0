/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * Ground-supported model tilt, readable on either side. Yaw remains the movement controller's heading.
 *
 * @param pitch
 *            degrees nose down, negative nose up
 * @param bank
 *            degrees rolled right, with the right side down
 */
@ApiStatus.Experimental
public record TerrainAttitude(float pitch, float bank)
{
    /** A model resting level. */
    public static final TerrainAttitude LEVEL = new TerrainAttitude(0F, 0F);
}
