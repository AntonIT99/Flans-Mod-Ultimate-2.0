/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * Where a turret, a seat gun or an AA gun points, relative to the hull it stands on.
 *
 * @param yaw
 *            degrees from the hull's forward direction, positive to the right, as for a Minecraft entity
 * @param pitch
 *            degrees from level, positive downwards, as for a Minecraft entity
 */
@ApiStatus.Experimental
public record MountAim(float yaw, float pitch)
{

    /** Pointing straight ahead and level. */
    public static final MountAim REST = new MountAim(0F, 0F);

    /** Replaces angles that are not numbers by the rest direction. */
    public MountAim
    {
        yaw = Float.isFinite(yaw) ? yaw : 0F;
        pitch = Float.isFinite(pitch) ? pitch : 0F;
    }
}
