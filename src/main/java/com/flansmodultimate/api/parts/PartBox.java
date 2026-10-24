/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.AABB;

/**
 * The hitbox of one damageable part of a driveable, with what it takes to break it.
 *
 * @param bounds
 *            the box in the static model preview frame: blocks, Y up, forward -Z, relative to the model origin. A part
 *            carried by the turret is given with the turret at rest and turns with it
 * @param health
 *            hit points of the part; 0 makes a part that stops rounds but cannot be broken
 * @param penetrationResistance
 *            penetrating power a round loses passing through the part
 */
@ApiStatus.Experimental
public record PartBox(AABB bounds, float health, float penetrationResistance)
{
    /** Keeps the numbers finite and non-negative. */
    public PartBox
    {
        health = Float.isFinite(health) ? Math.max(0F, health) : 0F;
        penetrationResistance = Float.isFinite(penetrationResistance) ? Math.max(0F, penetrationResistance) : 0F;
    }
}
