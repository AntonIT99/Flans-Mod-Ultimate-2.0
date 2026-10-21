/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Picks where a player takes the field again in a Teams round, instead of the game type's spawn point: on a squad, at a
 * rally point, or anywhere else an addon offers. Register one with {@link FlansTeams#addSpawnProvider}. Providers are
 * asked on the server thread, in the order they were registered, once every {@link ITeamsRespawnRule} let the player
 * back; the first position given is used. They are asked for the same occasions as the respawn rules, never when a round
 * starts. A provider that throws is logged and skipped.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface ITeamsSpawnProvider
{
    /**
     * @param round
     *            the playing in progress
     * @param player
     *            the player about to take the field
     * @param team
     *            the team the player plays for
     * @param reason
     *            why the player takes the field
     * @return where the player appears, in the round's dimension, or empty to leave it to the game type
     */
    Optional<Vec3> spawnPosition(ITeamsLiveRound round, ServerPlayer player, ITeam team, ITeamsRespawnRule.Reason reason);
}
