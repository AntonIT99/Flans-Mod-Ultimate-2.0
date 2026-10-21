/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * One playing of a Teams rotation entry, read as it goes on. Sides are numbered as in {@link ITeamsRound}.
 *
 * <p>
 * A view may be kept. Once its playing ends, {@link #isRunning()} turns false, though the view still reads the map
 * through the break that follows; once another round starts or an operator stops it, its lists are empty and its
 * clock reads 0. Get the current one through {@link FlansTeams#getRunningRound()}. Server thread only.
 * </p>
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ITeamsLiveRound
{
    /**
     * @return the rotation entry being played
     */
    ITeamsRound getRound();

    /**
     * @return the id of this playing; every playing of an entry gets a new one
     */
    UUID getInstanceId();

    /**
     * @return whether this playing is still in progress; false once it ended, from the end-of-round event on
     */
    boolean isRunning();

    /**
     * @return the dimension the map is played in
     */
    ResourceKey<Level> getDimension();

    /**
     * @param side
     *            a side, from 0
     * @return the score of the side, which allied teams share; 0 for a side the round does not have. In Assault the
     *         attackers' score counts the reinforcements they lost.
     */
    int getScore(int side);

    /**
     * @return game ticks left on the round clock
     */
    int getTicksLeft();

    /**
     * @return game ticks played since the round started
     */
    int getTicksElapsed();

    /**
     * @return the side that attacks, in game types that have attackers and defenders such as Assault; empty otherwise
     */
    OptionalInt getAttackingSide();

    /**
     * @return the side that defends, in game types that have attackers and defenders such as Assault; empty otherwise
     */
    OptionalInt getDefendingSide();

    /**
     * @return the loaded capture points of the map in play order, by letter, immutable; empty in game types without
     *         them
     */
    List<TeamsCapturePoint> getCapturePoints();

    /**
     * @return the flags of the map in Capture the Flag, immutable; empty in other game types
     */
    List<TeamsFlag> getFlags();

    /**
     * Where members of a team may come back right now, as players do: the player spawners of the bases the team or,
     * for capture points, its side holds. A base without spawners counts with its own position.
     *
     * @param team
     *            a team of the round
     * @return world positions in the round's dimension, immutable; empty for a team that does not play
     */
    List<Vec3> getSpawnPositions(ITeam team);

    /**
     * @return the loaded spawner blocks connected to the bases of the map, immutable
     */
    List<TeamsSpawner> getSpawners();
}
