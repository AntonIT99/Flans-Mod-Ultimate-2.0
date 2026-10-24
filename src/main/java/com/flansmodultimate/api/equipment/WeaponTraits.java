/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * What a Flan gun is built for, beyond its damage and cadence: how far its sight magnifies and how many rounds it fires
 * before it has to be reloaded. Readable on either side after content loading.
 *
 * @param scopeZoom
 *            magnification of the sight in use, the attached scope's or else the gun's own; 1 or a little more for iron
 *            sights
 * @param magazineCapacity
 *            rounds the gun holds when fully loaded with the given ammunition, or with the first it takes when none is
 *            given; 0 when it takes none
 */
@ApiStatus.Experimental
public record WeaponTraits(float scopeZoom, int magazineCapacity)
{}
