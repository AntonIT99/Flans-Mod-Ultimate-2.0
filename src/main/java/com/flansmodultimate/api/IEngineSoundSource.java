/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** Optional entity engine pitch. Read on the client sound thread's game tick, without changing gameplay state. */
@ApiStatus.Experimental
public interface IEngineSoundSource
{
    /** @return current movement pitch multiplier; finite positive values are clamped to the sound engine's 0.5–2 range */
    default float flansmodultimateEnginePitch()
    {
        return 1F;
    }
}
