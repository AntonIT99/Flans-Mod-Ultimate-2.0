package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelMG;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class DeployableGunRenderer extends FlanEntityRenderer<DeployedGun>
{
    public DeployableGunRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected AABB getBoundingBoxForCulling(DeployedGun gun)
    {
        double range = DeployedGun.getRenderDistance();
        return new AABB(gun.getX() - range, gun.getY() - range, gun.getZ() - range,
            gun.getX() + range, gun.getY() + range, gun.getZ() + range);
    }

    @Override
    public void extractRenderState(DeployedGun gun, State state, float partialTicks)
    {
        super.extractRenderState(gun, state, partialTicks);
        GunType type = gun.getConfigType();
        if (type == null)
        {
            state.customData = null;
            return;
        }
        ModelMG model = ModelCache.getOrLoadDeployableGunModel(type);
        if (model == null)
        {
            state.customData = null;
            return;
        }

        float baseYaw = Direction.from2DDataValue(gun.getGunDirection()).toYRot();
        float aimPitch = getAimPitch(gun, partialTicks);
        float aimLocalYaw = Mth.wrapDegrees(getAimWorldYaw(gun, partialTicks, baseYaw) - baseYaw);
        state.customData = new RenderData(model, type, type.getDeployableTexture(), baseYaw, aimPitch, aimLocalYaw,
            gun.getReloadTimer() <= 0 && gun.hasAmmo());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        if (!(state.customData instanceof RenderData data))
            return;

        boolean translucent = ModClientConfig.get().useTranslucentRendering(data.type);
        boolean cull = ModClientConfig.get().useCullingRendering(data.type);
        float modelScale = data.type.getModelScale();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - data.baseYaw));
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(data.model))
        {
            collector.submitCustomGeometry(poseStack, renderPass.getRenderType(data.texture, translucent, cull), (pose, vertices) -> {
                PoseStack deferred = poseStack(pose);
                data.model.renderBipod(data.showAmmo, deferred, vertices, state.lightCoords, OverlayTexture.NO_OVERLAY,
                    state.red, state.green, state.blue, state.alpha, modelScale, renderPass);
            });
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-data.aimLocalYaw));
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(data.model))
        {
            collector.submitCustomGeometry(poseStack, renderPass.getRenderType(data.texture, translucent, cull), (pose, vertices) -> {
                PoseStack deferred = poseStack(pose);
                data.model.renderGun(data.showAmmo, data.aimPitch, deferred, vertices, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, state.red, state.green, state.blue, state.alpha, modelScale, renderPass);
            });
        }
        poseStack.popPose();
        submitEntityFeatures(state, poseStack, collector, camera);
    }

    private static PoseStack poseStack(PoseStack.Pose pose)
    {
        PoseStack result = new PoseStack();
        result.last().set(pose);
        return result;
    }

    private static float getAimPitch(DeployedGun gun, float partialTicks)
    {
        Player gunner = getPlayerGunner(gun);
        float pitch = gunner != null ? Mth.lerp(partialTicks, gunner.xRotO, gunner.getXRot())
            : Mth.lerp(partialTicks, gun.xRotO, gun.getXRot());
        float top = gun.getConfigType().getTopViewLimit();
        float bottom = gun.getConfigType().getBottomViewLimit();
        if (top > bottom)
        {
            float swap = top;
            top = bottom;
            bottom = swap;
        }
        return Mth.clamp(pitch, top, bottom);
    }

    private static float getAimWorldYaw(DeployedGun gun, float partialTicks, float baseYaw)
    {
        Player gunner = getPlayerGunner(gun);
        float viewYaw = gunner != null ? Mth.rotLerp(partialTicks, gunner.yRotO, gunner.getYRot())
            : Mth.rotLerp(partialTicks, gun.yRotO, gun.getYRot());
        float localYaw = Mth.clamp(Mth.wrapDegrees(viewYaw - baseYaw),
            -gun.getConfigType().getSideViewLimit(), gun.getConfigType().getSideViewLimit());
        return baseYaw + localYaw;
    }

    private static @Nullable Player getPlayerGunner(DeployedGun gun)
    {
        return gun.getFirstPassenger() instanceof Player player ? player : null;
    }

    private record RenderData(ModelMG model, GunType type, Identifier texture, float baseYaw, float aimPitch,
                              float aimLocalYaw, boolean showAmmo) { }
}
