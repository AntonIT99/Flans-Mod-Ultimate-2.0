package com.wolffsmod.npcs.platform.render;

import com.flansmodultimate.api.client.FlansModelPreviews;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import noppes.npcs.client.renderer.RenderCustomNpc;
import noppes.npcs.entity.EntityCustomNpc;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Buffer adapter rather than global state: nested renders, cancellations and exceptions cannot leak context. */
public final class NpcRenderBuffers implements MultiBufferSource
{
    private final MultiBufferSource source;
    private final EntityCustomNpc npc;
    private final FlanModelEntityType type;
    private final float partialTick;
    private final PoseStack.Pose entityPose;
    private final Vec3 renderOffset;

    private NpcRenderBuffers(MultiBufferSource source, EntityCustomNpc npc, FlanModelEntityType type,
        float partialTick, PoseStack pose, Vec3 renderOffset)
    {
        this.source = source;
        this.npc = npc;
        this.type = type;
        this.partialTick = partialTick;
        this.entityPose = new PoseStack().last();
        this.entityPose.pose().set(pose.last().pose());
        this.entityPose.normal().set(pose.last().normal());
        this.renderOffset = renderOffset;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static MultiBufferSource wrap(MultiBufferSource source, EntityCustomNpc npc, float partialTick,
        PoseStack pose, RenderCustomNpc renderer)
    {
        if (!(npc.modelData.getEntity(npc) instanceof FlanModelEntity modelEntity))
            return source;
        return new NpcRenderBuffers(source, npc, (FlanModelEntityType)modelEntity.getType(), partialTick,
            pose, renderer.getRenderOffset(npc, partialTick));
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType)
    {
        VertexConsumer vertices = source.getBuffer(renderType);
        var infoType = type.getInfoType();
        ResourceLocation texture = npc.modelData.simpleRender && infoType != null ? infoType.getTexture() : npc.textureLocation;
        // Only the normal body buffer opts in. Glow overlays, invisibility and outlines retain their exact consumer.
        if (source.getClass() == MultiBufferSource.BufferSource.class && texture != null && infoType != null
            && renderType == FlansModelPreviews.getRenderType(infoType, texture))
            return new BodyVertices(vertices, texture);
        return vertices;
    }

    /** Called after the model has restored the Flan origin and coordinate basis. */
    public static boolean renderWorld(FlanModelEntityType type, PoseStack pose, VertexConsumer vertices,
        int light, int overlay, float red, float green, float blue, float alpha)
    {
        if (!(vertices instanceof NpcRenderBuffers.BodyVertices body) || body.owner().type != type || alpha != 1F)
            return false;
        NpcRenderBuffers context = body.owner();
        return FlansModelPreviews.renderWorld(type.getInfoType(), context.npc, context.partialTick,
            context.entityPose, context.renderOffset, body.texture, pose, context.source,
            light, overlay, red, green, blue, alpha);
    }

    private final class BodyVertices implements VertexConsumer
    {
        private final VertexConsumer delegate;
        private final ResourceLocation texture;

        private BodyVertices(VertexConsumer delegate, ResourceLocation texture)
        {
            this.delegate = delegate;
            this.texture = texture;
        }

        private NpcRenderBuffers owner() { return NpcRenderBuffers.this; }

        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { delegate.color(r, g, b, a); return this; }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, a); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
