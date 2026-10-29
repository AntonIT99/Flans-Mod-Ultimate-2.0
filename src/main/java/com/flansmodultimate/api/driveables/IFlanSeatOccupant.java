/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * A non-player entity that sits in driveable seats the way a player does. Seats place a player, and an entity that
 * implements this, so that a player-shaped body sits on the seat as the content pack authored it; any other rider sits
 * with its feet on the seat anchor. Read on both sides while the entity rides a seat.
 */
@ApiStatus.Experimental
public interface IFlanSeatOccupant
{
    /**
     * @return whether the entity is seated like a player at the moment
     */
    // The default of an extension point; implementations may decide per moment.
    @SuppressWarnings("SameReturnValue")
    default boolean flansmodultimateSeatsLikePlayer()
    {
        return true;
    }
}
