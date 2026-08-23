package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Grenade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public class GrenadeRenderer extends FlanEntityRenderer<Grenade>
{
    public GrenadeRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void extractRenderState(Grenade grenade, State state, float partialTicks)
    {
        super.extractRenderState(grenade, state, partialTicks);
        if (grenade.isStuck())
        {
            state.yaw = 180F - grenade.getAxes().getYaw();
            state.pitch = grenade.getAxes().getPitch();
            state.roll = grenade.getAxes().getRoll();
        }
        else
        {
            state.yaw = 180F - (grenade.yRotO + Mth.wrapDegrees(grenade.getAxes().getYaw() - grenade.yRotO) * partialTicks);
            state.pitch = grenade.xRotO + Mth.wrapDegrees(grenade.getAxes().getPitch() - grenade.xRotO) * partialTicks;
            state.roll = grenade.getPrevRotationRoll()
                + Mth.wrapDegrees(grenade.getAxes().getRoll() - grenade.getPrevRotationRoll()) * partialTicks;
        }
        if (grenade.getConfigType() != null && grenade.getConfigType().isHasLight())
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.roll));
        submitFlanState(state, poseStack, collector);
        poseStack.popPose();
        submitEntityFeatures(state, poseStack, collector, camera);
    }
}
