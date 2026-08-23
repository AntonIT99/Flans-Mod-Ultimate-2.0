package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Parachute;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

public class ParachuteRenderer extends FlanEntityRenderer<Parachute>
{
    public ParachuteRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
        shadowRadius = 2.0F;
    }

    @Override
    public void extractRenderState(Parachute parachute, State state, float partialTicks)
    {
        super.extractRenderState(parachute, state, partialTicks);
        state.yaw = -Mth.rotLerp(partialTicks, parachute.yRotO, parachute.getYRot());
        state.pitch = -Mth.lerp(partialTicks, parachute.xRotO, parachute.getXRot());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        submitFlanState(state, poseStack, collector);
        poseStack.popPose();
        submitEntityFeatures(state, poseStack, collector, camera);
    }
}
