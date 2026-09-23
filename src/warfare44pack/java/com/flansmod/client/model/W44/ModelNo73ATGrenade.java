
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelNo73ATGrenade extends ModelBase {
   int textureX = 128;
   int textureY = 128;
   public ModelRendererTurbo[] no73atgrenadeModel = new ModelRendererTurbo[6];

   public ModelNo73ATGrenade() {
      no73atgrenadeModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      no73atgrenadeModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      no73atgrenadeModel[2] = new ModelRendererTurbo(this, 49, 9, textureX, textureY);
      no73atgrenadeModel[3] = new ModelRendererTurbo(this, 81, 9, textureX, textureY);
      no73atgrenadeModel[4] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      no73atgrenadeModel[5] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      no73atgrenadeModel[0].addShapeBox(-5.5F, 1.0F, -2.5F, 4, 4, 5, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F);
      no73atgrenadeModel[0].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[0].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[0].rotateAngleY = (float) -Math.PI;
      no73atgrenadeModel[1].addShapeBox(-6.0F, 1.0F, -2.5F, 1, 4, 5, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);
      no73atgrenadeModel[1].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[1].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[1].rotateAngleY = (float) -Math.PI;
      no73atgrenadeModel[2].addShapeBox(-8.5F, 4.0F, -5.0F, 3, 30, 10, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      no73atgrenadeModel[2].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[2].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[2].rotateAngleY = (float) -Math.PI;
      no73atgrenadeModel[3].addShapeBox(-1.5F, 4.0F, -5.0F, 3, 30, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      no73atgrenadeModel[3].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[3].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[3].rotateAngleY = (float) -Math.PI;
      no73atgrenadeModel[4].addShapeBox(-5.5F, 4.0F, -5.0F, 4, 30, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      no73atgrenadeModel[4].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[4].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[4].rotateAngleY = (float) -Math.PI;
      no73atgrenadeModel[5].addShapeBox(-2.0F, 1.0F, -2.5F, 1, 4, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      no73atgrenadeModel[5].setRotationPoint(0.0F, 10.0F, 0.0F);
      no73atgrenadeModel[5].rotateAngleX = (float) Math.PI;
      no73atgrenadeModel[5].rotateAngleY = (float) -Math.PI;
   }

   @Override
   public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
   {
       poseStack.pushPose();
       poseStack.scale(0.2F, 0.2F, 0.2F);
       super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
       poseStack.popPose();
   }
}
