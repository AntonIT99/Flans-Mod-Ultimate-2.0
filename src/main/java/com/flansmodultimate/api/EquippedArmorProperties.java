/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/** Flan armor's additional absorption fractions and special protections, readable on either side. Vanilla attributes are separate. */
@ApiStatus.Experimental
public record EquippedArmorProperties(double defense, double bulletDefense, boolean negatesFallDamage, boolean waterBreathing, boolean fireResistance)
{}
