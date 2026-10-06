/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Read-only Flan weapon inspection, including stack attachments and the selected ammunition.
 * Damage is the generic tooltip estimate; actual impacts resolve target, ammunition and belt rules.
 * Speed is blocks/tick, spread uses Flan units, and all times are ticks. Readable on either side after content loading.
 */
@ApiStatus.Experimental
public record EquippedWeaponProperties(boolean ranged, boolean throwable, boolean shield, double damage, double meleeDamage, double speed, double spread, int projectiles,
    double shotDelay, double reloadTime, int chargeTime, int meleeTime, String firingSound, boolean automaticFire, float spinUpThreshold, float spinUpMaximum)
{}
