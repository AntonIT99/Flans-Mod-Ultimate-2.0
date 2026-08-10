package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Flag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Lightweight textured recreation of the legacy flag and three-block flagpole model. */
public final class TeamObjectRenderer<T extends Entity> extends EntityRenderer<T>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/entity/teams/flagpole.png");

    public TeamObjectRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.2F;
    }

    @Override
    public void render(@NotNull T entity, float yaw, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        if (entity instanceof Flag flag)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(180F - entityRenderDispatcher.camera.getYRot()));
            int colour = flag.getColour();
            float red = (colour >> 16 & 255) / 255F;
            float green = (colour >> 8 & 255) / 255F;
            float blue = (colour & 255) / 255F;
            quad(poseStack.last(), vertices, -0.5F, -0.25F, 0F, 0.5F, 0.75F, 0F,
                0F, 0F, 0.25F, 0.5F, red, green, blue, packedLight);
        }
        else
        {
            // Two crossed strips remain legible from every camera angle and use
            // the same pole section of the original 64x32 texture atlas.
            quad(poseStack.last(), vertices, -0.0625F, 0F, 0F, 0.0625F, 3F, 0F,
                0F, 0.5F, 0.0625F, 1F, 1F, 1F, 1F, packedLight);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            quad(poseStack.last(), vertices, -0.0625F, 0F, 0F, 0.0625F, 3F, 0F,
                0F, 0.5F, 0.0625F, 1F, 1F, 1F, 1F, packedLight);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vertices,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float u0, float v0, float u1, float v1,
                             float red, float green, float blue, int light)
    {
        vertex(pose, vertices, x0, y1, z0, u0, v0, red, green, blue, light);
        vertex(pose, vertices, x1, y1, z1, u1, v0, red, green, blue, light);
        vertex(pose, vertices, x1, y0, z1, u1, v1, red, green, blue, light);
        vertex(pose, vertices, x0, y0, z0, u0, v1, red, green, blue, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float z,
                               float u, float v, float red, float green, float blue, int light)
    {
        vertices.addVertex(pose.pose(), x, y, z).setColor(red, green, blue, 1F).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0F, 0F, 1F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        return TEXTURE;
    }
}
