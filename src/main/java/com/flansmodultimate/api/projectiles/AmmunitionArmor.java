/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/**
 * Read-only nominal ammunition performance, without tracing a hit or applying damage. Readable on either side after loading.
 *
 * @param penetrationMm
 *            penetration in millimetres at 100 metres; excludes impact angle, distance correction and platform overrides
 * @param shapedCharge
 *            whether the round tests the plate's HEAT protection rather than its kinetic protection
 */
@ApiStatus.Experimental
public record AmmunitionArmor(double penetrationMm, boolean shapedCharge)
{}
