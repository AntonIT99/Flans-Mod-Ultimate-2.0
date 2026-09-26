/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * A content-pack driveable definition: a vehicle, plane or mecha.
 *
 * <p>Distances are in blocks, in the driveable's local frame before it is rotated, with the
 * origin at the driveable entity's position and Y up. Horizontally, X is the lateral axis and Z
 * the longitudinal (fore-aft) one. Which end of Z is the front differs between planes and other
 * driveables, so rely on extents rather than on signs.</p>
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IDriveableType extends IContentType
{
    DriveableKind getDriveableKind();

    /**
     * @return the height of the origin above level ground when the driveable rests on its wheels,
     *         as its suspension places it, or its {@code YOffset} when it has no wheels. Negative
     *         when the wheels sit above the origin, as with some flying boats.
     */
    float getRestingHeight();

    /**
     * @return the collision box of the core part, which is the main body, if the definition has one
     */
    Optional<AABB> getCoreBounds();

    /**
     * @return the box enclosing the collision boxes of every part, if the definition has any
     */
    Optional<AABB> getBounds();
}
