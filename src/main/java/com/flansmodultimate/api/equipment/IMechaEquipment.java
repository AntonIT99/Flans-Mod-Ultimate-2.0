/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.equipment;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.item.ItemStack;

/** Equipment drawn on an entity wearing a mecha model. Implementations synchronize their tools to tracking clients. */
@ApiStatus.Experimental
public interface IMechaEquipment
{
    /** Returns the tool on the requested arm, or an empty stack; either side. The right arm is {@code left = false}. */
    ItemStack flansmodultimateMechaTool(boolean left);
}
