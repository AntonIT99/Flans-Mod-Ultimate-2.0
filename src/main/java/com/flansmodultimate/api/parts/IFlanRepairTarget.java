/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Server-side repair bridge for entities wearing driveable parts. Implementations keep entity and part HP consistent. */
@ApiStatus.Experimental
public interface IFlanRepairTarget extends IFlanPartTarget
{
    /** Repairs one eligible part nearest the world-space origin, in definition HP, with the standard tool cap. Server thread. */
    boolean flansmodultimateRepair(Vec3 origin, int amount);

    /** Restores every part of a surviving entity. The caller must check operator permission. Server thread. */
    boolean flansmodultimateRestore();

    /** Requests a stationary repair session; the entity validates eligibility and owns movement/fire locks. Server thread. */
    boolean flansmodultimateStartRepair();

    /** Rebuilds one destroyed part, paying from the player's inventory and checking dependencies. Server thread. */
    boolean flansmodultimateRebuild(Player player, String part);
}
