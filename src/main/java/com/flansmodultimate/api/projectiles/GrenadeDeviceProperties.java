/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.projectiles;

import org.jetbrains.annotations.ApiStatus;

/**
 * Deployment properties read on either side after content loading. Radii are in blocks; delays are in ticks.
 *
 * @param livingTrigger
 *            living-entity proximity radius; zero when absent
 * @param driveableTrigger
 *            vehicle proximity radius; zero when absent
 * @param armingTicks
 *            delay before detonation is permitted
 * @param remote
 *            whether the owner can detonate the device remotely
 * @param supplyBag
 *            whether this is a deployed healing, effect or ammunition bag
 */
@ApiStatus.Experimental
public record GrenadeDeviceProperties(double livingTrigger, double driveableTrigger, int armingTicks, boolean remote, boolean supplyBag)
{
    /** @return whether either proximity trigger makes this device a mine */
    public boolean mine()
    {
        return livingTrigger > 0D || driveableTrigger > 0D;
    }
}
