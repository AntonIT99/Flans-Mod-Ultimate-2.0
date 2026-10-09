package com.flansmodultimate.client.render.entity;

import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BulletRenderer extends FlanEntityRenderer<Bullet>
{
    public BulletRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
    }

    /** Small, fast projectiles add nothing visible to a shader pack's shadow map. */
    @Override
    public boolean shouldRender(@NotNull Bullet bullet, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ)
    {
        return outsideShadowPass() && super.shouldRender(bullet, frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public void render(@NotNull Bullet bullet, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        float pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

        super.render(bullet, entityYaw, partialTicks, poseStack, buffer, bullet.getConfigType().isHasLight() ? LightTexture.FULL_BRIGHT : packedLight);

        poseStack.popPose();
    }

    /** Alternate rounds of a mixed belt use the ammunition's {@code AlternateModel} when it names one. */
    @Override
    @Nullable
    protected IModelBase getModel(@NotNull Bullet bullet, @NotNull InfoType type)
    {
        if (bullet.isAlternateRound())
        {
            IModelBase alternate = ModelCache.getOrLoadAlternateBulletModel(bullet.getConfigType());
            if (alternate != null)
                return alternate;
        }
        return super.getModel(bullet, type);
    }

    /** Alternate rounds of a mixed belt use the ammunition's {@code AlternateTexture} when it names one. */
    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull Bullet bullet)
    {
        if (bullet.isAlternateRound() && bullet.getConfigType() != null && bullet.getConfigType().getAlternateTexture() != null)
            return bullet.getConfigType().getAlternateTexture();
        return super.getTextureLocation(bullet);
    }
}
