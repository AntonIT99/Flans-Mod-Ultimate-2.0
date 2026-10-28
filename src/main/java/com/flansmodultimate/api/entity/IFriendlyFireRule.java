/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.LivingEntity;

/**
 * Decides whether one living entity is kept from hurting another as friendly fire, for sides an addon knows of, such as
 * the factions of its NPCs. Register one with {@link FlansFriendlyFire#addRule}.
 *
 * <p>
 * Rules are asked about hits between entities that do not both play a running Teams round, whose game type decides those.
 * Flan's Mod Ultimate asks them before any damage lands, whatever dealt it, and before a Flan round damages a driveable
 * someone rides or a proximity fuse goes off beside someone. A hit is spared when any rule spares it. Called on the server
 * thread; a rule that throws is logged and skipped.
 * </p>
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface IFriendlyFireRule
{
    /**
     * @param victim
     *            the entity about to be hurt, or riding the driveable about to be hit
     * @param attacker
     *            the entity responsible: the one that struck or fired, or the owner of the projectile; never the victim
     * @return whether the hit is held back as friendly fire
     */
    boolean spares(LivingEntity victim, LivingEntity attacker);
}
