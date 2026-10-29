/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.debug;

import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

/**
 * Flan's debug mode, which the debug key toggles, and the markers it draws in the world, such as the seats, shoot points
 * and muzzles of driveables. Client only, on the client thread.
 */
@ApiStatus.Experimental
public final class FlansDebugMarkers
{
    private FlansDebugMarkers()
    {}

    /** @return whether debug mode is on */
    public static boolean isDebugMode()
    {
        return ModClient.isDebug();
    }

    /**
     * Marks a point while debug mode is on; does nothing otherwise.
     *
     * @param position
     *            world position, in blocks
     * @param ticks
     *            client ticks the mark stays; renew it each tick to keep it
     * @param red
     *            colour channel, 0 to 1
     * @param green
     *            colour channel, 0 to 1
     * @param blue
     *            colour channel, 0 to 1
     */
    public static void point(Vec3 position, int ticks, float red, float green, float blue)
    {
        DebugHelper.spawnDebugDot(position, ticks, red, green, blue);
    }

    /**
     * Draws a line from a point while debug mode is on; does nothing otherwise.
     *
     * @param start
     *            world position the line starts at, in blocks
     * @param offset
     *            from the start to the end of the line, in blocks
     * @param ticks
     *            client ticks the line stays; renew it each tick to keep it
     * @param red
     *            colour channel, 0 to 1
     * @param green
     *            colour channel, 0 to 1
     * @param blue
     *            colour channel, 0 to 1
     */
    public static void vector(Vec3 start, Vec3 offset, int ticks, float red, float green, float blue)
    {
        DebugHelper.spawnDebugVector(start, offset, ticks, red, green, blue);
    }
}
