/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.client.teams.TeamsClientState;
import org.jetbrains.annotations.ApiStatus;

/**
 * The Teams round as the local player sees it, from the state the server sends. Client only, on the client thread.
 */
@ApiStatus.Experimental
public final class FlansTeamsClient
{
    private FlansTeamsClient()
    {}

    /**
     * @return whether the local player plays in a running round on a team, rather than spectating or waiting
     */
    public static boolean isLocalPlayerPlaying()
    {
        return TeamsClientState.isLocalPlayerPlaying();
    }

    /**
     * @param team
     *            any team
     * @return whether the team plays on the local player's side in the running round; false between rounds, while
     *         the local player spectates and for teams outside the round
     */
    public static boolean isAllyOfLocalPlayer(ITeam team)
    {
        return TeamsClientState.isAllyOfLocalPlayer(team.getShortName());
    }
}
