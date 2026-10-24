/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Which way a driveable points: where its front, a plane's nose, faces, and where its roof faces, in world space. Angles
 * read from it are in degrees.
 *
 * @param front
 *            unit direction the front points in, its pitch included
 * @param roof
 *            unit direction the roof points in, at right angles to the front; straight up for a driveable that is level
 */
@ApiStatus.Experimental
public record DriveableAttitude(Vec3 front, Vec3 roof)
{
    /** @return degrees the front points above the horizon, negative below it */
    public float pitch()
    {
        return (float) Math.toDegrees(Math.asin(Mth.clamp(front.y, -1D, 1D)));
    }

    /** @return the Minecraft yaw the front points at, 0 facing south and growing to the right; 0 for a front pointing straight up or down */
    public float heading()
    {
        return front.horizontalDistanceSqr() < 1.0E-12D ? 0F : (float) Math.toDegrees(Math.atan2(-front.x, front.z));
    }

    /**
     * @return degrees the driveable is rolled to its right, the right side down, negative to its left; beyond 90 it is
     *         upside down
     */
    public float bank()
    {
        double length = front.horizontalDistance();
        if (length < 1.0E-6D)
            return 0F;
        // To the right of a front facing south lies the west.
        double right = (-front.z * roof.x + front.x * roof.z) / length;
        return (float) Math.toDegrees(Math.atan2(right, roof.y));
    }
}
