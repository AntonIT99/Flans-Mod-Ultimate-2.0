/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

/**
 * Combined passive mecha addons, readable on either side after content loading. Flags combine by OR, repair and light by maximum.
 *
 * @param stopFallDamage
 *            whether addons prevent fall damage
 * @param breakBlocksUponFalling
 *            whether addons enable landing block damage
 * @param autoRepair
 *            health points restored per second
 * @param lightLevel
 *            emitted block light, from 0 to 15
 * @param forceDark
 *            whether addons suppress nearby sky light
 */
@ApiStatus.Experimental
public record MechaUpgrades(boolean stopFallDamage, boolean breakBlocksUponFalling, float autoRepair, int lightLevel, boolean forceDark)
{
    /** No passive addon effects. */
    public static final MechaUpgrades NONE = new MechaUpgrades(false, false, 0F, 0, false);
}
