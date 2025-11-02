package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import org.jetbrains.annotations.NotNull;

public class ModelBullet extends ModelBase
{
    protected ModelRenderer bulletModel;

    public ModelBullet()
    {
        super();

        bulletModel = new ModelRenderer(this, 0, 0);
        bulletModel.addBox(-0.5F, -1.5F, -0.5F, 1, 3, 1);

        setScale(0.5F);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack pPoseStack, @NotNull VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha)
    {
        pPoseStack.pushPose();
        bulletModel.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, getScale());
        pPoseStack.popPose();
    }
}
