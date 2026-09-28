package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelFlash;
import com.flansmod.client.model.ModelMG;
import com.flansmod.client.model.ModelMuzzleFlash;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.debug.DebugHelper;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.DeployedGunMuzzleGeometry;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public class DeployableGunRenderer extends FlanEntityRenderer<DeployedGun>
{
    private final Map<DeployedGun, Integer> diagnosticMarkerTicks = new WeakHashMap<>();

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

        GunType type = deployedGun.getConfigType();
        float red = getRed(type);
        float green = getGreen(type);
        float blue = getBlue(type);
        float modelScale = deployedGun.getConfigType().getModelScale();
        ResourceLocation texture = deployedGun.getConfigType().getDeployableTexture();
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);
        boolean cull = ModClientConfig.get().useCullingRendering(type);

        poseStack.pushPose();

        float baseYaw = Direction.from2DDataValue(deployedGun.getGunDirection()).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - baseYaw));

        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            model.renderBipod(deployedGun, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        float aimPitch = getAimPitch(deployedGun, partialTicks);
        float aimWorldYaw = getAimWorldYaw(deployedGun, partialTicks);
        float aimLocalYaw = Mth.wrapDegrees(aimWorldYaw - baseYaw);
        
        poseStack.mulPose(Axis.YP.rotationDegrees(-aimLocalYaw));

        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            model.renderGun(deployedGun, aimPitch, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);

        renderMuzzleFlash(deployedGun, model, aimPitch, poseStack, buffer, modelScale);

        poseStack.popPose();

        if (ModClient.isDebug() && !Integer.valueOf(deployedGun.tickCount).equals(diagnosticMarkerTicks.get(deployedGun)))
        {
            diagnosticMarkerTicks.put(deployedGun, deployedGun.tickCount);
            ModelMG.MuzzleOriginData muzzle = model.getModelMuzzleOriginData();
            if (muzzle != null)
            {
                Vec3 position = deployedGun.position().add(DeployedGunMuzzleGeometry.modelMuzzleOffset(
                    muzzle.pivot(), muzzle.muzzle(), modelScale, aimWorldYaw, aimPitch));
                DebugHelper.spawnDebugDot(position, 2, 1F, 1F, 1F);
            }
        }
    }

    private static void renderMuzzleFlash(@NotNull DeployedGun gun, @NotNull ModelMG model, float aimPitch,
                                          @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                                          float modelScale)
    {
        if (gun.getMuzzleFlashTicks() <= 0)
            return;

        Vec3 muzzle = model.getModelMuzzle(aimPitch);
        if (muzzle == null)
            return;

        Float configuredFlashScale = gun.getConfigType().getAnimationConfig().getFlashScale();
        float flashScale = configuredFlashScale == null ? 1F : configuredFlashScale;

        poseStack.pushPose();
        poseStack.translate(muzzle.x * modelScale / 16D, muzzle.y * modelScale / 16D, muzzle.z * modelScale / 16D);
        // ModelMG aims around local X and points down local Z. Flash models use
        // the hand-held gun convention and point down local X.
        poseStack.mulPose(Axis.XP.rotationDegrees(-aimPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        ModelFlash flash = ModelCache.getOrLoadFlashModel(gun.getConfigType());
        if (flash != null)
        {
            flash.renderFlash(gun.getMuzzleFlashFrame(), poseStack,
                buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(gun.getConfigType().getFlashTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F,
                modelScale * flashScale);
        }
        else if (StringUtils.isBlank(gun.getConfigType().getFlashModelClassName())
            && StringUtils.isNotBlank(gun.getConfigType().getMuzzleFlashModelClassName()))
        {
            ModelMuzzleFlash muzzleFlash = ModelCache.getOrLoadMuzzleFlashModel(gun.getConfigType());
            if (muzzleFlash != null)
                muzzleFlash.renderToBuffer(poseStack,
                    buffer.getBuffer(CustomRenderType.entityEmissiveAlpha(muzzleFlash.getTexture())),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        }
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

    private static float getAimWorldYaw(@NotNull DeployedGun gun, float partialTicks)
    {
        Player gunner = getPlayerGunner(gun);
        if (gunner == null)
            return Mth.rotLerp(partialTicks, gun.yRotO, gun.getYRot());

        float baseYaw = Direction.from2DDataValue(gun.getGunDirection()).toYRot();
        float riderYaw = Mth.rotLerp(partialTicks, gunner.yRotO, gunner.getYRot());
        float side = Math.abs(gun.getConfigType().getSideViewLimit());
        return baseYaw + Mth.clamp(Mth.wrapDegrees(riderYaw - baseYaw), -side, side);
    }

    @Nullable
    private static Player getPlayerGunner(DeployedGun gun)
    {
        if (gun.getFirstPassenger() instanceof Player p)
            return p;
        return null;
    }
}
