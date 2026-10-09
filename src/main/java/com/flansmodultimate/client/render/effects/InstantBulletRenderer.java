package com.flansmodultimate.client.render.effects;

import com.flansmodultimate.client.render.CustomRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstantBulletRenderer
{
    private static final List<InstantShotTrail> trails = new ArrayList<>();

    public static void addTrail(InstantShotTrail trail)
    {
        trails.add(trail);
    }

    /**
     * Trails are full-bright emissive quads, so a shader pack draws them with its glowing-entity
     * program. Each texture's trails share one draw.
     */
    public static void renderAllTrails(PoseStack poseStack, MultiBufferSource.BufferSource buffers, float partialTicks, Camera camera)
    {
        if (trails.isEmpty())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;

        Vec3 cam = camera.getPosition();
        Vec3 viewer = mc.player.getEyePosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < trails.size(); i++)
        {
            ResourceLocation texture = trails.get(i).getTexture();
            if (drawnBefore(i, texture))
                continue;
            RenderType renderType = CustomRenderType.entityEmissiveAlpha(texture, false);
            VertexConsumer vertices = buffers.getBuffer(renderType);
            for (int j = i; j < trails.size(); j++)
            {
                InstantShotTrail trail = trails.get(j);
                if (trail.getTexture().equals(texture))
                    trail.render(pose, vertices, viewer, partialTicks);
            }
            buffers.endBatch(renderType);
        }

        poseStack.popPose();
    }

    private static boolean drawnBefore(int index, ResourceLocation texture)
    {
        for (int i = 0; i < index; i++)
        {
            if (trails.get(i).getTexture().equals(texture))
                return true;
        }
        return false;
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
