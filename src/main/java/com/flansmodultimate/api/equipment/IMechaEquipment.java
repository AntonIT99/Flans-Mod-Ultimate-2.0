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

    /** @return synchronized addon block light, 0 to 15; either side */
    default int flansmodultimateMechaLightLevel()
    {
        return 0;
    }

    /** @return synchronized addon sky-darkening state; either side */
    default boolean flansmodultimateMechaForceDark()
    {
        return false;
    }
}
