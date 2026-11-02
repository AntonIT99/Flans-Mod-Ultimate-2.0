/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.common.guns.EmplacementSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Server-thread placement of carried deployable guns by addon-controlled living entities. */
@ApiStatus.Experimental
public final class FlansEmplacements
{
    private FlansEmplacements()
    {}

    /**
     * Places one carried deployable gun above a solid support block, facing the horizontal direction.
     * Validates support, free space and the operator's standing position. Removes one item only after successful placement.
     *
     * @return the placed mount, or empty when placement fails; server thread only
     */
    public static Optional<IEmplacement> deploy(LivingEntity operator, ItemStack weapon, BlockPos support, Direction facing)
    {
        return EmplacementSupport.deploy(operator, weapon, support, facing);
    }
}
