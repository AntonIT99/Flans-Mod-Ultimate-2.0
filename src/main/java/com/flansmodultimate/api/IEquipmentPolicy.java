/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** Optional living-entity policy for Flan equipment effects and presentation. Read on the entity's own side. */
@ApiStatus.Experimental
public interface IEquipmentPolicy
{
    /** @return whether equipped Flan armor may apply its damage and special effects */
    default boolean flansArmorEffects()
    {
        return true;
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

    /** @return whether Flan weapons may change the wearer's arm poses */
    default boolean flansWeaponAnimations()
    {
        return true;
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
}
