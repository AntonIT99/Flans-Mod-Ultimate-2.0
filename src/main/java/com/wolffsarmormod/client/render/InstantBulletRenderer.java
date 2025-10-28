package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstantBulletRenderer
{
    private static final List<InstantShotTrail> trails = new ArrayList<>();

    public static void addTrail(InstantShotTrail trail)
    {
        // Called from your client packet on the main thread
        trails.add(trail);
    }

    /**
     * Call from a level render event (see subscriber below).
     */
    public static void renderAllTrails(PoseStack poseStack, float partialTicks, Camera camera)
    {
        if (trails.isEmpty())
            return;

        Minecraft mc = Minecraft.getInstance();
        // Safe-guard: level may be null during transitions
        if (mc.level == null)
            return;

        // Basic state
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

    /**
     * Call from ClientTickEvent (END)
     */
    public static void updateAllTrails()
    {
        for (int i = trails.size() - 1; i >= 0; i--)
        {
            if (trails.get(i).update())
                trails.remove(i);
        }
    }
}
