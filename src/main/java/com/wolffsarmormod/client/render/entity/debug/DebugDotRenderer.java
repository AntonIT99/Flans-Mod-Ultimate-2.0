package com.wolffsarmormod.client.render.entity.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.common.entity.debug.DebugDot;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class DebugDotRenderer extends EntityRenderer<DebugDot>
{
    public DebugDotRenderer(EntityRendererProvider.Context pContext)
    {
        super(pContext);
    }

    @Override
    public void render(@NotNull DebugDot entity, float entityYaw, float partialTicks, @NotNull PoseStack pose, @NotNull MultiBufferSource buffers, int packedLight)
    {
        if (!ModClient.isDebug())
            return;

        // Make it always visible like the old code (depth disabled while drawing)
        RenderSystem.disableDepthTest();

        pose.pushPose();
        // The renderer is already at the entity’s position; just draw around origin
        float s = 0.05f; // ~5 cm – tweak to match your desired “point size”

        float r = entity.getColorRed();
        float g = entity.getColorGreen();
        float b = entity.getColorBlue();
        float a = 1.0f;

        // Filled little cube (looks like a big point)
        VertexConsumer quad = buffers.getBuffer(RenderType.solid());
        LevelRenderer.addChainedFilledBoxVertices(pose, quad, -s, -s, -s, s,  s,  s, r,  g,  b,  a);

        // Optional: thin outline to make it pop
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, -s, -s, -s,  s, s, s, r,  g,  b,  1.0f);

        pose.popPose();

        RenderSystem.enableDepthTest();
        // no texture binding needed; we didn’t use textured quads
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull DebugDot pEntity)
    {
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }
}
