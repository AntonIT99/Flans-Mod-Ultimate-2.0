/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/**
 * Definition-based grenade purpose, readable on either side after content loading.
 *
 * @param use
 *            primary purpose; placement and remote devices require their own control logic
 * @param effectRadius
 *            additional flash or smoke-effect reach in blocks, beyond the detonation radii
 */
@ApiStatus.Experimental
public record GrenadeProperties(Use use, double effectRadius)
{
    /** Primary grenade purpose, independent of its name, model or particles. */
    @ApiStatus.Experimental
    public enum Use
    {
        EXPLOSIVE, INCENDIARY, FLASH, SMOKE, GAS, PLACEABLE, REMOTE, UTILITY
    }
}
