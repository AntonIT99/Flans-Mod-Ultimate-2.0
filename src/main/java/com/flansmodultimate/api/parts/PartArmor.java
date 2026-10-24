/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import org.jetbrains.annotations.ApiStatus;

/**
 * Uniform armour for every face of one part of an {@link IPartBody}, with no authored virtual slope. Immutable and safe
 * to pass between threads; apply it to a body on that body's level thread.
 *
 * @param thicknessMm
 *            nominal protection against kinetic rounds, in millimetres; zero leaves the part unarmoured
 * @param heatThicknessMm
 *            equivalent protection against shaped-charge jets, in millimetres, allowing composite armour
 */
@ApiStatus.Experimental
public record PartArmor(float thicknessMm, float heatThicknessMm)
{
    /** Keeps both thicknesses finite and non-negative. */
    public PartArmor
    {
        thicknessMm = Float.isFinite(thicknessMm) ? Math.max(0F, thicknessMm) : 0F;
        heatThicknessMm = Float.isFinite(heatThicknessMm) ? Math.max(0F, heatThicknessMm) : 0F;
    }
}
