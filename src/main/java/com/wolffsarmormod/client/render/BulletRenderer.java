package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.entity.Bullet;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BulletRenderer extends EntityRenderer<Bullet>
{
    public BulletRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    @Override
    public void render(@NotNull Bullet bullet, float yaw, float pt, @NotNull PoseStack pose, @NotNull MultiBufferSource buf, int light)
    {
        IModelBase bulletModel = bullet.getFiredShot().getBulletType().getModel();
        if (bulletModel != null)
        {
            VertexConsumer vertexconsumer = buf.getBuffer(RenderType.entityTranslucent(getTextureLocation(bullet)));
            bulletModel.renderToBuffer(pose, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull Bullet bullet)
    {
        return bullet.getFiredShot().getBulletType().getTexture();
    }
}
