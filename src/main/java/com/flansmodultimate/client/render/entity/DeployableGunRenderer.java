package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelMG;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

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

        float baseYaw = Direction.from2DDataValue(deployedGun.getGunDirection()).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - baseYaw));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderBipod(deployedGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        float aimPitch = getAimPitch(deployedGun, partialTicks);
        float aimWorldYaw = getAimWorldYaw(deployedGun, partialTicks, baseYaw);
        float aimLocalYaw = Mth.wrapDegrees(aimWorldYaw - baseYaw);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-aimLocalYaw));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderGun(deployedGun, aimPitch, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        poseStack.popPose();
    }

    private static float getAimPitch(@NotNull DeployedGun gun, float partialTicks)
    {
        Player gunner = getPlayerGunner(gun);

        float pitchDeg;
        if (gunner != null)
            pitchDeg = Mth.lerp(partialTicks, gunner.xRotO, gunner.getXRot());
        else
            pitchDeg = Mth.lerp(partialTicks, gun.xRotO, gun.getXRot());

        float top = gun.getConfigType().getTopViewLimit();
        float bottom = gun.getConfigType().getBottomViewLimit();
        if (top > bottom)
        {
            float t = top; top = bottom;
            bottom = t;
        }

        return Mth.clamp(pitchDeg, top, bottom);
    }

    private static float getAimWorldYaw(@NotNull DeployedGun gun, float partialTicks, float baseYaw)
    {
        Player player = getPlayerGunner(gun);

        float viewYaw;
        if (player != null)
            viewYaw = Mth.rotLerp(partialTicks, player.yRotO, player.getYRot());
        else
            viewYaw = Mth.rotLerp(partialTicks, gun.yRotO, gun.getYRot());

        float localYaw = Mth.wrapDegrees(viewYaw - baseYaw);
        float side = gun.getConfigType().getSideViewLimit();
        localYaw = Mth.clamp(localYaw, -side, side);

        return baseYaw + localYaw;
    }

    @Nullable
    private static Player getPlayerGunner(DeployedGun gun)
    {
        if (gun.getFirstPassenger() instanceof Player p)
            return p;
        return null;
    }
}