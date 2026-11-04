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
    /**
     * The attitude with the given angles, the inverse of {@link #heading()}, {@link #pitch()} and {@link #roll()}.
     *
     * @param heading
     *            the Minecraft yaw the front points at
     * @param pitch
     *            degrees the front points above the horizon, negative below it
     * @param bank
     *            degrees rolled to the right about the front, the right side down
     */
    public static DriveableAttitude of(float heading, float pitch, float bank)
    {
        double yaw = Math.toRadians(heading);
        double elevation = Math.toRadians(pitch);
        double level = Math.cos(elevation);
        Vec3 front = new Vec3(-Math.sin(yaw) * level, Math.sin(elevation), Math.cos(yaw) * level);
        // The roof with the wings level, at right angles to the front: up, tipped back as far as the front is raised.
        Vec3 levelRoof = new Vec3(Math.sin(yaw) * Math.sin(elevation), level, -Math.cos(yaw) * Math.sin(elevation));
        Vec3 right = front.cross(levelRoof);
        double roll = Math.toRadians(bank);
        return new DriveableAttitude(front, levelRoof.scale(Math.cos(roll)).add(right.scale(Math.sin(roll))));
    }

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

    /**
     * @return degrees the driveable is rolled to its right about its front, the right side down, negative to its left;
     *         unlike {@link #bank()} measured from the wings-level roof at its pitch, so it stays exact with the nose
     *         raised or lowered
     */
    public float roll()
    {
        double length = front.horizontalDistance();
        if (length < 1.0E-6D)
            return 0F;
        // The wings-level roof lies in the vertical plane through the front; the right lies across it.
        Vec3 levelRoof = new Vec3(-front.x * front.y / length, length, -front.z * front.y / length);
        double right = (-front.z * roof.x + front.x * roof.z) / length;
        return (float) Math.toDegrees(Math.atan2(right, roof.dot(levelRoof)));
    }
}
