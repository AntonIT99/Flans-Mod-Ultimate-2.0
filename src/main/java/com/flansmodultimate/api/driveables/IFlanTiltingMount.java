/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * A living mount, other than a driveable seat, whose drawn body pitches and rolls under its riders. Riders are drawn,
 * and player hitboxes shaped, tilted with it about their own feet, as riders of driveables are.
 */
@ApiStatus.Experimental
public interface IFlanTiltingMount
{
    /**
     * Read on the client render thread while ridden, and wherever player hitboxes are posed.
     *
     * @param partialTick
     *            the frame's partial tick
     * @return the tilt of the drawn body in its own frame, after the renderer's {@code 180 - body yaw} turn; null while
     *         level
     */
    @Nullable
    Quaternionf flansmodultimateRiderTilt(float partialTick);
}
