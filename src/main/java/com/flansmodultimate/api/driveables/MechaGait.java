/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * Authored mecha footfall cycle, readable on either side after content loading.
 *
 * @param sound
 *            stomp sound identifier
 * @param delay
 *            minimum ticks between sounds
 * @param speed
 *            cycles per walking tick
 * @param lower
 *            beginning of the audible phase window, in cycles
 * @param upper
 *            end of the audible phase window, in cycles
 */
@ApiStatus.Experimental
public record MechaGait(String sound, int delay, float speed, float lower, float upper)
{
    /** @return whether a forward phase step crossed the authored sound window, including wrap around at one cycle */
    public boolean crosses(float previous, float current)
    {
        return previous <= current ? previous < upper && current >= lower : previous < upper || current >= lower;
    }
}
