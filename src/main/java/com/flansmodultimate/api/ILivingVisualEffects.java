/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** Optional living-entity presentation policy. Read on the render thread; never changes damage or death processing. */
@ApiStatus.Experimental
public interface ILivingVisualEffects
{
    /** @return whether the renderer displays the red overlay when hurt or dying */
    default boolean flansmodultimateHurtFlash()
    {
        return true;
    }

    /** @return whether the renderer tips the entity over during death */
    default boolean flansmodultimateDeathRotation()
    {
        return true;
    }
}
