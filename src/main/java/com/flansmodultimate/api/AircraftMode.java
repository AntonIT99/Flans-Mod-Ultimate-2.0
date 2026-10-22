/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** How an aircraft flies at the moment; a VTOL switches between the first two. */
@ApiStatus.Experimental
@SuppressWarnings("unused") // Read by the NPC module and other mods, which IntelliJ does not count from here.
public enum AircraftMode
{
    /**
     * On its wings: it needs airspeed for lift, its throttle sets its thrust, and it pitches, rolls and yaws to manoeuvre.
     */
    FIXED_WING,
    /**
     * On its rotors: its throttle is the collective, about half of which holds a hover once the rotor has spun up, and
     * tilting it carries it along.
     */
    ROTORCRAFT,
    /** Freely in every direction, without lift or gravity: it flies along its nose at the speed its throttle sets. */
    SIX_DOF
}
