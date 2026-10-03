package com.flansmodultimate.client.distant;

import com.flansmodultimate.client.render.WhiteTexture;
import com.flansmodultimate.platform.render.VertexPlatform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the distant shapes that lie within the vanilla chunks. A far-terrain renderer draws before the
 * vanilla terrain, which then covers whatever it drew there, so these shapes go through the vanilla pipeline
 * instead, depth tested against the vanilla terrain like any entity. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantBoxRenderer
{
    /** Receives one shape: boxes relative to an origin, drawn in a style. */
    @FunctionalInterface
    public interface ShapeConsumer
    {
        void accept(IDistantBoxGroup.Origin origin, List<DistantBox> boxes, DistantBoxStyle style);
    }

    /** Draws the distant contacts and explosion cues after the entities, in the camera-relative pose. */
    public static void render(PoseStack poseStack, Camera camera, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null)
            return;

        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ShapeConsumer draw = (origin, boxes, style) -> {
            Vec3 position = origin.at(partialTick);
            int light = style == DistantBoxStyle.GLOW ? LightTexture.FULL_BRIGHT
                : LevelRenderer.getLightColor(level, BlockPos.containing(position));
            emit(poseStack, buffers.getBuffer(renderType(style)), position.subtract(cameraPosition), boxes, light);
        };
        DistantContactsClient.forEachNear(draw);
        DistantExplosionCues.forEachNear(draw);
        for (DistantBoxStyle style : DistantBoxStyle.values())
            buffers.endBatch(renderType(style));
    }

    /** Adds the nearby driveable shapes to a thermal sight's heat mask, which recolours whatever it is given. */
    public static void renderHeatMask(PoseStack poseStack, VertexConsumer mask, Vec3 camera, float partialTick)
    {
        DistantContactsClient.forEachNear((origin, boxes, style) ->
            emit(poseStack, mask, origin.at(partialTick).subtract(camera), boxes, LightTexture.FULL_BRIGHT));
    }

    private static RenderType renderType(DistantBoxStyle style)
    {
        return switch (style)
        {
            case SOLID -> RenderType.entitySolid(WhiteTexture.get());
            case GLOW -> RenderType.entityTranslucentEmissive(WhiteTexture.get());
            case SMOKE -> RenderType.entityTranslucent(WhiteTexture.get());
        };
    }

    private static void emit(PoseStack poseStack, VertexConsumer consumer, Vec3 offset, List<DistantBox> boxes, int light)
    {
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        PoseStack.Pose pose = poseStack.last();
        for (DistantBox box : boxes)
        {
            if (box.argb() >>> 24 == 0 || box.maxX() <= box.minX() || box.maxY() <= box.minY() || box.maxZ() <= box.minZ())
                continue;
            emitBox(pose, consumer, box, light);
        }
        poseStack.popPose();
    }

    /** The six faces of a box, wound counter-clockwise as seen from outside. */
    private static void emitBox(PoseStack.Pose pose, VertexConsumer consumer, DistantBox box, int light)
    {
        float r = (box.argb() >> 16 & 0xFF) / 255F;
        float g = (box.argb() >> 8 & 0xFF) / 255F;
        float b = (box.argb() & 0xFF) / 255F;
        float a = (box.argb() >>> 24) / 255F;
        float x0 = box.minX();
        float y0 = box.minY();
        float z0 = box.minZ();
        float x1 = box.maxX();
        float y1 = box.maxY();
        float z1 = box.maxZ();

        quad(pose, consumer, r, g, b, a, light, 0F, -1F, 0F, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(pose, consumer, r, g, b, a, light, 0F, 1F, 0F, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, consumer, r, g, b, a, light, 0F, 0F, -1F, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, consumer, r, g, b, a, light, 0F, 0F, 1F, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(pose, consumer, r, g, b, a, light, -1F, 0F, 0F, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(pose, consumer, r, g, b, a, light, 1F, 0F, 0F, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float r, float g, float b, float a, int light,
                             float nx, float ny, float nz, float... corners)
    {
        for (int i = 0; i < 12; i += 3)
        {
            VertexPlatform.vertex(consumer, pose, corners[i], corners[i + 1], corners[i + 2], r, g, b, a, 0.5F, 0.5F,
                OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        }
    }
}
