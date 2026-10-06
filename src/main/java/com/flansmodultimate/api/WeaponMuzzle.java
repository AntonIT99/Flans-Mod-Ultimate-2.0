/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * A muzzle in the static model preview frame: blocks, Y up, forward -Z, relative to the model origin.
 * Includes loaded barrel measurements where available. Readable on either side after content loading.
 *
 * @param offset
 *            muzzle position before the caller's entity size, resting height and body rotation
 * @param weapon
 *            mounted gun definition, if the point names one
 * @param particles
 *            shooting particle velocities in the same frame
 */
@ApiStatus.Experimental
public record WeaponMuzzle(Vec3 offset, Optional<IContentType> weapon, List<Particle> particles)
{
    /** Makes the particle list immutable. */
    public WeaponMuzzle
    {
        particles = List.copyOf(particles);
    }

    /**
     * A content-defined shooting particle.
     *
     * @param name
     *            legacy or modern particle identifier
     * @param velocity
     *            velocity in model preview coordinates, in blocks per tick
     */
    @ApiStatus.Experimental
    public record Particle(String name, Vec3 velocity)
    {}
}
