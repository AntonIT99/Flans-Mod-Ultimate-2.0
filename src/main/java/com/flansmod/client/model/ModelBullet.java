package com.flansmod.client.model;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelRenderer;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.translate(0F, 2F, 0F);
        super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
        poseStack.popPose();
    }
}
