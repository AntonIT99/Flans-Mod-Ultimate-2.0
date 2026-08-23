package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public class BulletRenderer extends FlanEntityRenderer<Bullet>
{
    public BulletRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void extractRenderState(Bullet bullet, State state, float partialTicks)
    {
        super.extractRenderState(bullet, state, partialTicks);
        state.yaw = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        state.pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());
        if (bullet.getConfigType() != null && bullet.getConfigType().isHasLight())
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - state.pitch));
        submitFlanState(state, poseStack, collector);
        poseStack.popPose();
        submitEntityFeatures(state, poseStack, collector, camera);
    }
}
