package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstantBulletRenderer
{
    private static final List<InstantShotTrail> trails = new ArrayList<>();
    private static final ContextKey<List<InstantShotTrail.Snapshot>> TRAIL_STATE = new ContextKey<>(
        Identifier.fromNamespaceAndPath("flansmodultimate", "instant_shot_trails"));

    public static void addTrail(InstantShotTrail trail)
    {
        trails.add(trail);
    }

    public static void extract(LevelRenderState levelRenderState, float partialTicks, Camera camera)
    {
        Vec3 cameraPosition = camera.position();
        List<InstantShotTrail.Snapshot> snapshots = trails.stream()
            .map(trail -> trail.extract(partialTicks, cameraPosition))
            .filter(java.util.Objects::nonNull)
            .toList();
        levelRenderState.setRenderData(TRAIL_STATE, snapshots);
    }

    public static void submit(LevelRenderState levelRenderState, PoseStack poseStack, SubmitNodeCollector collector)
    {
        List<InstantShotTrail.Snapshot> snapshots = levelRenderState.getRenderData(TRAIL_STATE);
        if (snapshots == null)
            return;
        for (InstantShotTrail.Snapshot snapshot : snapshots)
        {
            collector.submitCustomGeometry(poseStack, CustomRenderType.entityEmissiveAlpha(snapshot.texture()), (pose, vertices) -> {
                vertex(vertices, pose, snapshot.startTop(), 0F, 0F);
                vertex(vertices, pose, snapshot.startBottom(), 0F, 1F);
                vertex(vertices, pose, snapshot.endBottom(), 1F, 1F);
                vertex(vertices, pose, snapshot.endTop(), 1F, 0F);
            });
        }
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
                               org.joml.Vector3f position, float u, float v)
    {
        vertices.addVertex(pose.pose(), position.x, position.y, position.z)
            .setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0F, 1F, 0F);
    }

    public static void updateAllTrails()
    {
        Iterator<InstantShotTrail> iterator = trails.iterator();
        while (iterator.hasNext())
        {
            if (iterator.next().update())
                iterator.remove();
        }
    }
}
