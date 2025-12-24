package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.common.entity.Bullet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.LightTexture;
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
    public void render(@NotNull Bullet bullet, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buf, int light)
    {
        // hasLight -> use full-bright light value
        light = bullet.getConfigType().isHasLight() ? LightTexture.FULL_BRIGHT : light;


        poseStack.pushPose();

        float yaw   = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        float pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

        super.render(bullet, entityYaw, partialTicks, poseStack, buf, light);

        poseStack.popPose();
    }
}
