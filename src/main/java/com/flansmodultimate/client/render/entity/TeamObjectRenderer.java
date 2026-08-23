package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.entity.Flag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/** Lightweight textured recreation of the legacy flag and three-block flagpole model. */
public final class TeamObjectRenderer<T extends Entity> extends EntityRenderer<T, TeamObjectRenderer.State>
{
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/entity/teams/flagpole.png");

    public TeamObjectRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.2F;
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick)
    {
        super.extractRenderState(entity, state, partialTick);
        state.isFlag = entity instanceof Flag;
        state.colour = state.isFlag ? ((Flag)entity).getColour() : 0xFFFFFF;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        if (state.isFlag)
            poseStack.mulPose(Axis.YP.rotationDegrees(180F - camera.yRot));

        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, vertices) -> {
            if (state.isFlag)
            {
                float red = (state.colour >> 16 & 255) / 255F;
                float green = (state.colour >> 8 & 255) / 255F;
                float blue = (state.colour & 255) / 255F;
                quad(pose, vertices, -0.5F, -0.25F, 0F, 0.5F, 0.75F, 0F,
                    0F, 0F, 0.25F, 0.5F, red, green, blue, state.lightCoords);
            }
            else
            {
                quad(pose, vertices, -0.0625F, 0F, 0F, 0.0625F, 3F, 0F,
                    0F, 0.5F, 0.0625F, 1F, 1F, 1F, 1F, state.lightCoords);
            }
        });
        if (!state.isFlag)
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, vertices) ->
                quad(pose, vertices, -0.0625F, 0F, 0F, 0.0625F, 3F, 0F,
                    0F, 0.5F, 0.0625F, 1F, 1F, 1F, 1F, state.lightCoords));
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
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

    public static final class State extends EntityRenderState
    {
        private boolean isFlag;
        private int colour;
    }
}
