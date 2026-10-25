/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/** Lets an addon replace the distance from which the name tag of a living entity can be seen. Client thread. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NameTagRanges
{
    private static Resolver resolver = (entity, configured) -> configured;

    /** Replaces the resolver; set it once, during client setup. */
    public static void setResolver(Resolver newResolver)
    {
        resolver = Objects.requireNonNull(newResolver);
    }

    /** @return the range in blocks for this entity, given the range the Flan's Mod config sets */
    public static float resolve(LivingEntity entity, float configured)
    {
        return resolver.range(entity, configured);
    }

    @FunctionalInterface
    public interface Resolver
    {
        float range(LivingEntity entity, float configured);
    }
}
