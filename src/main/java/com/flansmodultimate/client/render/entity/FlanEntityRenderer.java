package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.entity.IFlanEntity;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.LegacyTransformApplier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
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

        IModelBase model = ModelCache.getOrLoadTypeModel(flanEntity.getConfigType());
        if (model != null)
            LegacyTransformApplier.renderModel(model, flanEntity.getConfigType(), getTextureLocation(entity), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
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
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }
}
