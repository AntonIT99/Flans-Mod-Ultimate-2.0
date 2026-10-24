/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Caller-chosen replacements for some of a held gun's own statistics when it fires through
 * {@link FlansEquipment#fire(net.minecraft.world.entity.LivingEntity, net.minecraft.world.InteractionHand, net.minecraft.world.phys.Vec3, net.minecraft.world.phys.Vec3, boolean, boolean, WeaponOverrides)}.
 * An empty value keeps what the gun, its attachments and its holder's stance give. Replaced values stand in for the gun's
 * own, so the loaded ammunition still modifies them as it would the gun's: its damage and spread multipliers apply, and a
 * muzzle velocity it declares still takes precedence over the speed.
 *
 * @param damage
 *            weapon damage per round before ammunition, at least 0
 * @param spread
 *            angular spread in Flan units, at least 0
 * @param speed
 *            launch speed in blocks per tick used when the ammunition declares none, above 0
 * @param projectiles
 *            rounds launched per shot, from 1 to 128
 */
@ApiStatus.Experimental
public record WeaponOverrides(OptionalDouble damage, OptionalDouble spread, OptionalDouble speed, OptionalInt projectiles)
{
    /** Fires the gun with its own statistics. */
    public static final WeaponOverrides NONE = new WeaponOverrides(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty(), OptionalInt.empty());

    /** @return whether nothing is replaced */
    public boolean isEmpty()
    {
        return damage.isEmpty() && spread.isEmpty() && speed.isEmpty() && projectiles.isEmpty();
    }
}
