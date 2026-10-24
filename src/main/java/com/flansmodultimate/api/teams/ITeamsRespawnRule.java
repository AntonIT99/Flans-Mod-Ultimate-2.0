/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.teams;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;

/**
 * Decides whether a player may take the field again in a Teams round, for example while their side has tickets left.
 * Register one with {@link FlansTeams#addRespawnRule}. Rules are asked on the server thread before the player is placed,
 * and a player comes back only when every rule allows it; one held back stays on their team as a spectator until the
 * round ends. Rules are never asked when a round starts, nor when the auto-balancer or a game type moves a player. A rule
 * that throws is logged and counts as allowing.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface ITeamsRespawnRule
{
    /** Why a player is about to take the field. */
    enum Reason
    {
        /** The player died and respawns. */
        DIED,
        /** The player joined a team while the round was running, from no team or from the spectators. */
        JOINED,
        /** The player changed sides without dying, as a creative player or one a game type shields from damage. */
        CHANGED_TEAM
    }

    /**
     * @param round
     *            the playing in progress
     * @param player
     *            the player about to take the field
     * @param team
     *            the team the player plays for, any pending choice of team already applied
     * @param reason
     *            why the player takes the field
     * @return whether the player may take the field; a rule that allows it may also charge for it here
     */
    boolean mayRespawn(ITeamsLiveRound round, ServerPlayer player, ITeam team, Reason reason);
}
