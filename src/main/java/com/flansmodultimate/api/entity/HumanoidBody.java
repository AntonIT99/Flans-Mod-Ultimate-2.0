/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.entity;

import org.jetbrains.annotations.ApiStatus;

/**
 * The proportions of a body drawn with the vanilla humanoid model, as Flan's Mod Ultimate needs them to place its head,
 * body, arm and leg hitboxes where the model is drawn. The parts are posed as the vanilla player model poses them.
 * Immutable and safe to pass between threads.
 *
 * <p>
 * Model space is the one of {@code HumanoidModel}: model pixels (sixteenths of a block), X towards the body's left,
 * Y down and Z forward, before the renderer flips and scales the model.
 * </p>
 *
 * @param scaleX
 *            how much the renderer scales the whole model sideways; a player is drawn at {@value #PLAYER_SCALE}
 * @param scaleY
 *            how much the renderer scales the whole model upwards
 * @param scaleZ
 *            how much the renderer scales the whole model forwards
 * @param offsetY
 *            how far the renderer moves the whole model up from the entity's feet, in blocks, including any crouching
 *            offset; negative moves it down
 * @param head
 *            the head, whose box holds headshots
 * @param body
 *            the torso
 * @param rightArm
 *            the right arm
 * @param leftArm
 *            the left arm
 * @param rightLeg
 *            the right leg
 * @param leftLeg
 *            the left leg
 */
@ApiStatus.Experimental
public record HumanoidBody(float scaleX, float scaleY, float scaleZ, float offsetY, Part head, Part body, Part rightArm, Part leftArm, Part rightLeg, Part leftLeg)
{
    /** The size the player renderer draws its model at. */
    public static final float PLAYER_SCALE = 0.9375F;

    /** Keeps the scales finite and positive, the offset finite and every part present. */
    public HumanoidBody
    {
        scaleX = Part.scale(scaleX);
        scaleY = Part.scale(scaleY);
        scaleZ = Part.scale(scaleZ);
        offsetY = Float.isFinite(offsetY) ? offsetY : 0F;
        head = head == null ? Part.STANDARD : head;
        body = body == null ? Part.STANDARD : body;
        rightArm = rightArm == null ? Part.STANDARD : rightArm;
        leftArm = leftArm == null ? Part.STANDARD : leftArm;
        rightLeg = rightLeg == null ? Part.STANDARD : rightLeg;
        leftLeg = leftLeg == null ? Part.STANDARD : leftLeg;
    }

    /**
     * @param scale
     *            how much the renderer scales the whole model on every axis
     * @param offsetY
     *            how far the renderer moves the whole model up, in blocks
     * @return a body with the vanilla proportions of every part
     */
    public static HumanoidBody standard(float scale, float offsetY)
    {
        return new HumanoidBody(scale, scale, scale, offsetY, Part.STANDARD, Part.STANDARD, Part.STANDARD, Part.STANDARD, Part.STANDARD, Part.STANDARD);
    }

    /**
     * How one part differs from the vanilla model: moved, then scaled about its pivot, as a model part with its own
     * offset and scale is drawn.
     *
     * @param offsetX
     *            how far the part's pivot moves along X, in model pixels
     * @param offsetY
     *            how far the part's pivot moves along Y, in model pixels; positive moves it down
     * @param offsetZ
     *            how far the part's pivot moves along Z, in model pixels
     * @param scaleX
     *            how much the part is scaled along X about its pivot
     * @param scaleY
     *            how much the part is scaled along Y about its pivot
     * @param scaleZ
     *            how much the part is scaled along Z about its pivot
     */
    @ApiStatus.Experimental
    public record Part(float offsetX, float offsetY, float offsetZ, float scaleX, float scaleY, float scaleZ)
    {
        /** A part as the vanilla model has it. */
        public static final Part STANDARD = new Part(0F, 0F, 0F, 1F, 1F, 1F);

        /** Keeps the offsets finite, without negative zero, and the scales finite and positive. */
        public Part
        {
            offsetX = offset(offsetX);
            offsetY = offset(offsetY);
            offsetZ = offset(offsetZ);
            scaleX = scale(scaleX);
            scaleY = scale(scaleY);
            scaleZ = scale(scaleZ);
        }

        private static float scale(float scale)
        {
            return Float.isFinite(scale) && scale > 0F ? scale : 1F;
        }

        /** Adding zero turns negative zero into zero, so that parts placed alike are equal. */
        private static float offset(float offset)
        {
            return Float.isFinite(offset) ? offset + 0F : 0F;
        }
    }
}
