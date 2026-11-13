/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * How a Flan melee weapon that can block, such as a lightsaber, guards its holder while a block is raised. Readable on either
 * side after content loading.
 *
 * @param blockTime
 *            longest a raised block lasts, in ticks
 * @param holdToBlock
 *            whether a player's block lasts only while the key is held, rather than for its full time
 * @param moveSpeed
 *            movement speed multiplier of the holder while blocking, from 0 to 1
 * @param deflectChance
 *            chance, from 0 to 1, that a frontal projectile is sent back where the holder looks
 * @param maxBlockableMeleeDamage
 *            strongest frontal melee hit or thrown weapon, in damage points, the block still stops
 */
@ApiStatus.Experimental
public record EquippedBlockProperties(int blockTime, boolean holdToBlock, double moveSpeed, double deflectChance, double maxBlockableMeleeDamage)
{}
