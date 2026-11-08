/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

/**
 * How much of a driveable model's wheel footprint rests on terrain, so that something moving in a driveable's place can
 * be held up, and dropped, by the same ground a player-driven one is.
 *
 * <p>
 * An entity wearing a driveable model collides through its own box, which covers only the middle of a long or wide
 * model. Left to that box alone it stands wherever the block under its centre is, with the hull hanging over a roof,
 * a bridge or a ledge the vehicle itself could never park on. The wheels sampled here are what a real driveable rests
 * on, and they say both whether the model is carried at all and which way it hangs over.
 * </p>
 *
 * @param grounded
 *            wheels that found support beneath them
 * @param total
 *            wheels sampled; zero when the definition has none or the terrain was not loaded, which is never a report
 *            of missing support
 * @param overhang
 *            world-horizontal vector, in blocks, from the supported wheels towards the unsupported ones: the way the
 *            model hangs over. Zero when every wheel agrees, and when there is nothing to compare
 */
@ApiStatus.Experimental
public record WheelSupport(int grounded, int total, Vec3 overhang)
{
    /** No usable sample: a definition without wheels, or terrain that is not loaded. Reads as fully supported. */
    public static final WheelSupport UNKNOWN = new WheelSupport(0, 0, Vec3.ZERO);

    /** @return the share of the wheel footprint that is carried, 1 without a usable sample */
    public double share()
    {
        return total == 0 ? 1D : (double) grounded / total;
    }

    /** @return whether the sample is usable and no wheel at all is carried */
    public boolean unsupported()
    {
        return total > 0 && grounded == 0;
    }

    /**
     * @param holdShare
     *            the share of its wheels a model has to keep on the ground to be held up by them
     * @return whether too little of the footprint is carried for the model to stay where it is
     */
    public boolean overhanging(double holdShare)
    {
        return total > 0 && share() < holdShare;
    }
}
