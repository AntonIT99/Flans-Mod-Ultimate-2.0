/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import java.util.EnumSet;
import java.util.Set;

/** Pure torso-relative walking decisions shared by autonomous mecha controllers. Safe on either side. */
@ApiStatus.Experimental
public final class MechaWalking
{
    private static final double STEP_SHARE = 0.38D;

    private MechaWalking()
    {}

    /**
     * @param off
     *            degrees clockwise from the torso's forward direction to the goal
     * @param distance
     *            horizontal distance to the goal, in blocks
     * @param arrival
     *            horizontal stopping distance, in blocks
     * @return independent forward/back and strafe controls; empty inside the stopping distance
     */
    public static Set<DriveControl> toward(double off, double distance, double arrival)
    {
        Set<DriveControl> controls = EnumSet.noneOf(DriveControl.class);
        if (distance <= arrival)
            return Set.of();
        double radians = Math.toRadians(off);
        double forward = Math.cos(radians);
        double side = Math.sin(radians);
        if (forward > STEP_SHARE)
            controls.add(DriveControl.FORWARD);
        else if (forward < -STEP_SHARE)
            controls.add(DriveControl.BACKWARD);
        if (side > STEP_SHARE)
            controls.add(DriveControl.RIGHT);
        else if (side < -STEP_SHARE)
            controls.add(DriveControl.LEFT);
        return Set.copyOf(controls);
    }
}
