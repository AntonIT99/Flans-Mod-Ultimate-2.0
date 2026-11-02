/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

/** Optional living-entity presentation policy. Queried on the logical side documented by each method; never changes damage or death processing. */
@ApiStatus.Experimental
public interface ILivingVisualEffects
{
    /**
     * Opts an entity into airborne wound effects and temporary ground splatters on death and heavy damage. Queried on the server thread;
     * the server's Blood Effects rule must also allow them. Players participate without implementing this interface.
     *
     * @return whether this entity produces blood effects; false unless explicitly enabled
     */
    default boolean flansmodultimateBloodEffects()
    {
        return false;
    }

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
