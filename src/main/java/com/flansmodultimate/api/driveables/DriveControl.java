/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * A movement control of a driveable, which its driver holds as a player holds the key of the same name. What a control
 * does depends on the kind of driveable and its definition, exactly as it does for a player.
 */
@ApiStatus.Experimental
public enum DriveControl
{
    /**
     * Accelerates a vehicle and walks a mecha forward. It moves an aircraft's throttle lever up, which stays where it is
     * left; a rotorcraft's lever is its collective.
     */
    FORWARD,
    /** Slows a vehicle down and then reverses it, walks a mecha backward and moves an aircraft's throttle lever down. */
    BACKWARD,
    /** Steers a vehicle, which a wheeled one only does while rolling, or yaws an aircraft to its left; a mecha steps to its left. */
    LEFT,
    /** Steers a vehicle, which a wheeled one only does while rolling, or yaws an aircraft to its right; a mecha steps to its right. */
    RIGHT,
    /** Pulls an aircraft's nose up; a mecha jumps or fires its rocket pack, and a vehicle brakes. */
    ASCEND,
    /** Pushes an aircraft's nose down. */
    DESCEND,
    /** Rolls an aircraft to its left. */
    ROLL_LEFT,
    /** Rolls an aircraft to its right. */
    ROLL_RIGHT,
    /** Brakes. */
    BRAKE
}
