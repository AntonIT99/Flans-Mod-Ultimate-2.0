/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * A movement control of a driveable, which its driver holds as a player holds the key of the same name. What a control
 * does depends on the kind of driveable and its definition, exactly as it does for a player.
 */
@ApiStatus.Experimental
public enum DriveControl
{
    /** Accelerates a vehicle, opens a plane's throttle, walks a mecha forward. */
    FORWARD,
    /** Slows a vehicle down and then reverses it, closes a plane's throttle, walks a mecha backward. */
    BACKWARD,
    /** Steers a vehicle or plane to its left, which a wheeled vehicle only does while rolling; a mecha steps to its left. */
    LEFT,
    /** Steers a vehicle or plane to its right, which a wheeled vehicle only does while rolling; a mecha steps to its right. */
    RIGHT,
    /** As a player's ascend key: a plane climbs or pulls up, a mecha jumps or fires its rocket pack, a vehicle brakes. */
    ASCEND,
    /** As a player's descend key: a plane descends or pushes down. */
    DESCEND,
    /** Rolls a plane to its left. */
    ROLL_LEFT,
    /** Rolls a plane to its right. */
    ROLL_RIGHT,
    /** Brakes. */
    BRAKE
}
