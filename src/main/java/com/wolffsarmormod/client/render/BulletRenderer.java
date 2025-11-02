package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsarmormod.common.entity.Bullet;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class BulletRenderer extends FlanEntityRenderer<Bullet>
{
    public BulletRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull Bullet bullet, float entityYaw, float partialTicks, @NotNull PoseStack pose, @NotNull MultiBufferSource buf, int light)
    {
        pose.pushPose();

        float yaw   = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        float pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());

        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

        super.render(bullet, entityYaw, partialTicks, pose, buf, light);

        pose.popPose();
    }
}
