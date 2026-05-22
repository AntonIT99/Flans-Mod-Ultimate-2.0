package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelAAGun;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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

        int color = type.getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float modelScale = type.getModelScale();
        ResourceLocation texture = type.getTexture();

        poseStack.pushPose();

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderBase(aaGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        float yaw = Mth.rotLerp(partialTicks, aaGun.getPrevGunYaw(), aaGun.getGunYaw());
        poseStack.mulPose(Axis.YP.rotationDegrees(270F - yaw));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderGun(aaGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        poseStack.popPose();
    }
}
