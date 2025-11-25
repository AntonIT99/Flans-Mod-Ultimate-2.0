package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Grenade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class GrenadeRenderer extends FlansEntityRenderer<Grenade>
{
    public GrenadeRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull Grenade grenade, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buf, int light)
    {
        shadowRadius = grenade.getHitboxSize();
        // hasLight -> use full-bright light value
        light = grenade.getConfigType().isHasLight() ? LightTexture.FULL_BRIGHT : light;

        poseStack.pushPose();

        if (grenade.isStuck())
        {
            poseStack.mulPose(Axis.YP.rotationDegrees(180F - grenade.getAxes().getYaw()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(grenade.getAxes().getPitch()));
            poseStack.mulPose(Axis.XP.rotationDegrees(grenade.getAxes().getRoll()));
        }
        else
        {
            float dYaw = Mth.wrapDegrees(grenade.getAxes().getYaw() - grenade.yRotO);
            float dPitch = Mth.wrapDegrees(grenade.getAxes().getPitch() - grenade.xRotO);
            float dRoll = Mth.wrapDegrees(grenade.getAxes().getRoll() - grenade.getPrevRotationRoll());

            float yaw = 180F - (grenade.yRotO + dYaw * partialTicks);
            float pitch = grenade.xRotO + dPitch * partialTicks;
            float roll = grenade.getPrevRotationRoll() + dRoll * partialTicks;

            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        }

        super.render(grenade, entityYaw, partialTicks, poseStack, buf, light);

        poseStack.popPose();
    }
}
