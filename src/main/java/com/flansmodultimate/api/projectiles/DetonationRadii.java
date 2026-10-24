/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/**
 * How far round its point of detonation a Flan round reaches, in blocks, as the server would detonate it now. A radius of
 * zero means the round has no effect of that kind.
 *
 * @param explosion
 *            radius of the explosion that breaks blocks and pushes entities
 * @param blast
 *            radius within which the blast damages entities
 * @param fragments
 *            radius within which casing fragments still deal at least one point of damage to a player-sized target
 * @param fire
 *            radius within which the round sets fire
 */
@ApiStatus.Experimental
public record DetonationRadii(double explosion, double blast, double fragments, double fire)
{
    /** @return the largest of the radii, in blocks */
    public double largest()
    {
        return Math.max(Math.max(explosion, blast), Math.max(fragments, fire));
    }
}
