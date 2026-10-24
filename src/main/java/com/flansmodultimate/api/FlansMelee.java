/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.driveables.damage.VehicleMeleeDamage;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Shared melee classification and weapon scaling for driveables and entities representing them. */
@ApiStatus.Experimental
public final class FlansMelee
{
    private FlansMelee()
    {}

    /** @return whether this is a direct melee hit, excluding projectiles and explosions; either side */
    public static boolean isMelee(DamageSource source)
    {
        return VehicleMeleeDamage.isMelee(source);
    }

    /**
     * Applies a Flan weapon's driveable melee multiplier once to an otherwise unscaled incoming hit, before checking
     * the threshold or applying armour. Non-melee and non-Flan weapons keep their damage. Either side on the level thread.
     *
     * @return damage in health points against a driveable or its model
     */
    public static float againstVehicle(DamageSource source, float amount)
    {
        return VehicleMeleeDamage.againstVehicle(source, amount);
    }

    /**
     * Creates a Flan melee source retaining the striking weapon, including an offhand weapon. Pass ordinary melee damage
     * to the target; driveables and model targets apply its vehicle multiplier. Server level thread only.
     *
     * @return a damage source attributed to the attacker
     */
    public static DamageSource source(LivingEntity attacker, ItemStack weapon)
    {
        return VehicleMeleeDamage.source(attacker, weapon);
    }

    /**
     * Retains a striking Flan weapon on an existing melee source without changing its damage type or attribution.
     * Non-Flan weapons retain the original source. Server level thread only.
     *
     * @return the source carrying weapon information for vehicle scaling
     */
    public static DamageSource source(DamageSource original, ItemStack weapon)
    {
        return VehicleMeleeDamage.source(original, weapon);
    }

    /**
     * Keeps a real driveable's rider protected, forwarding melee to its seat or hull before rider resistances apply.
     * Other damage remains cancelled as before. Call once per incoming hit on the level thread; either side may query
     * protection, but only the server forwards damage.
     *
     * @return whether the target rides a real driveable and its own damage must be cancelled
     */
    public static boolean protectRider(LivingEntity rider, DamageSource source, float amount)
    {
        return VehicleMeleeDamage.protectRider(rider, source, amount);
    }
}
