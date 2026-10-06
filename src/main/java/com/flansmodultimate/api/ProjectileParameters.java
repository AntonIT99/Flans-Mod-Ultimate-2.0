/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Caller-supplied weapon defaults for preparing a projectile. Ammunition physics still apply.
 *
 * @param round
 *            nonnegative belt position
 * @param weaponStats
 *            whether content weapon statistics and pellet counts take precedence
 * @param damage
 *            fallback weapon damage when no weapon is found or weapon statistics are disabled
 * @param spread
 *            fallback Flan angular spread units
 * @param speed
 *            fallback velocity in blocks per tick; ammunition velocity still takes precedence
 */
@ApiStatus.Experimental
public record ProjectileParameters(int round, boolean weaponStats, float damage, float spread, float speed)
{}
