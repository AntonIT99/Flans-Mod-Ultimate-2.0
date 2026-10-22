package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.common.entity.NukeDropEntity;
import com.flansmodultimate.client.render.CustomRenderType;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the falling legacy bomb, then its expanding, emissive blast throughout the configured visual lifetime. */
public final class NukeDropRenderer extends EntityRenderer<NukeDropEntity>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/entity/nukedrop.png");

    private final NukeDropModel model = new NukeDropModel();

    public NukeDropRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(@NotNull NukeDropEntity entity, float yaw, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int light)
    {
        if (entity.getExplodedTicks() <= 0)
        {
            model.renderBomb(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        }
        else
        {
            float age = entity.getExplodedTicks() + partialTick;
            float progress = Mth.clamp(age / ModApocalypseConfig.apocalypseNukeVisualTicks(), 0F, 1F);
            float alpha = (1F - progress * progress) * 0.5F;
            float growth = 1F - 1F / (age / 5F + 1F);
            float scale = growth * growth * 100F;
            pose.pushPose();
            pose.scale(-scale, scale, scale);
            model.renderBlast(pose, buffer.getBuffer(CustomRenderType.entityEmissiveAdditiveNoDepthWrite(TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, alpha);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NukeDropEntity entity)
    {
        return TEXTURE;
    }
}
