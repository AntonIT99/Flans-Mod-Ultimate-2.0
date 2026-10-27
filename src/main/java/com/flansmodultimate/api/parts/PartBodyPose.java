/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import com.flansmodultimate.api.driveables.MountAim;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.Vec3;

/**
 * Where an {@link IPartBody} stands in the world: turned by its yaw, then tilted by its pitch and roll as a driveable is.
 *
 * @param origin
 *            world position of the model origin
 * @param yaw
 *            the vanilla yaw, in degrees, the front of the model faces
 * @param pitch
 *            degrees the nose is raised, as a driveable pitches
 * @param roll
 *            degrees the model is banked to the right, about its length, as a driveable rolls
 * @param scale
 *            factor the model is drawn larger or smaller by; 1 is the size the definition gives it
 * @param turret
 *            where the main turret points, relative to the hull
 */
@ApiStatus.Experimental
public record PartBodyPose(Vec3 origin, float yaw, float pitch, float roll, float scale, MountAim turret)
{
    /** A body at the world origin, facing south, level, at its own size with the turret at rest. */
    public static final PartBodyPose REST = new PartBodyPose(Vec3.ZERO, 0F, 1F, MountAim.REST);

    /** Keeps the scale usable. */
    public PartBodyPose
    {
        scale = Float.isFinite(scale) && scale > 1.0E-4F ? scale : 1F;
    }

    /** A level body, turned only by its yaw. */
    public PartBodyPose(Vec3 origin, float yaw, float scale, MountAim turret)
    {
        this(origin, yaw, 0F, 0F, scale, turret);
    }
}
