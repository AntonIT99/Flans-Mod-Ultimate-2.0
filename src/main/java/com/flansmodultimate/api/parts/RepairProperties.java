/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import org.jetbrains.annotations.ApiStatus;

/**
 * Repair settings of a driveable definition, on either side after content loading.
 *
 * @param enabled
 *            whether timed sessions are enabled
 * @param sessionTicks
 *            duration of one session in ticks
 * @param ship
 *            whether naval damage control and buoyancy recovery apply
 * @param carrier
 *            whether the hull can service aircraft
 */
@ApiStatus.Experimental
public record RepairProperties(boolean enabled, int sessionTicks, boolean ship, boolean carrier)
{}
