package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

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
    public void submit(@NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull Camera cam)
    {
        if (!ModClient.isDebug() || position == null || pointing == null)
            return;

        double len = pointing.length();
        if (len < 1.0e-6)
            return; // nothing to draw

        // half-thickness
        float h = THICKNESS * 0.5F;

        pose.pushPose();

        // 1) translate to start (camera-relative)
        Vec3 camPos = cam.position();
        pose.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);

        // 2) rotate local +X to the segment direction
        Vector3f from = new Vector3f(1f, 0f, 0f);
        Vector3f to = new Vector3f((float)(pointing.x / len), (float)(pointing.y / len), (float)(pointing.z / len));
        pose.mulPose(new Quaternionf().rotationTo(from, to));

        // 3) draw a rectangular prism from x=[0..len], y,z=[-h..h]
        collector.submitShapeOutline(pose, Shapes.box(0F, -h, -h, (float)len, h, h),
            RenderTypes.lines(), packedColor(), 1F, false);

        pose.popPose();

    }

    private int packedColor()
    {
        return (Math.round(colorAlpha * 255F) << 24) | (Math.round(colorRed * 255F) << 16)
            | (Math.round(colorGreen * 255F) << 8) | Math.round(colorBlue * 255F);
    }
}
