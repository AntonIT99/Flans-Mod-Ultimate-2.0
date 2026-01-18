package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelMG;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DeployableGunRenderer extends FlanEntityRenderer<DeployedGun>
{
    public DeployableGunRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull DeployedGun deployedGun, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        ModelMG model = ModelCache.getOrLoadDeployableGunModel(deployedGun.getConfigType());
        if (model == null)
            return;

        int color = deployedGun.getConfigType().getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float modelScale = deployedGun.getConfigType().getModelScale();
        ResourceLocation texture = deployedGun.getConfigType().getDeployableTexture();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - deployedGun.getGunDirection() * 90F));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderBipod(deployedGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, deployedGun.yRotO, deployedGun.getYRot())));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderGun(deployedGun, partialTicks, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        poseStack.popPose();
    }
}