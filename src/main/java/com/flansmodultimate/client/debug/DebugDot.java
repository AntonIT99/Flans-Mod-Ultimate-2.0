package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DebugDot extends DebugColor
{
    public static final float SIZE = 0.1F;

    public DebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        super(position, lifeTime, red, green, blue);
    }

    @Override
    public void render(@NotNull PoseStack pose, @NotNull MultiBufferSource buffers, @NotNull Camera cam)
    {
        if (!ModClient.isDebug() || position == null)
            return;

        RenderSystem.disableDepthTest();

        // translate to camera-relative so drawing at origin == world pos
        Vec3 camPos = cam.getPosition();
        pose.pushPose();
        pose.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);

        float h = (float) (SIZE * 0.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.debugFilledBox());
        LevelRenderer.addChainedFilledBoxVertices(pose, vc, -h, -h, -h, h, h, h, colorRed, colorGreen, colorBlue, colorAlpha);

        pose.popPose();

        RenderSystem.enableDepthTest();
    }

    @Override
    public AABB getAABB()
    {
        double r = SIZE * 0.5;
        return new AABB(position.x - r, position.y - r, position.z - r, position.x + r, position.y + r, position.z + r);
    }
}
