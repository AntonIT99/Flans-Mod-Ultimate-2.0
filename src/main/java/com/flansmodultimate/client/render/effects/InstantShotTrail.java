package com.flansmodultimate.client.render.effects;

import com.flansmodultimate.platform.render.VertexPlatform;
import com.flansmodultimate.util.JomlUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
     * @param origin
     *            world-space start
     * @param hitPos
     *            world-space end
     * @param width
     *            trail width (blocks)
     * @param length
     *            visible length (blocks)
     * @param bulletSpeed
     *            blocks per tick (client-simulated travel)
     * @param trailTexture
     *            texture RL (e.g., "modid:textures/misc/trail.png")
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

    public ResourceLocation getTexture()
    {
        return texture;
    }

    /** Pose holds world coordinates relative to the camera; {@code viewer} is the player's eye. */
    public void render(PoseStack.Pose pose, VertexConsumer vertices, Vec3 viewer, float partialTicks)
    {
        float parametric = (ticksExisted + partialTicks) * bulletSpeed;

        // Direction from origin to hit
        Vector3f dir = JomlUtils.fromVec3(hitPos.subtract(origin));
        if (dir.lengthSquared() == 0)
            return;
        dir.normalize();

        float startT = parametric - length * 0.5f;
        float endT = parametric + length * 0.5f;

        float startX = (float) origin.x + dir.x * startT;
        float startY = (float) origin.y + dir.y * startT;
        float startZ = (float) origin.z + dir.z * startT;
        float endX = (float) origin.x + dir.x * endT;
        float endY = (float) origin.y + dir.y * endT;
        float endZ = (float) origin.z + dir.z * endT;

        // Build trail frame:
        // tangent is perpendicular to both (dir) and (toCamera)
        Vector3f toCam = new Vector3f((float) (viewer.x - hitPos.x), (float) (viewer.y - hitPos.y), (float) (viewer.z - hitPos.z));
        Vector3f tangent = dir.cross(toCam, new Vector3f());
        if (tangent.lengthSquared() == 0)
            return;
        tangent.normalize();
        // The quad faces the viewer; shader packs read this normal.
        Vector3f normal = tangent.cross(dir, new Vector3f()).normalize();
        tangent.mul(-width * 0.5f);

        // Quad: start+tan, start-tan, end-tan, end+tan
        vertex(pose, vertices, startX + tangent.x, startY + tangent.y, startZ + tangent.z, 0.0f, 0.0f, normal);
        vertex(pose, vertices, startX - tangent.x, startY - tangent.y, startZ - tangent.z, 0.0f, 1.0f, normal);
        vertex(pose, vertices, endX - tangent.x, endY - tangent.y, endZ - tangent.z, 1.0f, 1.0f, normal);
        vertex(pose, vertices, endX + tangent.x, endY + tangent.y, endZ + tangent.z, 1.0f, 0.0f, normal);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float z, float u, float v, Vector3f normal)
    {
        VertexPlatform.vertex(vertices, pose, x, y, z, 1F, 1F, 1F, 1F, u, v, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, normal.x, normal.y, normal.z);
    }
}
