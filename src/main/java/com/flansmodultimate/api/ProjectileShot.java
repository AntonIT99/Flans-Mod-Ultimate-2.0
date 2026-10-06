/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A resolved round, including weapon modifiers and belt position. Server thread only. */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ProjectileShot
{
    /** @return initial velocity in blocks per tick */
    float speed();

    /** @return downward acceleration in blocks per tick squared, including dimension gravity */
    double gravity();

    /** @return air velocity retention per tick, including dimension drag */
    double drag();

    /**
     * Launches actual projectile entities, including for weapons normally using hitscan.
     * Does not consume inventory. Call once per trigger; weapon pellet counts are included.
     *
     * @param origin
     *            world position in blocks
     * @param direction
     *            nonzero world direction, normalized internally
     * @param throwSound
     *            whether a grenade may play its throw sound
     * @return immutable list of successfully spawned entities; empty on invalid input or the wrong side
     */
    List<Entity> launch(Vec3 origin, Vec3 direction, boolean throwSound);
}
