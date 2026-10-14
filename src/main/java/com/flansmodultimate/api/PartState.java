/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * A snapshot of one part of an {@link IPartBody}.
 *
 * @param name
 *            the name the part has in type files, such as {@code core}, {@code turret} or {@code leftTrack}
 * @param displayName
 *            readable English name of the part
 * @param box
 *            the box and hit points in use
 * @param definition
 *            the box and hit points the definition gives the part
 * @param custom
 *            whether {@code box} replaces the definition's
 * @param enabled
 *            whether the part takes part in hit detection and holds hit points at all
 * @param turretMounted
 *            whether the part turns with the main turret
 * @param health
 *            hit points left, between 0 and {@code box.health()}
 * @param destroyed
 *            whether the part has been broken off
 * @param onFire
 *            whether the part burns
 */
@ApiStatus.Experimental
public record PartState(String name, String displayName, PartBox box, PartBox definition, boolean custom, boolean enabled, boolean turretMounted, float health, boolean destroyed, boolean onFire)
{}
