/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Optional;

/** Optional living-entity policy for Flan equipment effects and presentation. Read on the entity's own side. */
@ApiStatus.Experimental
public interface IEquipmentPolicy
{
    /** @return whether equipped Flan armor may apply its damage and special effects */
    default boolean flansArmorEffects()
    {
        return true;
    }

    /**
     * Read on the server when armor potion effects are refreshed. Returning false also removes tracked armor potion effects.
     * Armor protection, attributes and movement bonuses remain governed by {@link #flansArmorEffects()}.
     *
     * @return whether equipped Flan armor may grant potion effects; defaults to the general armor policy
     */
    default boolean flansmodultimateArmorPotionEffects()
    {
        return flansArmorEffects();
    }

    /**
     * Narrows {@link #flansArmorEffects()} to one incoming hit: Flan armor defense and bullet defense only reduce it when this
     * returns true. Read on the server while the hit is resolved.
     *
     * @param source
     *            the damage being resolved
     * @return whether equipped Flan armor reduces this hit; by default, whether armor effects apply at all
     */
    default boolean flansArmorProtects(DamageSource source)
    {
        return flansArmorEffects();
    }

    /** @return whether Flan held shields may block incoming attacks */
    default boolean flansWeaponEffects()
    {
        return true;
    }

    /** @return whether equipped armor follows the wearer's animated limbs; false uses the resting pose */
    default boolean flansArmorAnimations()
    {
        return true;
    }

    /** @return whether normal Flan weapon animations change arm poses; shield requirements and explicit custom axes remain active */
    default boolean flansWeaponAnimations()
    {
        return true;
    }

    /**
     * Lets an addon-controlled non-player inherit the server's enforced player aim rule. With free player choice, the
     * server's entity rule applies. Read on the entity's own side; false preserves the normal entity policy.
     *
     * @return whether this non-player follows enforced player aim rules before the entity fallback
     */
    @SuppressWarnings("SameReturnValue") // Extension point: external addons may opt in to the enforced player rule.
    default boolean flansmodultimateUsePlayerAimRules()
    {
        return false;
    }

    /**
     * Read on the entity's own side before the general aim rules. Addons must synchronize their preference to clients.
     *
     * @return an explicit aim preference (true for dynamic, false for always raised), or empty to inherit the general rules
     */
    default Optional<Boolean> flansmodultimateDynamicAimPose()
    {
        return Optional.empty();
    }

    /**
     * Read on the server when a held gun fires; the aim pose is governed by {@link #flansWeaponAnimations()} instead.
     *
     * @return whether a held Flan gun plays its shot animation and muzzle flash model for others
     */
    default boolean flansGunShotAnimations()
    {
        return true;
    }

    /**
     * Allows an addon to draw a throw wind-up without changing the entity's real active-use item.
     * The caller's renderer must expose the displayed item in that hand. Read on the entity's own side.
     *
     * @param hand
     *            hand drawing back for a throw
     * @return whether the hand uses the player's throw pose, preserving the other arm's held pose
     */
    default boolean flansmodultimateChargingThrow(InteractionHand hand)
    {
        return false;
    }
}
