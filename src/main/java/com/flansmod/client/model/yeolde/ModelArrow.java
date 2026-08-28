package com.flansmod.client.model.yeolde;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import org.jetbrains.annotations.NotNull;

public class ModelArrow extends ModelBase
{
    public ModelRenderer bulletModel;

    public ModelArrow()
    {
        bulletModel = new ModelRenderer(this, 0, 0);
        bulletModel.addBox(-0.5F, -1F, -0.5F, 1, 2, 1);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        byte var11 = 0;
        float var12 = 0.0F;
        float var13 = 0.5F;
        float var14 = (var11 * 10) / 32.0F;
        float var15 = (5 + var11 * 10) / 32.0F;
        float var16 = 0.0F;
        float var17 = 0.15625F;
        float var18 = (5 + var11 * 10) / 32.0F;
        float var19 = (10 + var11 * 10) / 32.0F;
        float var20 = 0.05625F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(var20, var20, var20);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        addVertex(poseStack, vertexConsumer, -7.0F, -2.0F, -2.0F, var16, var18, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, -2.0F, 2.0F, var17, var18, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, 2.0F, 2.0F, var17, var19, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, 2.0F, -2.0F, var16, var19, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);

        addVertex(poseStack, vertexConsumer, -7.0F, 2.0F, -2.0F, var16, var18, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, 2.0F, 2.0F, var17, var18, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, -2.0F, 2.0F, var17, var19, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);
        addVertex(poseStack, vertexConsumer, -7.0F, -2.0F, -2.0F, var16, var19, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay, red, green, blue, alpha);

        for (int var23 = 0; var23 < 4; ++var23)
        {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            addVertex(poseStack, vertexConsumer, -8.0F, -2.0F, 0.0F, var12, var14, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay, red, green, blue, alpha);
            addVertex(poseStack, vertexConsumer, 8.0F, -2.0F, 0.0F, var13, var14, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay, red, green, blue, alpha);
            addVertex(poseStack, vertexConsumer, 8.0F, 2.0F, 0.0F, var13, var15, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay, red, green, blue, alpha);
            addVertex(poseStack, vertexConsumer, -8.0F, 2.0F, 0.0F, var12, var15, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay, red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private static void addVertex(PoseStack poseStack, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        PoseStack.Pose pose = poseStack.last();
        vertexConsumer.vertex(pose.pose(), x, y, z)
            .color(red, green, blue, alpha)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(pose.normal(), normalX, normalY, normalZ)
            .endVertex();
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
    {
    }
}
