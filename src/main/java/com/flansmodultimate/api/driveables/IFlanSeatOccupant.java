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

    /**
     * Read on the client render thread while riding. An occupant whose seat supplies its body yaw can opt out of the
     * vanilla living-mount body clamp, which would otherwise turn its torso back towards the mount's body instead of
     * its seat or gun. Does not change the sitting animation or head rotation.
     *
     * @return whether the renderer should preserve this occupant's own body yaw while mounted
     */
    default boolean flansmodultimateHasSeatBodyRotation()
    {
        return false;
    }
}
