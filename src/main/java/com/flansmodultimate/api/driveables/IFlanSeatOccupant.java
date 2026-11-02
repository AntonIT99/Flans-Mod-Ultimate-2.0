/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.player.Player;

/**
 * A non-player entity that sits in driveable seats the way a player does. Seats place a player, and an entity that
 * implements this, so that a player-shaped body sits on the seat as the content pack authored it; any other rider sits
 * with its feet on the seat anchor. Read on both sides while the entity rides a seat.
 */
@ApiStatus.Experimental
public interface IFlanSeatOccupant
{
    /**
     * Read-only availability check on the server thread. An addon may offer an allied player's requested seat even while
     * occupied. Deployment checks may use a separate setting. Returning false preserves exclusive occupancy.
     *
     * @param player
     *            the requesting player
     * @param deployment
     *            whether the request comes from Teams vehicle-spawner deployment rather than direct interaction
     * @return whether this occupant is willing to move aside; does not move anyone
     */
    default boolean flansmodultimateMayYieldSeat(Player player, boolean deployment)
    {
        return false;
    }

    /**
     * Gives up the requested seat on the server thread, after the mount has checked access and entry permissions.
     * Move to another free seat or dismount, and prevent automatic immediate reboarding. Called only for an occupant
     * that offered the seat through {@link #flansmodultimateMayYieldSeat(Player, boolean)}.
     *
     * @param player
     *            the requesting player
     * @param deployment
     *            whether this is a Teams vehicle-spawner deployment
     * @return whether the original seat was vacated
     */
    default boolean flansmodultimateYieldSeat(Player player, boolean deployment)
    {
        return false;
    }

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
