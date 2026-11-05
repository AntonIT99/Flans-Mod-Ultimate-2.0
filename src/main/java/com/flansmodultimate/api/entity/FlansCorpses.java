/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;

/**
 * Bodies kept after the death animation. An addon that holds a dead entity past vanilla's removal keeps its
 * {@code deathTime} at {@link #DEATH_ANIMATION_TICKS} or above; the renderer then draws it fallen over without the red
 * hurt tint.
 */
@ApiStatus.Experimental
public final class FlansCorpses
{
    /** Ticks of vanilla's death animation, after which an ordinary entity is removed. */
    public static final int DEATH_ANIMATION_TICKS = 20;

    private FlansCorpses()
    {}

    /** Whether the renderer tints an entity red: while it is hurt and during its death animation, but not as a lingering corpse. Either side. */
    public static boolean hurtFlashing(LivingEntity entity)
    {
        return entity.hurtTime > 0 || entity.deathTime > 0 && entity.deathTime < DEATH_ANIMATION_TICKS;
    }
}
