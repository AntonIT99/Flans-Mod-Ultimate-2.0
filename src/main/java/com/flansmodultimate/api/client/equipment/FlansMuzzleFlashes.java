/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.equipment;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.client.render.effects.MuzzleFlashRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;

/** Muzzle flashes for externally rendered content-pack weapons. Client render thread only. */
@ApiStatus.Experimental
public final class FlansMuzzleFlashes
{
    private FlansMuzzleFlashes()
    {}

    /**
     * Draws at the physical muzzle, with local +X along the barrel and Y up. Uses the client's flash-style preference,
     * the gun's flash models and size, and the platform's DefaultFlash fallback. Does not spawn particles.
     *
     * @param weapon
     *            mounted gun definition, or null for a platform weapon without a gun definition
     * @param platform
     *            vehicle definition supplying the default flash, or null
     * @param frame
     *            elapsed ticks since the shot, normally 0 through 2
     * @param scale
     *            additional entity-size multiplier; 1 for normal size
     * @param poseStack
     *            pose at the muzzle in blocks
     * @param buffers
     *            destination render buffers
     */
    public static void render(@Nullable IContentType weapon, @Nullable IContentType platform, int frame, float scale, PoseStack poseStack, MultiBufferSource buffers)
    {
        MuzzleFlashRenderer.renderExternal(weapon, platform, frame, scale, poseStack, buffers);
    }
}
