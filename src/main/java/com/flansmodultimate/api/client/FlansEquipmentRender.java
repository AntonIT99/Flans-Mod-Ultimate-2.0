/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import com.flansmodultimate.client.model.EquipmentAnimationSupport;
import com.flansmodultimate.client.model.HeldItemTransforms;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Client render support for humanoid models with a custom animation pipeline. */
@ApiStatus.Experimental
public final class FlansEquipmentRender
{
    private FlansEquipmentRender()
    {}

    /** Applies held Flan weapon and native use poses, preserving melee swings, after the model's own animation; render thread only. Honors the entity's equipment policy. */
    public static void applyWeaponPose(HumanoidModel<?> model, LivingEntity holder)
    {
        EquipmentAnimationSupport.weapons(model, holder);
    }

    /**
     * Applies the saved per-hand item transform in a custom held-item layer, after positioning the hand and before drawing
     * the item. Render thread only. Does nothing for sprites and shields. Vanilla held layers already call this: do not
     * apply it twice. Offsets are model pixels, rotations are degrees about the local display axes; see HeldItemTransform.
     */
    public static void applyHeldItemTransform(PoseStack pose, LivingEntity holder, ItemStack stack, InteractionHand hand, ItemDisplayContext context)
    {
        HeldItemTransforms.apply(pose, holder, stack, hand, context);
    }

    /**
     * Registers a step run on every Flan armour model just before each piece is drawn. Call once, during client setup.
     *
     * @param step
     *            adjusts the armour model for its wearer
     */
    public static void addArmorPoseStep(ArmorPoseStep step)
    {
        EquipmentAnimationSupport.addArmorPoseStep(step);
    }
}
