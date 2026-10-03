/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * An NPC whose renderer and server tracker can use the Flan NPC distance settings.
 * Implementations synchronize the explicit opt-in to clients and persist it with the NPC.
 * Read on the entity's owning thread on either side; change only on the server thread.
 */
@ApiStatus.Experimental
public interface IFlanNpcDistance
{
    /** @return whether this NPC uses the settings, through its model or an explicit opt-in */
    boolean usesFlanNpcDistanceSettings();

    /** @return the saved explicit opt-in, independently of the selected model */
    boolean isFlanNpcDistanceOptIn();

    /**
     * Set the saved explicit opt-in and refresh tracking on the server thread.
     * Removing the opt-in leaves automatic recognition of a Flan model in force.
     * @param enabled whether this NPC explicitly opts in
     */
    void setFlanNpcDistanceOptIn(boolean enabled);
}
