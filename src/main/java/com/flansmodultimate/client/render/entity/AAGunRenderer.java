package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelAAGun;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class AAGunRenderer extends FlanEntityRenderer<AAGun>
{
    public AAGunRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    protected AABB getBoundingBoxForCulling(AAGun aaGun)
    {
        double range = AAGun.getRenderDistance();
        return new AABB(aaGun.getX() - range, aaGun.getY() - range, aaGun.getZ() - range,
            aaGun.getX() + range, aaGun.getY() + range, aaGun.getZ() + range);
    }

    @Override
    public void extractRenderState(AAGun aaGun, State state, float partialTicks)
    {
        super.extractRenderState(aaGun, state, partialTicks);
        AAGunType type = aaGun.getConfigType();
        if (type == null || !(ModelCache.getOrLoadTypeModel(type) instanceof ModelAAGun model))
        {
            state.customData = null;
            return;
        }

        int barrelCount = Math.max(0, type.getNumBarrels());
        boolean[] hasAmmo = new boolean[barrelCount];
        for (int i = 0; i < barrelCount; i++)
            hasAmmo[i] = aaGun.hasAmmo(i);
        state.customData = new RenderData(model, type, Mth.rotLerp(partialTicks, aaGun.getPrevGunYaw(), aaGun.getGunYaw()),
            aaGun.getGunPitch(), aaGun.getBarrelRecoil().clone(), hasAmmo);
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
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(data.model))
        {
            collector.submitCustomGeometry(poseStack, renderPass.getRenderType(state.texture, translucent, cull), (pose, vertices) -> {
                PoseStack deferred = poseStack(pose);
                data.model.renderBase(deferred, vertices, state.lightCoords, OverlayTexture.NO_OVERLAY,
                    state.red, state.green, state.blue, state.alpha, modelScale, renderPass);
            });
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(270F - data.gunYaw));
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(data.model))
        {
            collector.submitCustomGeometry(poseStack, renderPass.getRenderType(state.texture, translucent, cull), (pose, vertices) -> {
                PoseStack deferred = poseStack(pose);
                data.model.renderGun(data.gunPitch, data.barrelRecoil, data.hasAmmo, deferred, vertices,
                    state.lightCoords, OverlayTexture.NO_OVERLAY, state.red, state.green, state.blue, state.alpha,
                    modelScale, renderPass);
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

    private record RenderData(ModelAAGun model, AAGunType type, float gunYaw, float gunPitch,
                              float[] barrelRecoil, boolean[] hasAmmo) { }
}
