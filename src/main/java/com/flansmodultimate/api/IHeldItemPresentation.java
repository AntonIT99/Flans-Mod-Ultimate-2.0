/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.InteractionHand;

/** Optional entity-owned storage for held-item settings, for addons with their own editor, persistence and synchronization. */
@ApiStatus.Experimental
public interface IHeldItemPresentation
{
    /** Returns settings on the entity's own side; never null. */
    HeldItemSettings flansmodultimateHeldItemSettings(InteractionHand hand);

    /** Stores settings on the entity's own thread. The main API handles network synchronization for API callers. */
    void flansmodultimateHeldItemSettings(InteractionHand hand, HeldItemSettings settings);
}
