package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelAAGun;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class AAGunRenderer extends FlanEntityRenderer<AAGun>
{
    public AAGunRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull AAGun aaGun, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        AAGunType type = aaGun.getConfigType();
        if (type == null)
            return;

        if (!(ModelCache.getOrLoadTypeModel(type) instanceof ModelAAGun model))
            return;

        float red = getRed(type);
        float green = getGreen(type);
        float blue = getBlue(type);
        float modelScale = type.getModelScale();
        ResourceLocation texture = type.getTexture();
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);

        poseStack.pushPose();

        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            model.renderBase(aaGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        float yaw = Mth.rotLerp(partialTicks, aaGun.getPrevGunYaw(), aaGun.getRenderGunYaw());
        float pitch = Mth.lerp(partialTicks, aaGun.getPrevGunPitch(), aaGun.getRenderGunPitch());
        if (aaGun.getFirstPassenger() instanceof LivingEntity gunner)
        {
            // The gun's synced aim updates once per tick; the rider's view also has
            // the in-between angles needed for smooth local and remote rendering.
            yaw = Mth.rotLerp(partialTicks, gunner.yRotO, gunner.getYRot());
            float top = -Math.abs(type.getTopViewLimit());
            float bottom = type.getBottomViewLimit();
            pitch = Mth.clamp(Mth.lerp(partialTicks, gunner.xRotO, gunner.getXRot()),
                Math.min(top, bottom), Math.max(top, bottom));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(270F - yaw));

        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            model.renderGun(aaGun, pitch, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        poseStack.popPose();
    }
}
