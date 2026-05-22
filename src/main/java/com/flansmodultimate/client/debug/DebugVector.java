package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entity for debugging purposes.
 * On the client side a line (Vector) between the position of the entity and its pointing location is rendered
 */
public class DebugVector extends DebugColor
{
    public static final float THICKNESS = 0.05F;

    @Getter
    protected final Vec3 pointing;

    public DebugVector(Vec3 startPosition, Vec3 direction, int lifeTime, float red, float green, float blue)
    {
        super(startPosition, lifeTime, red, green, blue);
        pointing = direction;
    }

    @Override
    public AABB getAABB()
    {
        double r = THICKNESS * 0.5;

        double minX = Math.min(position.x, pointing.x) - r;
        double minY = Math.min(position.y, pointing.y) - r;
        double minZ = Math.min(position.z, pointing.z) - r;

        double maxX = Math.max(position.x, pointing.x) + r;
        double maxY = Math.max(position.y, pointing.y) + r;
        double maxZ = Math.max(position.z, pointing.z) + r;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void render(@NotNull PoseStack pose, @NotNull MultiBufferSource buffers, @NotNull Camera cam)
    {
        if (!ModClient.isDebug() || position == null || pointing == null)
            return;

        double len = pointing.length();
        if (len < 1.0e-6)
            return; // nothing to draw

        // half-thickness
        float h = THICKNESS * 0.5F;

        RenderSystem.disableDepthTest();

        pose.pushPose();

        // 1) translate to start (camera-relative)
        Vec3 camPos = cam.getPosition();
        pose.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);

        // 2) rotate local +X to the segment direction
        Vector3f from = new Vector3f(1f, 0f, 0f);
        Vector3f to = new Vector3f((float)(pointing.x / len), (float)(pointing.y / len), (float)(pointing.z / len));
        pose.mulPose(new Quaternionf().rotationTo(from, to));

        // 3) draw a rectangular prism from x=[0..len], y,z=[-h..h]
        VertexConsumer solid = buffers.getBuffer(RenderType.debugFilledBox());
        LevelRenderer.addChainedFilledBoxVertices(pose, solid, 0f, -h, -h, (float) len, h, h, colorRed, colorGreen, colorBlue, colorAlpha);

        pose.popPose();

        RenderSystem.enableDepthTest();
    }
}
