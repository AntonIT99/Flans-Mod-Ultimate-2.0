/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * One entry of the Teams rotation: the map, the game type and the teams that play it, grouped into sides. Teams on the
 * same side are allied. Sides are numbered from 0 in the order their first team is listed.
 *
 * <p>
 * The entry outlives each playing of it; {@link ITeamsLiveRound} describes the playing in progress. List the entries
 * through {@link FlansTeams#getRounds()}. Server thread only.
 * </p>
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ITeamsRound
{
    /**
     * @return the id of the rotation entry, stable across server restarts and every playing of the entry
     */
    UUID getId();

    /**
     * @return the short name of the map the entry is played on
     */
    String getMapId();

    /**
     * @return the id of the game type, such as {@code tdm}, {@code conquest} or {@code aslt}
     */
    String getGameTypeId();

    /**
     * @return the teams of the entry in round order, immutable; a team that is no longer loaded is left out
     */
    List<ITeam> getTeams();

    /**
     * @return how many sides fight each other
     */
    int getSideCount();

    /**
     * @param team
     *            any team
     * @return the side the team fights on, or -1 when it does not play this entry
     */
    int getSideOf(ITeam team);

    /**
     * @param side
     *            a side, from 0
     * @return the teams on that side in round order, immutable; empty for a side the entry does not have
     */
    List<ITeam> getTeamsOfSide(int side);

    /**
     * @return whether both teams play this entry on the same side; a playing team is its own ally
     */
    boolean areTeamsAllied(ITeam first, ITeam second);

    /**
     * @return the time limit of a playing of the entry, in minutes
     */
    int getTimeLimitMinutes();

    /**
     * @return the score a side needs to win, or the reinforcements of the attackers in Assault
     */
    int getScoreLimit();

    /**
     * @return the side, as sides are numbered here, that attacks in a Domination or Conquest round played attacker against
     *         defender: only it takes capture points, and the defenders cannot take back a point once lost; empty for a
     *         round every side captures in. Assault has attackers of its own, see {@link ITeamsLiveRound#getAttackingSide}.
     */
    OptionalInt getAttackingSide();

    /**
     * Reads data an addon saved with this entry under its own key, such as the settings of its own features for the
     * entry. Change it through {@link FlansTeams#setRoundExtensionData}.
     *
     * @param key
     *            the addon's key, namespaced with its mod id
     * @return a copy of the saved data, empty when there is none
     */
    CompoundTag getExtensionData(ResourceLocation key);
}
