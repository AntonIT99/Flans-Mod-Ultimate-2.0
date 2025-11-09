package com.wolffsarmormod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.client.ModelCache;
import com.wolffsarmormod.common.entity.IFlanEntity;
import com.wolffsarmormod.common.types.InfoType;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buf, int light)
    {
        if (!(entity instanceof IFlanEntity flanEntity))
            return;

        IModelBase bulletModel = ModelCache.getOrLoadTypeModel(flanEntity.getShortName());
        if (bulletModel != null)
        {
            VertexConsumer vertexconsumer = buf.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
            bulletModel.renderToBuffer(pose, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull T entity)
    {
        if (entity instanceof IFlanEntity flanEntity)
        {
            InfoType infoType = InfoType.getInfoType(flanEntity.getShortName());
            if (infoType != null)
                return infoType.getTexture();
        }
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }
}
