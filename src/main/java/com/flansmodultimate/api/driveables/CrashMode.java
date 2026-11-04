/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/** How a crippled or dead aircraft comes down, decided by what it has lost. */
@ApiStatus.Experimental
public enum CrashMode
{
    /** Still flying: nothing that keeps it in the air has been lost. */
    NONE,
    /** Its airframe or its pilot is gone: the nose follows the flight path down into an ever steeper dive. */
    DIVE,
    /** Its left wing is gone: the right wing's lift rolls it over to the left into a tightening spiral. */
    SPIRAL_LEFT,
    /** Its right wing is gone: it rolls over to the right into a tightening spiral. */
    SPIRAL_RIGHT,
    /** Its tail is gone: nothing keeps the nose into the wind, and it settles into a flat spin, falling almost level. */
    FLAT_SPIN,
    /** A rotorcraft without its tail rotor: the main rotor's torque spins the fuselage about the mast as it comes down. */
    MAST_SPIN,
    /** A rotorcraft without its main rotor: it drops, nose tucking under. */
    ROTOR_LOSS,
    /** Torn apart: it tumbles end over end with whatever momentum it was thrown with. */
    TUMBLE;

    /** @return whether the aircraft is coming down */
    public boolean falling()
    {
        return this != NONE;
    }

    /** @return whether the aircraft spins about its own vertical axis rather than flying nose first */
    public boolean spins()
    {
        return this == FLAT_SPIN || this == MAST_SPIN;
    }
}
