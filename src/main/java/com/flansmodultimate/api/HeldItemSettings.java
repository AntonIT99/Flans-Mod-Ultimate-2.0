/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.guns.HeldItemPresentation;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

/** Immutable held-item placement and arm rules for one hand; readable on either side. */
@ApiStatus.Experimental
public record HeldItemSettings(HeldItemTransform transform, WeaponPoseRules poses)
{
    /** Authored item placement and normal server arm policy. */
    public static final HeldItemSettings DEFAULT = new HeldItemSettings(HeldItemTransform.IDENTITY, WeaponPoseRules.DEFAULT);

    /** Requires a transform and pose rules. */
    public HeldItemSettings
    {
        Objects.requireNonNull(transform);
        Objects.requireNonNull(poses);
    }

    /** Returns a new NBT compound suitable for entity persistence; either side. */
    public CompoundTag save()
    {
        return HeldItemPresentation.save(this);
    }

    /** Reads saved settings, retaining defaults for absent or invalid fields; either side. */
    public static HeldItemSettings load(CompoundTag tag)
    {
        return HeldItemPresentation.load(tag);
    }
}
