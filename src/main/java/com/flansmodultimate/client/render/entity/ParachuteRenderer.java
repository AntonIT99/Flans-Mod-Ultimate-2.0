package com.flansmodultimate.client.render.entity;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.entity.Parachute;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ParachuteRenderer extends EntityRenderer<Parachute>
{
    public ParachuteRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);
        shadowRadius = 2.0F;
    }

    @Override
    public void render(@NotNull Parachute parachute, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight)
    {
        ToolType type = parachute.getConfigType();
        if (type == null)
            return;

        IModelBase model = ModelCache.getOrLoadTypeModel(type);
        if (model == null)
            return;

        int color = type.getColour();
        float red = (color >> 16 & 255) / 255F;
        float green = (color >> 8 & 255) / 255F;
        float blue = (color & 255) / 255F;
        float modelScale = type.getModelScale();
        ResourceLocation texture = getTextureLocation(parachute);
        boolean translucent = ModClientConfig.get().useTranslucentRendering(type);

        if (model instanceof ModelBase modelBase)
            modelBase.setScale(modelScale);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, parachute.yRotO, parachute.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, parachute.xRotO, parachute.getXRot())));

        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
        {
            for (ModelRenderer part : model.getBoxList())
            {
                if (part instanceof ModelRendererTurbo turboPart)
                    turboPart.render(poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale, renderPass);
                else if (renderPass == EnumRenderPass.DEFAULT)
                    part.render(poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent)), packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1F, modelScale);
            }
        }

        poseStack.popPose();
        super.render(parachute, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull Parachute entity)
    {
        ToolType type = entity.getConfigType();
        return type == null ? ResourceLocation.parse("") : type.getTexture();
    }
}
