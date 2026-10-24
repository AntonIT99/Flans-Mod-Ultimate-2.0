/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * Extra transform in the held item's local display frame, before its authored model transform: offsets in model pixels
 * (16 per block), followed by X, Y and Z rotations in degrees. Positive axes follow the display pose stack, without automatic
 * left-hand mirroring. Immutable and safe on either side. Offsets are limited to 64 pixels and rotations to 180 degrees.
 */
@ApiStatus.Experimental
public record HeldItemTransform(float x, float y, float z, float rotationX, float rotationY, float rotationZ)
{
    /** No adjustment to the authored placement. */
    public static final HeldItemTransform IDENTITY = new HeldItemTransform(0, 0, 0, 0, 0, 0);

    /** Rejects nonfinite inputs and bounds adjustments to the supported editor range. */
    public HeldItemTransform
    {
        x = bounded(x, 64);
        y = bounded(y, 64);
        z = bounded(z, 64);
        rotationX = bounded(rotationX, 180);
        rotationY = bounded(rotationY, 180);
        rotationZ = bounded(rotationZ, 180);
    }

    private static float bounded(float value, float limit)
    {
        if (!Float.isFinite(value))
            throw new IllegalArgumentException("Held item transforms must be finite");
        return Math.max(-limit, Math.min(limit, value));
    }
}
