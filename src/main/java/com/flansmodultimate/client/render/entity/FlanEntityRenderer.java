package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.LegacyTransformApplier;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
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
        if (model != null)
            renderFlanModel(model, type, getTextureLocation(entity), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, getRed(type), getGreen(type), getBlue(type), getAlpha(type));
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
