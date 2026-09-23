
package com.flansmod.client.model.manus_sifi;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelRenderer;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelLaser extends ModelBase {
   public ModelRenderer laserModel = new ModelRenderer(this, 0, 0);

   public ModelLaser() {
      laserModel.addBox(-1.5F, -16.0F, -1.5F, 3, 32, 3);
   }

   @Override
   public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
   {
       poseStack.pushPose();
       poseStack.scale(0.5F, 0.5F, 0.5F);
       super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
       poseStack.popPose();
   }
}
