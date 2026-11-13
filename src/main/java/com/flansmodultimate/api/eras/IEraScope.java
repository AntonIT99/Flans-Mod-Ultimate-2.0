/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.eras;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * A source of era restrictions other than the server, its dimensions and Teams rounds, such as an addon's battles. Its
 * restrictions cover only the subjects it names, and an entity of the mod's own, such as a vehicle, that one of them
 * blocks is despawned, so a scope should name only the entities it spawned. Register it with
 * {@link FlansEras#addScope}; called on the server thread.
 */
@ApiStatus.Experimental
public interface IEraScope
{
    /**
     * The restrictions on a player, bot or entity; an item must pass every one of them, and those of every other scope.
     *
     * @param subject
     *            the entity whose equipment is checked
     * @return unmodifiable, empty when this scope does not cover the subject
     */
    List<EraScopeRestriction> restrictionsFor(Entity subject);
}
