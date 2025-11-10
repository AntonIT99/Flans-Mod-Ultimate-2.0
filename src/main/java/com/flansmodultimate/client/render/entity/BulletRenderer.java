package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class BulletRenderer extends FlansEntityRenderer<Bullet>
{
    public BulletRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull Bullet bullet, float entityYaw, float partialTicks, @NotNull PoseStack pose, @NotNull MultiBufferSource buf, int light)
    {
        //TODO: Read scaling from models
        pose.pushPose();

        float scale = bullet.getBulletType().getModelScale();
        pose.scale(scale, scale, scale);

        float yaw   = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        float pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());

        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

        super.render(bullet, entityYaw, partialTicks, pose, buf, light);

        pose.popPose();
    }
}
