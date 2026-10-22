/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * A row an addon adds to a team on the Teams scoreboard, beside its players: one combatant, or several summed up.
 *
 * @param name
 *            the row's name; it should not read as a player name, so that clients never mistake it for one
 * @param score
 *            the row's score, sorted against the players' scores
 * @param kills
 *            kills the row counts
 * @param deaths
 *            deaths the row counts
 * @param label
 *            what the class column shows, such as the combatant's class
 */
@ApiStatus.Experimental
public record TeamsScoreEntry(String name, int score, int kills, int deaths, String label)
{}
