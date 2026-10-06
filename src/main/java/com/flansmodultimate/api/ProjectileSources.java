/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Sources used instead of caller-supplied projectile defaults. Readable on either side after content loading.
 * A mixed belt reports CALLER if any round still needs that default.
 *
 * @param damage
 *            source of damage, including kinetic ammunition
 * @param spread
 *            source of angular spread
 * @param speed
 *            source of launch velocity
 * @param firingSound
 *            whether content supplies firing audio; grenades reserve their native throw audio even when silent
 */
@ApiStatus.Experimental
public record ProjectileSources(Source damage, Source spread, Source speed, boolean firingSound)
{
    /** Which definition controls a setting, without changing its saved caller value. */
    @ApiStatus.Experimental
    public enum Source
    {
        /** At least one round uses the caller's fallback. */
        CALLER,
        /** All rounds ignore the caller's fallback, with a weapon contributing the setting. */
        WEAPON,
        /** Every round supplies its own setting through ammunition or a per-ammunition platform override. */
        AMMUNITION
    }
}
