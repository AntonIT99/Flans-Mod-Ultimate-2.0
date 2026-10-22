/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;

/**
 * Definition-derived ground handling, independent of driver policy and installed engine parts. Missing values have
 * no reliable equivalent and must retain the consumer's fallback. Safe to read on either side after content loading.
 *
 * @param turnRate
 *            sustained hull yaw rate in degrees per second, when absolute steering is active
 * @param turningRadius
 *            rolling turn radius in blocks derived from reference speed and yaw rate; absent for tracked vehicles
 * @param reverseShare
 *            reverse speed divided by forward speed; zero means the definition prohibits reversing
 */
@ApiStatus.Experimental
public record GroundHandling(OptionalDouble turnRate, OptionalDouble turningRadius, OptionalDouble reverseShare)
{
    /** No compatible handling values; consumers retain their own fallback for each field. */
    public static final GroundHandling EMPTY = new GroundHandling(OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
}
