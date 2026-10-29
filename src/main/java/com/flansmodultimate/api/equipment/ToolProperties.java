/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * What a Flan tool does when used, from {@link FlansEquipment#getToolProperties}.
 *
 * @param heals
 *            whether it heals living entities, as a medkit does
 * @param repairs
 *            whether it repairs driveables, as a wrench does
 * @param amount
 *            health it heals or hit points it repairs with each use
 * @param parachute
 *            whether it opens a parachute
 */
@ApiStatus.Experimental
public record ToolProperties(boolean heals, boolean repairs, int amount, boolean parachute)
{}
