package com.flansmod.client.model;

import com.flansmodultimate.client.model.ModelBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;

public abstract class ModelMuzzleFlash extends ModelBase
{
    /** Flash callers supply an emissive buffer, so include every part regardless of its glow flags. */
    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        forEachModelBox(part -> part.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale()));
    }

    @Override
    public abstract ResourceLocation getTexture();
}
