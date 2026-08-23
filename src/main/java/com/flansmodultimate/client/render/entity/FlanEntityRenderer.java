package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.api.client.model.IModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class FlanEntityRenderer<T extends Entity> extends EntityRenderer<T, FlanEntityRenderer.State>
{
    public FlanEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick)
    {
        super.extractRenderState(entity, state, partialTick);
        state.type = entity instanceof IFlanEntity<?> flanEntity ? flanEntity.getConfigType() : null;
        state.model = state.type == null ? null : ModelCache.getOrLoadTypeModel(state.type);
        state.texture = getTextureLocation(entity);
        if (state.type != null)
        {
            state.red = getRed(state.type);
            state.green = getGreen(state.type);
            state.blue = getBlue(state.type);
            state.alpha = getAlpha(state.type);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        submitFlanState(state, poseStack, collector);
        submitEntityFeatures(state, poseStack, collector, camera);
    }

    protected final void submitEntityFeatures(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        super.submit(state, poseStack, collector, camera);
    }

    protected void submitFlanState(State state, PoseStack poseStack, SubmitNodeCollector collector)
    {
        if (state.type == null || state.model == null)
            return;

        if (state.model instanceof ModelBullet modelBullet)
        {
            submitDirectModel(modelBullet, state, poseStack, collector);
            return;
        }
        if (state.model instanceof ModelBomb modelBomb)
        {
            submitDirectModel(modelBomb, state, poseStack, collector);
            return;
        }

        LegacyTransformApplier.submitModel(state.model, state.type, state.texture, poseStack, collector,
            state.lightCoords, OverlayTexture.NO_OVERLAY, state.red, state.green, state.blue, state.alpha);
    }

    private static void submitDirectModel(IModelBase model, State state, PoseStack poseStack, SubmitNodeCollector collector)
    {
        RenderType renderType = EnumRenderPass.DEFAULT.getRenderType(state.texture,
            ModClientConfig.get().useTranslucentRendering(state.type), ModClientConfig.get().useCullingRendering(state.type));
        collector.submitCustomGeometry(poseStack, renderType, (submittedPose, vertexConsumer) -> {
            PoseStack deferredPoseStack = new PoseStack();
            deferredPoseStack.last().set(submittedPose);
            model.renderToBuffer(deferredPoseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY,
                state.red, state.green, state.blue, state.alpha);
        });
    }

    protected float getRed(InfoType type) { return (type.getColour() >> 16 & 255) / 255F; }
    protected float getGreen(InfoType type) { return (type.getColour() >> 8 & 255) / 255F; }
    protected float getBlue(InfoType type) { return (type.getColour() & 255) / 255F; }
    protected float getAlpha(InfoType type) { return 1F; }

    protected Identifier getTextureLocation(T entity)
    {
        if (entity instanceof IFlanEntity<?> flanEntity)
        {
            InfoType infoType = InfoType.getInfoType(flanEntity.getShortName());
            if (infoType != null)
                return infoType.getTexture();
        }
        return FlansMod.defaultFallbackTexture;
    }

    public static class State extends EntityRenderState
    {
        protected @Nullable InfoType type;
        protected @Nullable IModelBase model;
        protected Identifier texture = FlansMod.defaultFallbackTexture;
        protected float red = 1F;
        protected float green = 1F;
        protected float blue = 1F;
        protected float alpha = 1F;
        protected float yaw;
        protected float pitch;
        protected float roll;
        protected @Nullable Object customData;
    }
}
