/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/** Read-only trigger behaviour and current burst state. Query on the server thread before updating automated triggers. */
@ApiStatus.Experimental
public record WeaponTriggerState(Mode mode, boolean burstActive)
{
    /** How a weapon responds to its trigger. */
    @ApiStatus.Experimental
    public enum Mode
    {
        /** Keep the trigger held, including weapons that need to spin up. */
        HELD,
        /** Release between shots. */
        SEMIAUTOMATIC,
        /** Release between complete bursts. */
        BURST
    }

    /** @return whether the controller may release this trigger to prepare its next shot or burst */
    public boolean needsRelease()
    {
        return mode == Mode.SEMIAUTOMATIC || mode == Mode.BURST && !burstActive;
    }
}
