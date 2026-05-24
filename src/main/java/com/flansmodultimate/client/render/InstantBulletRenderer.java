package com.flansmodultimate.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstantBulletRenderer
{
    private static final List<InstantShotTrail> trails = new ArrayList<>();

    public static void addTrail(InstantShotTrail trail)
    {
        trails.add(trail);
    }

    public static void renderAllTrails(PoseStack poseStack, float partialTicks, Camera camera)
    {
        if (trails.isEmpty())
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        for (InstantShotTrail t : trails)
        {
            t.render(poseStack, partialTicks);
        }

        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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
