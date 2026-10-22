/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * How a held Flan shield protects its holder from hits taken from the front, in either hand. Readable on either side
 * after content loading.
 *
 * @param damageAbsorption
 *            share of a frontal hit's damage absorbed, from 0 to 1, for hits a shield cannot block outright, such as bullets
 * @param blockChance
 *            chance, from 0 to 1, that the shield blocks a frontal melee hit or thrown weapon entirely
 * @param maxBlockableMeleeDamage
 *            strongest melee hit, in damage points, the shield can still block
 */
@ApiStatus.Experimental
public record EquippedShieldProperties(double damageAbsorption, double blockChance, double maxBlockableMeleeDamage)
{}
