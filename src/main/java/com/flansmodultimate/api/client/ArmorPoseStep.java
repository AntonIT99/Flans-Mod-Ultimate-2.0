/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * A step run on a Flan armour model just before each piece is drawn, registered with
 * {@link FlansEquipmentRender#addArmorPoseStep}. It lets an addon whose entities scale or move their body parts
 * individually do the same to the armour they wear.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface ArmorPoseStep
{
    /**
     * Adjusts the parts of the armour model. The pose of the wearer's own model has already been copied to them, and
     * they are reset before the next piece, so a step may scale and move them freely. Render thread only.
     *
     * @param wearer
     *            the entity wearing the armour
     * @param armor
     *            the armour model about to be drawn
     */
    void apply(LivingEntity wearer, HumanoidModel<?> armor);
}
