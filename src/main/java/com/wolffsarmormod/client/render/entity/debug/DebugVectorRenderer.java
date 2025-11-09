package com.wolffsarmormod.client.render.entity.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.ModClient;
import com.wolffsarmormod.common.entity.debug.DebugVector;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class DebugVectorRenderer extends EntityRenderer<DebugVector>
{
    public DebugVectorRenderer(EntityRendererProvider.Context pContext)
    {
        super(pContext);
    }

    @Override
    public void render(@NotNull DebugVector entity, float entityYaw, float partialTicks, @NotNull PoseStack pose, @NotNull MultiBufferSource buffers, int packedLight)
    {
        if (!ModClient.isDebug())
            return;

        // Match old behavior: draw on top of everything
        RenderSystem.disableDepthTest();

        pose.pushPose();

        // PoseStack is already at the entity's position — draw from origin to vector
        Matrix4f mat = pose.last().pose();
        float r = entity.getColorRed();
        float g = entity.getColorGreen();
        float b = entity.getColorBlue();

        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        // Start
        vc.vertex(mat, 0f, 0f, 0f)
                .color(r, g, b, 1f)
                .normal(0f, 1f, 0f)
                .endVertex();
        // End at the vector tip
        vc.vertex(mat, entity.getPointingX(), entity.getPointingY(), entity.getPointingZ())
                .color(r, g, b, 1f)
                .normal(0f, 1f, 0f).endVertex();

        pose.popPose();

        RenderSystem.enableDepthTest();

        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull DebugVector pEntity)
    {
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }
}
