/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hears what happens in Teams rounds. Register one with {@link FlansTeams#addListener}. Every method is called on the
 * server thread after Flan's Mod Ultimate has handled the event; a listener that throws is logged and skipped.
 */
@ApiStatus.Experimental
// Every event is optional: a listener overrides the ones it needs and the rest do nothing.
@SuppressWarnings("EmptyMethod")
public interface ITeamsListener
{
    /**
     * A round started: its teams, scores and capture points are set up and the players are being placed.
     *
     * @param round
     *            the new playing
     */
    default void roundStarted(ITeamsLiveRound round)
    {}

    /**
     * A round ended, because a side won, the clock ran out, an operator stopped it or another round replaced it.
     *
     * @param round
     *            the playing that ended, which no longer reports itself running
     * @param winner
     *            the first team of the winning side, or null for a draw or a round that was stopped
     */
    default void roundEnded(ITeamsLiveRound round, @Nullable ITeam winner)
    {}

    /**
     * A capture point changed owner: a side took it, or it was taken down to neutral.
     *
     * @param round
     *            the playing in progress
     * @param point
     *            the point as it is now
     * @param previousOwnerSide
     *            the side that owned it before, or -1
     */
    default void capturePointChanged(ITeamsLiveRound round, TeamsCapturePoint point, int previousOwnerSide)
    {}

    /**
     * A player or a combatant of the round died, after the game type has scored the death.
     *
     * @param round
     *            the playing in progress
     * @param victim
     *            the player or combatant who died
     * @param killer
     *            the player or combatant held responsible, or null when there is none or it is the victim
     * @param source
     *            the damage that killed the victim
     */
    default void combatantKilled(ITeamsLiveRound round, LivingEntity victim, @Nullable LivingEntity killer, DamageSource source)
    {}
}
