package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Renderer for logical seat/wheel proxies whose visuals belong to the root model. */
public final class InvisibleEntityRenderer<T extends Entity> extends EntityRenderer<T>
{
    private static final ResourceLocation EMPTY = FlansMod.defaultFallbackTexture;

    public InvisibleEntityRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(@NotNull T entity, float yaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight)
    {
        // Intentionally empty. The parent DriveableRenderer draws these parts.
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        return EMPTY;
    }
}
