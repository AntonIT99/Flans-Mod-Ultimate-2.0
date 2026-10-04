/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.client.render.TypeModelPreview;
import com.flansmodultimate.client.render.WorldModelPreview;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Draws static previews of content-pack models, such as for a GUI, a custom entity or a block
 * entity. Client only, on the render thread.
 *
 * <p>Supports AA guns and driveables. A preview shows the model at rest: turrets and guns face
 * forward, doors are closed and gear is down.</p>
 */
@ApiStatus.Experimental
public final class FlansModelPreviews
{
    private FlansModelPreviews() {}

    /**
     * Measures the static preview geometry, including model transforms and model scale.
     * Client render thread only; cached until model reload.
     * @param type definition to measure
     * @return geometry bounds in the same block coordinates as {@link #render}, before resting-height
     *         offsets; empty for missing, unsupported or invalid geometry
     */
    public static Optional<AABB> getBounds(IContentType type)
    {
        return type instanceof InfoType infoType ? TypeModelPreview.bounds(infoType) : Optional.empty();
    }

    /**
     * @param texture the texture to draw with, usually {@link IContentType#getTexture()} or a paintjob's
     * @return the render type the type's model is drawn with, honouring the client rendering options
     */
    public static RenderType getRenderType(IContentType type, ResourceLocation texture)
    {
        return type instanceof InfoType infoType ? TypeModelPreview.renderType(infoType, texture) : RenderType.entityCutoutNoCull(texture);
    }

    /**
     * Draws the type's model with its origin at the pose origin, in blocks, facing -Z like a vanilla
     * entity model, with Y up. For a driveable, lift it by {@code IDriveableType#getRestingHeight()}
     * to stand it on the ground. Every render pass goes to the given buffer, so glowing parts are
     * drawn like the others.
     *
     * @param red   tint multiplied with the type's own colour, like {@code green}, {@code blue} and {@code alpha}
     * @return false when the type has no model to preview
     */
    public static boolean render(IContentType type, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        return type instanceof InfoType infoType
            && TypeModelPreview.render(infoType, poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Draws a static world model with the shared GPU cache, part/track detail reduction and eligible
     * distant impostors. Client render thread only. Uses the same origin, facing and tint as {@link #render}.
     * Ordinary body passes only: callers must retain their original consumer for outlines, glint,
     * invisibility and extra texture layers. Nonstandard buffer sources retain standard geometry rendering.
     * Orthographic menu previews keep full detail. Mechas retain geometry instead of impostors.
     *
     * @param entity world instance used for position and weakly held detail history
     * @param partialTick interpolation fraction between game ticks
     * @param entityPose snapshot of the renderer's pose before living-model rotation, scaling or offsets
     * @param renderOffset renderer displacement from the interpolated entity position, in world blocks
     * @param texture actual body skin, including a custom skin or paintjob
     * @param poseStack current pose at the model origin, with Y up and facing -Z
     * @param buffers source used for the ordinary body pass
     * @param packedLight packed Minecraft light coordinates
     * @param packedOverlay packed Minecraft overlay coordinates, including damage flashes
     * @param red red tint multiplied with the definition colour
     * @param green green tint multiplied with the definition colour
     * @param blue blue tint multiplied with the definition colour
     * @param alpha opacity; partially transparent models retain geometry
     * @param type content definition to draw
     * @return false when the definition has no supported model
     */
    public static boolean renderWorld(IContentType type, Entity entity, float partialTick,
        PoseStack.Pose entityPose, Vec3 renderOffset, ResourceLocation texture, PoseStack poseStack,
        MultiBufferSource buffers, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        return type instanceof InfoType infoType && WorldModelPreview.render(infoType, entity, partialTick,
            entityPose, renderOffset, texture, poseStack, buffers, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
