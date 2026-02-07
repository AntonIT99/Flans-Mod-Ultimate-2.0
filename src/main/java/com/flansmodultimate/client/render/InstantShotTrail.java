package com.flansmodultimate.client.render;

import com.flansmod.common.vector.Vector3f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class InstantShotTrail
{
    private final Vec3 origin;
    private final Vec3 hitPos;
    private final float width;
    private final float length;
    private final float bulletSpeed; // blocks per tick
    private final double distanceToTarget;
    private int ticksExisted;
    private final ResourceLocation texture;

    /**
     * @param origin       world-space start
     * @param hitPos       world-space end
     * @param width        trail width (blocks)
     * @param length       visible length (blocks)
     * @param bulletSpeed  blocks per tick (client-simulated travel)
     * @param trailTexture texture RL (e.g., "modid:textures/misc/trail.png")
     */
    public InstantShotTrail(Vec3 origin, Vec3 hitPos, float width, float length, float bulletSpeed, ResourceLocation trailTexture)
    {
        this.origin = origin;
        this.hitPos = hitPos;
        this.width = width;
        this.length = length;
        this.bulletSpeed = bulletSpeed;
        this.ticksExisted = 0;
        this.texture = trailTexture;

        Vec3 dPos = hitPos.subtract(origin);
        double dist = dPos.length();
        if (Math.abs(dist) > 300.0f)
            dist = 300.0f;
        this.distanceToTarget = dist;
    }

    /** Return true if this needs deleting */
    public boolean update()
    {
        ticksExisted++;
        return ticksExisted * bulletSpeed >= distanceToTarget - length;
    }

    public void render(PoseStack poseStack, float partialTicks)
    {
        // Camera/player vectors
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        // Bind texture per trail (cheap)
        RenderSystem.setShaderTexture(0, texture);

        float parametric = (ticksExisted + partialTicks) * bulletSpeed;

        // Direction from origin to hit
        Vector3f dir = new Vector3f(hitPos.subtract(origin));
        if (dir.lengthSquared() == 0)
            return;
        dir.normalise();

        float startT = parametric - length * 0.5f;
        float endT = parametric + length * 0.5f;

        Vector3f start = new Vector3f(origin.x + dir.x * startT, origin.y + dir.y * startT, origin.z + dir.z * startT);
        Vector3f end = new Vector3f(origin.x + dir.x * endT, origin.y + dir.y * endT, origin.z + dir.z * endT);

        // Build trail frame:
        // tangent is perpendicular to both (dir) and (toCamera)
        Vector3f toCam = new Vector3f((float) player.getX() - hitPos.x, (float) player.getEyeY() - hitPos.y, (float) player.getZ() - hitPos.z);
        Vector3f tangent = Vector3f.cross(new Vector3f(dir), toCam, null);
        tangent.normalise().scale(-width * 0.5f);

        Matrix4f pose = poseStack.last().pose();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        // Quad: start+tan, start-tan, end-tan, end+tan
        putPosUv(buf, pose, Vector3f.add(start, tangent, null), 0.0f, 0.0f);
        putPosUv(buf, pose, Vector3f.sub(start, tangent, null), 0.0f, 1.0f);
        putPosUv(buf, pose, Vector3f.sub(end, tangent, null), 1.0f, 1.0f);
        putPosUv(buf, pose, Vector3f.add(end, tangent, null), 1.0f, 0.0f);

        tess.end();
    }

    private static void putPosUv(BufferBuilder buf, Matrix4f pose, Vector3f p, float u, float v)
    {
        buf.vertex(pose, p.x, p.y, p.z).uv(u, v).endVertex();
    }
}