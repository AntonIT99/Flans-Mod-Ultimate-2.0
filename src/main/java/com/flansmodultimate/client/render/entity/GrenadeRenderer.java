package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Grenade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

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
    public void render(@NotNull Grenade grenade, float entityYaw, float partialTicks, @NotNull PoseStack pose, @NotNull MultiBufferSource buf, int light)
    {
        //TODO: Read scaling from model classes
        pose.pushPose();

        float scale = grenade.getGrenadeType().getModelScale();
        pose.scale(scale, scale, scale);

        if (grenade.isStuck())
        {
            pose.mulPose(Axis.YP.rotationDegrees(180F - grenade.getAxes().getYaw()));
            pose.mulPose(Axis.ZP.rotationDegrees(grenade.getAxes().getPitch()));
            pose.mulPose(Axis.XP.rotationDegrees(grenade.getAxes().getRoll()));
        }
        else
        {
            float dYaw = Mth.wrapDegrees(grenade.getAxes().getYaw() - grenade.yRotO);
            float dPitch = Mth.wrapDegrees(grenade.getAxes().getPitch() - grenade.xRotO);
            float dRoll = Mth.wrapDegrees(grenade.getAxes().getRoll() - grenade.getPrevRotationRoll());

            float yaw = 180F - (grenade.yRotO + dYaw * partialTicks);
            float pitch = grenade.xRotO + dPitch * partialTicks;
            float roll = grenade.getPrevRotationRoll() + dRoll * partialTicks;

            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(pitch));
            pose.mulPose(Axis.XP.rotationDegrees(roll));
        }

        super.render(grenade, entityYaw, partialTicks, pose, buf, light);

        pose.popPose();
    }
}
