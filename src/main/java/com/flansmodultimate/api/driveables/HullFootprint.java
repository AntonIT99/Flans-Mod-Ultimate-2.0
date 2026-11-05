/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * How much room a driveable's hull takes up, read from the collision shapes of its definition, without its turret and
 * barrels, which turn over the hull. Measured in blocks about the driveable's own position, so that a box this size
 * centred there holds the whole hull: an off-centre hull counts twice its furthest reach to each side. Safe to read on
 * either side after content loading.
 *
 * @param length
 *            blocks from bow to stern
 * @param width
 *            blocks from side to side, which is what has to fit between two walls
 * @param height
 *            blocks from the bottom of the hull to its top
 */
@ApiStatus.Experimental
public record HullFootprint(double length, double width, double height)
{}
