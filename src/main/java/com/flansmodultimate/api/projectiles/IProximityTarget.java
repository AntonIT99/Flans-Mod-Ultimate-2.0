/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/** Lets a living entity represented as machinery use vehicle proximity fuses instead of infantry fuses. */
@ApiStatus.Experimental
public interface IProximityTarget
{
    /** @return whether this entity currently counts as a vehicle for proximity triggers; server thread */
    boolean flansmodultimateDriveableProximityTarget();
}
