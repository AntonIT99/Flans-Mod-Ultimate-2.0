/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import com.flansmodultimate.common.entity.FriendlyFireRules;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;

/**
 * Friendly fire: which hits between living entities are held back because both fight on one side. Between two fighters of
 * a running Teams round, its game type decides through its friendly fire variable; between any others, the rules addons
 * register here. Server thread.
 */
@ApiStatus.Experimental
public final class FlansFriendlyFire
{
    private FlansFriendlyFire()
    {}

    /**
     * Registers a friendly fire rule. Registering the same rule twice has no effect.
     */
    public static void addRule(IFriendlyFireRule rule)
    {
        FriendlyFireRules.add(rule);
    }

    public static void removeRule(IFriendlyFireRule rule)
    {
        FriendlyFireRules.remove(rule);
    }

    /**
     * @param victim
     *            the entity that would be hurt
     * @param attacker
     *            the entity responsible for the hit: the one that struck or fired, or the owner of the projectile
     * @return whether the hit is held back as friendly fire, by the running Teams round or by a registered rule; false for an
     *         entity hurting itself
     */
    public static boolean spares(LivingEntity victim, LivingEntity attacker)
    {
        return FriendlyFireRules.spares(victim, attacker);
    }
}
