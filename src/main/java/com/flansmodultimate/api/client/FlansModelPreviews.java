/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.client.render.TypeModelPreview;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

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
}
