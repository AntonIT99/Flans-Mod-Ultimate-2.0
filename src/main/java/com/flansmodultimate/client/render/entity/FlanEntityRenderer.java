package com.flansmodultimate.client.render.entity;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class FlanEntityRenderer<T extends Entity> extends EntityRenderer<T>
{
    public FlanEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        if (!(entity instanceof IFlanEntity<?> flanEntity))
            return;

        renderFlanEntity(entity, flanEntity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    protected void renderFlanEntity(@NotNull T entity, @NotNull IFlanEntity<?> flanEntity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        InfoType type = flanEntity.getConfigType();
        if (type == null)
            return;

        IModelBase model = ModelCache.getOrLoadTypeModel(type);
        if (model == null)
            return;

        ResourceLocation texture = getTextureLocation(entity);
        float red = getRed(type);
        float green = getGreen(type);
        float blue = getBlue(type);
        float alpha = getAlpha(type);

        if (model instanceof ModelBullet modelBullet)
        {
            VertexConsumer vertexConsumer = buffer.getBuffer(EnumRenderPass.DEFAULT.getRenderType(texture, ModClientConfig.get().useTranslucentRendering(type), false));
            modelBullet.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            return;
        }
        if (model instanceof ModelBomb modelBomb)
        {
            VertexConsumer vertexConsumer = buffer.getBuffer(EnumRenderPass.DEFAULT.getRenderType(texture, ModClientConfig.get().useTranslucentRendering(type), false));
            modelBomb.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            return;
        }

        renderFlanModel(model, type, texture, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }

    protected void renderFlanModel(@NotNull IModelBase model, @NotNull InfoType type, @NotNull ResourceLocation texture, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        LegacyTransformApplier.renderModel(model, type, texture, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    protected float getRed(@NotNull InfoType type)
    {
        return (type.getColour() >> 16 & 255) / 255F;
    }

    protected float getGreen(@NotNull InfoType type)
    {
        return (type.getColour() >> 8 & 255) / 255F;
    }

    protected float getBlue(@NotNull InfoType type)
    {
        return (type.getColour() & 255) / 255F;
    }

    protected float getAlpha(@NotNull InfoType type)
    {
        return 1F;
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        if (entity instanceof IFlanEntity<?> flanEntity)
        {
            InfoType infoType = InfoType.getInfoType(flanEntity.getShortName());
            if (infoType != null)
                return infoType.getTexture();
        }
        return ResourceLocation.parse("");
    }
}
