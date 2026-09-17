
package com.flansmod.client.model.ww2;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelM8Smoke extends ModelBase {
   int textureX = 128;
   int textureY = 128;
   public ModelRendererTurbo[] m8smokeModel = new ModelRendererTurbo[18];

   public ModelM8Smoke() {
      this.m8smokeModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.m8smokeModel[1] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.m8smokeModel[2] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.m8smokeModel[3] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.m8smokeModel[4] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.m8smokeModel[5] = new ModelRendererTurbo(this, 97, 1, this.textureX, this.textureY);
      this.m8smokeModel[6] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.m8smokeModel[7] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.m8smokeModel[8] = new ModelRendererTurbo(this, 33, 9, this.textureX, this.textureY);
      this.m8smokeModel[9] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.m8smokeModel[10] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.m8smokeModel[11] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.m8smokeModel[12] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.m8smokeModel[13] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.m8smokeModel[14] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.m8smokeModel[15] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.m8smokeModel[16] = new ModelRendererTurbo(this, 81, 9, this.textureX, this.textureY);
      this.m8smokeModel[17] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.m8smokeModel[0].addShapeBox(-1.5F, -14.0F, -2.0F, 4, 4, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
      this.m8smokeModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[0].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[0].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[1].addShapeBox(-2.5F, -14.0F, -2.0F, 1, 4, 4, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);
      this.m8smokeModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[1].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[1].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[2].addShapeBox(2.5F, -14.0F, -1.5F, 3, 4, 3, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -0.5F, -1.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.2F, 0.0F);
      this.m8smokeModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[2].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[2].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[3].addShapeBox(-1.5F, -14.5F, -2.0F, 4, 1, 4, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.2F, 0.0F, -0.5F, 0.2F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, 0.2F);
      this.m8smokeModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[3].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[3].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[4].addShapeBox(-3.5F, -14.5F, -2.0F, 2, 1, 4, 0.0F, -0.8F, 0.0F, -1.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, -0.8F, 0.0F, -1.0F, 0.2F, -0.5F, -1.0F, 0.0F, -0.5F, 0.2F, 0.0F, -0.5F, 0.2F, 0.2F, -0.5F, -1.0F);
      this.m8smokeModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[4].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[4].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[5].addShapeBox(4.0F, -14.0F, -1.0F, 1, 4, 2, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -2.3F, 0.0F, 0.0F, 1.7F, 0.0F, 0.0F, 1.7F, 0.0F, 0.0F, -2.3F, 0.0F, 0.0F);
      this.m8smokeModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[5].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[5].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[6].addShapeBox(6.0F, -10.0F, -1.0F, 1, 5, 2, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -2.3F, 0.0F, 0.0F, 1.7F, 0.0F, 0.0F, 1.7F, 0.0F, 0.0F, -2.3F, 0.0F, 0.0F);
      this.m8smokeModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[6].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[6].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[7].addShapeBox(8.0F, -5.0F, -1.0F, 1, 5, 2, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -1.3F, 0.0F, 0.0F, 0.7F, 0.0F, 0.0F, 0.7F, 0.0F, 0.0F, -1.3F, 0.0F, 0.0F);
      this.m8smokeModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[7].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[7].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[8].addShapeBox(2.5F, -14.5F, -2.0F, 2, 1, 4, 0.0F, 0.0F, 0.0F, -0.3F, 0.2F, 0.0F, -1.0F, 0.2F, 0.0F, -1.0F, 0.0F, 0.0F, -0.3F, 0.0F, -0.5F, -0.3F, 0.2F, -0.5F, -0.3F, 0.2F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F);
      this.m8smokeModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[8].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[8].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[9].addShapeBox(-3.7F, -14.0F, -1.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.m8smokeModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[9].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[9].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[10].addShapeBox(4.5F, -13.0F, -3.5F, 1, 1, 5, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F);
      this.m8smokeModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[10].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[10].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[11].addShapeBox(5.0F, -13.5F, -3.5F, 4, 1, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F);
      this.m8smokeModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[11].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[11].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[12].addShapeBox(4.0F, -12.5F, -3.5F, 1, 4, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.m8smokeModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[12].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[12].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[13].addShapeBox(9.0F, -12.5F, -3.5F, 1, 4, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.m8smokeModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[13].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[13].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[14].addShapeBox(5.0F, -8.5F, -3.5F, 4, 1, 1, 0.0F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.m8smokeModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[14].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[14].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[15].addShapeBox(-4.5F, -10.0F, -5.0F, 3, 22, 10, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      this.m8smokeModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[15].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[15].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[16].addShapeBox(2.5F, -10.0F, -5.0F, 3, 22, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      this.m8smokeModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[16].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[16].rotateAngleY = (float) -Math.PI;
      this.m8smokeModel[17].addShapeBox(-1.5F, -10.0F, -5.0F, 4, 22, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.m8smokeModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.m8smokeModel[17].rotateAngleX = (float) Math.PI;
      this.m8smokeModel[17].rotateAngleY = (float) -Math.PI;
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
