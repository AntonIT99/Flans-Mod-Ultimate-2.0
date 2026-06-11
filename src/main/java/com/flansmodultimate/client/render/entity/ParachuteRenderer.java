package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Parachute;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class ParachuteRenderer extends FlanEntityRenderer<Parachute>
{
    public ParachuteRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
        shadowRadius = 2.0F;
    }

    @Override
    public void render(@NotNull Parachute parachute, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0, 1.5, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, parachute.yRotO, parachute.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, parachute.xRotO, parachute.getXRot())));
        super.render(parachute, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
