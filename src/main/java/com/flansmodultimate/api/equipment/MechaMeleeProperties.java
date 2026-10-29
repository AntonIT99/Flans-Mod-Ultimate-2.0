/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.util.Mth;

/**
 * A sword addon's effective melee statistics on a mecha. Readable on either side.
 *
 * @param damage
 *            health points per hit
 * @param delay
 *            ticks between attacks
 * @param reach
 *            attack reach in blocks, including the mecha's reach multiplier
 */
@ApiStatus.Experimental
public record MechaMeleeProperties(float damage, int delay, double reach)
{
    /** Resolves the content definition's tool speed, tool reach and mecha reach using the player-mecha rules. */
    public static MechaMeleeProperties of(float speed, float toolReach, float mechaReach)
    {
        return new MechaMeleeProperties(Math.max(1F, 6F * speed), Math.max(4, Mth.ceil(10F / Math.max(0.1F, speed))), Mth.clamp(toolReach * mechaReach, 1F, 32F));
    }
}
