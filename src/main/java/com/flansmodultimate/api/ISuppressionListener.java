/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Hears Flan rounds passing close to mobs, the near misses that suppress players, so an addon can make its NPCs react to
 * being shot at. Register one with {@link FlansProjectiles#addSuppressionListener}. Called on the server thread at most
 * once per shot and mob, for mobs that see the round's path and were not its shooter; a listener that throws is logged
 * and skipped.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface ISuppressionListener
{
    /**
     * @param target
     *            the mob the round passed
     * @param shooter
     *            who fired it, or null when unknown
     * @param intensity
     *            how hard the miss suppresses, from above 0 to 1, rising with the round's damage and with how close it came
     */
    void suppressed(Mob target, @Nullable LivingEntity shooter, float intensity);
}
