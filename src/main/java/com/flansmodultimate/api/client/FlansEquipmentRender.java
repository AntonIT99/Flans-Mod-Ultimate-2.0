/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import com.flansmodultimate.client.model.EquipmentAnimationSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

/** Client render support for humanoid models with a custom animation pipeline. */
@ApiStatus.Experimental
public final class FlansEquipmentRender
{
    private FlansEquipmentRender()
    {}

    /** Applies held Flan weapon and native use poses after the model's own animation; render thread only. Honors the entity's equipment policy. */
    public static void applyWeaponPose(HumanoidModel<?> model, LivingEntity holder)
    {
        EquipmentAnimationSupport.weapons(model, holder);
    }
}
