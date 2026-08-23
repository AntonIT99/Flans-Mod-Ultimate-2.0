package com.flansmodultimate.client.debug;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public class DebugDot extends DebugColor
{
    public static final float SIZE = 0.1F;

    public DebugDot(Vec3 position, int lifeTime, float red, float green, float blue)
    {
        super(position, lifeTime, red, green, blue);
    }

    @Override
    public void submit(@NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull Camera cam)
    {
        if (!ModClient.isDebug() || position == null)
            return;

        // translate to camera-relative so drawing at origin == world pos
        Vec3 camPos = cam.position();
        pose.pushPose();
        pose.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);

        float h = (float) (SIZE * 0.5);
        collector.submitShapeOutline(pose, Shapes.box(-h, -h, -h, h, h, h),
            RenderTypes.lines(), packedColor(), 1F, false);

        pose.popPose();

    }

    private int packedColor()
    {
        return (Math.round(colorAlpha * 255F) << 24) | (Math.round(colorRed * 255F) << 16)
            | (Math.round(colorGreen * 255F) << 8) | Math.round(colorBlue * 255F);
    }

    @Override
    public AABB getAABB()
    {
        double r = SIZE * 0.5;
        return new AABB(position.x - r, position.y - r, position.z - r, position.x + r, position.y + r, position.z + r);
    }
}
