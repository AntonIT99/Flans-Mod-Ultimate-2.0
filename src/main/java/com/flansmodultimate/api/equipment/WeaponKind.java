/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * What kind of weapon a Flan gun item is, by its definition alone. It agrees with the same three flags of
 * {@link EquippedWeaponProperties} but reads neither the stack's attachments nor its ammunition, so it costs next to nothing
 * and suits code that asks every tick what an entity holds. Readable on either side after content loading.
 *
 * @param ranged
 *            whether the weapon attacks at range: it takes ammunition or is thrown itself
 * @param throwable
 *            whether the weapon is thrown itself rather than fired
 * @param shield
 *            whether the weapon is a shield
 */
@ApiStatus.Experimental
public record WeaponKind(boolean ranged, boolean throwable, boolean shield)
{}
