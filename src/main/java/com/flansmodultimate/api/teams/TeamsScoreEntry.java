/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.teams;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

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
 *            the canonical class-column label
 * @param displayName
 *            the displayed row name, retained as a component for each client to translate
 * @param displayLabel
 *            the displayed class-column label, retained as a component for each client to translate
 */
@ApiStatus.Experimental
public record TeamsScoreEntry(String name, int score, int kills, int deaths, String label, Component displayName, Component displayLabel)
{
    /** Creates a row with literal name and class labels, readable on either side. */
    public TeamsScoreEntry(String name, int score, int kills, int deaths, String label)
    {
        this(name, score, kills, deaths, label, Component.literal(name), Component.literal(label));
    }
}
