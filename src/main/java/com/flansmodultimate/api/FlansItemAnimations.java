/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.guns.HeldItemPresentation;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Server-controlled presentation of held Flan items, usable without loading client classes. */
@ApiStatus.Experimental
public final class FlansItemAnimations
{
    private FlansItemAnimations()
    {}

    /** Reads the hand's saved settings on either side, including addon-owned storage. */
    public static HeldItemSettings get(LivingEntity holder, InteractionHand hand)
    {
        return HeldItemPresentation.get(holder, hand);
    }

    /**
     * Saves and synchronizes settings to the holder and tracking clients. Server entity thread only; a client call throws.
     * Item transforms affect held 3D Flan items; arm presets affect held Flan items, and explicit custom axes also affect empty arms.
     * Neither moves projectile origins.
     */
    public static void set(LivingEntity holder, InteractionHand hand, HeldItemSettings settings)
    {
        HeldItemPresentation.set(holder, hand, settings);
    }

    /** Resets this hand's placement and arm rules, saving and synchronizing the result; server entity thread only. */
    public static void reset(LivingEntity holder, InteractionHand hand)
    {
        set(holder, hand, HeldItemSettings.DEFAULT);
    }

}
