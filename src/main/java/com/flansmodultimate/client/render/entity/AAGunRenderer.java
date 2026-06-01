package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.entity.AAGun;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class AAGunRenderer extends FlanEntityRenderer<AAGun>
{
    public AAGunRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull AAGun gun, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        IModelBase model = ModelCache.getOrLoadAAGunModel(gun.getConfigType());
        if (model == null)
            return;

        if (!(model instanceof ModelAAGun aaModel))
        {
            LegacyTransformApplier.renderModel(model, gun.getConfigType(), getTextureLocation(gun), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            return;
        }

        int color = gun.getConfigType().getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float modelScale = gun.getConfigType().getModelScale();
        ResourceLocation texture = getTextureLocation(gun);

        poseStack.pushPose();

        float baseYaw = Direction.from2DDataValue(gun.getGunDirection()).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - baseYaw));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            for (ModelRendererTurbo part : aaModel.baseModel)
            {
                if (part != null)
                    part.render(poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);
            }
        }

        float aimPitch = getAimPitch(gun, partialTicks);
        float aimWorldYaw = getAimWorldYaw(gun, partialTicks, baseYaw);
        float aimLocalYaw = Mth.wrapDegrees(aimWorldYaw - baseYaw);

        poseStack.mulPose(Axis.YP.rotationDegrees(-aimLocalYaw));

        float pitchRad = -aimPitch * Mth.DEG_TO_RAD;

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            for (ModelRendererTurbo part : aaModel.seatModel)
            {
                if (part != null)
                {
                    part.rotateAngleX = pitchRad;
                    part.render(poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);
                }
            }
        }

        poseStack.popPose();
    }

    private static float getAimPitch(@NotNull AAGun gun, float partialTicks)
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

    private static float getAimWorldYaw(@NotNull AAGun gun, float partialTicks, float baseYaw)
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
    private static Player getPlayerGunner(AAGun gun)
    {
        if (gun.getFirstPassenger() instanceof Player p)
            return p;
        return null;
    }
}