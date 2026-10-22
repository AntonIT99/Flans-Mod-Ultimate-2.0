/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * One hand's arm rule. CUSTOM replaces only axes selected by bits 1 (X), 2 (Y), and 4 (Z), in degrees relative to the body,
 * as in a puppet pose. These explicit rotations alone may override a shield's required pose. Immutable, either side.
 */
@ApiStatus.Experimental
public record ArmPoseRule(Pose pose, int axes, float x, float y, float z)
{
    /** Follows the normal server aim policy. */
    public static final ArmPoseRule DEFAULT = new ArmPoseRule(Pose.DEFAULT, 0, 0, 0, 0);

    /** The available arm rules; shields ignore every preset except explicit CUSTOM axes. */
    @ApiStatus.Experimental
    public enum Pose
    {
        DEFAULT, RELAXED, ONE_HANDED, TWO_HANDED, THROW, CUSTOM
    }

    /** Validates axes and bounds rotations to the puppet editor's -180..180 degree range. */
    public ArmPoseRule
    {
        Objects.requireNonNull(pose);
        axes &= 7;
        HeldItemTransform rotation = new HeldItemTransform(0, 0, 0, x, y, z);
        x = rotation.rotationX();
        y = rotation.rotationY();
        z = rotation.rotationZ();
    }

    /** Creates a preset without custom rotations; either side. */
    public static ArmPoseRule of(Pose pose)
    {
        return new ArmPoseRule(pose, 0, 0, 0, 0);
    }
}
