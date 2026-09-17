
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelRenderer;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelWH40K_Misc_LaserSmall_1 extends ModelBase {
   public ModelRenderer laserSmallModel = new ModelRenderer(this, 0, 0);

   public ModelWH40K_Misc_LaserSmall_1() {
      laserSmallModel.addBox(-1.0F, -8.0F, -1.0F, 2, 16, 2);
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
