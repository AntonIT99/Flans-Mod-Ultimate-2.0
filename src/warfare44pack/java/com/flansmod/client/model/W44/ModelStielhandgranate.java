
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelStielhandgranate extends ModelBase {
   int textureX = 128;
   int textureY = 128;
   public ModelRendererTurbo[] stielhandgranateModel = new ModelRendererTurbo[20];

   public ModelStielhandgranate() {
      stielhandgranateModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      stielhandgranateModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      stielhandgranateModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY);
      stielhandgranateModel[3] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      stielhandgranateModel[4] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      stielhandgranateModel[5] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      stielhandgranateModel[6] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      stielhandgranateModel[7] = new ModelRendererTurbo(this, 25, 17, textureX, textureY);
      stielhandgranateModel[8] = new ModelRendererTurbo(this, 49, 17, textureX, textureY);
      stielhandgranateModel[9] = new ModelRendererTurbo(this, 73, 17, textureX, textureY);
      stielhandgranateModel[10] = new ModelRendererTurbo(this, 97, 17, textureX, textureY);
      stielhandgranateModel[11] = new ModelRendererTurbo(this, 65, 33, textureX, textureY);
      stielhandgranateModel[12] = new ModelRendererTurbo(this, 81, 33, textureX, textureY);
      stielhandgranateModel[13] = new ModelRendererTurbo(this, 105, 33, textureX, textureY);
      stielhandgranateModel[14] = new ModelRendererTurbo(this, 1, 41, textureX, textureY);
      stielhandgranateModel[15] = new ModelRendererTurbo(this, 25, 41, textureX, textureY);
      stielhandgranateModel[16] = new ModelRendererTurbo(this, 49, 41, textureX, textureY);
      stielhandgranateModel[17] = new ModelRendererTurbo(this, 81, 41, textureX, textureY);
      stielhandgranateModel[18] = new ModelRendererTurbo(this, 105, 41, textureX, textureY);
      stielhandgranateModel[19] = new ModelRendererTurbo(this, 1, 49, textureX, textureY);
      stielhandgranateModel[0].addShapeBox(-2.0F, 0.0F, -2.5F, 5, 2, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      stielhandgranateModel[0].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[0].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[1].addShapeBox(-2.0F, -4.0F, -2.5F, 5, 4, 5, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      stielhandgranateModel[1].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[1].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[2].addShapeBox(-2.0F, -21.0F, -2.5F, 5, 8, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      stielhandgranateModel[2].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[2].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[3].addShapeBox(-1.5F, -13.0F, -2.0F, 4, 9, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      stielhandgranateModel[3].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[3].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[4].addShapeBox(-2.0F, -25.0F, -2.5F, 5, 4, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      stielhandgranateModel[4].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[4].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[5].addShapeBox(-2.0F, -27.0F, -2.5F, 5, 2, 5, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F);
      stielhandgranateModel[5].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[5].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[6].addShapeBox(-1.5F, -39.0F, -3.5F, 4, 11, 7, 0.0F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F, 0.5F, 0.0F, 1.5F);
      stielhandgranateModel[6].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[6].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[7].addShapeBox(4.0F, -39.0F, -3.5F, 1, 11, 7, 0.0F, 1.0F, 0.0F, 1.5F, 0.5F, 0.0F, -1.5F, 0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 1.5F, 1.0F, 0.0F, 1.5F, 0.5F, 0.0F, -1.5F, 0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 1.5F);
      stielhandgranateModel[7].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[7].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[8].addShapeBox(-4.0F, -39.0F, -3.5F, 1, 11, 7, 0.0F, 0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 1.5F, 1.0F, 0.0F, 1.5F, 0.5F, 0.0F, -1.5F, 0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 1.5F, 1.0F, 0.0F, 1.5F, 0.5F, 0.0F, -1.5F);
      stielhandgranateModel[8].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[8].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[9].addShapeBox(-1.5F, -28.0F, -3.5F, 4, 1, 7, 0.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F, 0.5F, 0.0F, 2.0F);
      stielhandgranateModel[9].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[9].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[10].addShapeBox(4.0F, -28.0F, -3.5F, 1, 1, 7, 0.0F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F);
      stielhandgranateModel[10].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[10].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[11].addShapeBox(-4.0F, -28.0F, -3.5F, 1, 1, 7, 0.0F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, 2.0F, 1.0F, 0.0F, -1.5F);
      stielhandgranateModel[11].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[11].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[12].addShapeBox(-2.0F, 2.8F, -2.5F, 5, 1, 5, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F);
      stielhandgranateModel[12].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[12].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[13].addShapeBox(-2.0F, 2.2F, -2.5F, 5, 1, 5, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F);
      stielhandgranateModel[13].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[13].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[14].addShapeBox(-2.0F, 2.0F, -2.5F, 5, 2, 5, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      stielhandgranateModel[14].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[14].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[15].addShapeBox(-2.0F, -24.8F, -2.5F, 5, 1, 5, 0.0F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F);
      stielhandgranateModel[15].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[15].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[16].addShapeBox(-2.0F, -23.8F, -2.5F, 5, 1, 5, 0.0F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F, 0.25F, -0.3F, 0.25F);
      stielhandgranateModel[16].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[16].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[17].addShapeBox(-1.5F, -39.5F, -3.5F, 4, 1, 7, 0.0F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F);
      stielhandgranateModel[17].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[17].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[18].addShapeBox(-4.0F, -39.5F, -3.5F, 1, 1, 7, 0.0F, 0.0F, 0.0F, -2.0F, 1.0F, 0.0F, 0.5F, 1.0F, 0.0F, 0.5F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 1.0F, 0.0F, 0.5F, 1.0F, 0.0F, 0.5F, 0.0F, 0.0F, -2.0F);
      stielhandgranateModel[18].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[18].rotateAngleZ = (float) -Math.PI;
      stielhandgranateModel[19].addShapeBox(4.0F, -39.5F, -3.5F, 1, 1, 7, 0.0F, 1.0F, 0.0F, 0.5F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 1.0F, 0.0F, 0.5F, 1.0F, 0.0F, 0.5F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 1.0F, 0.0F, 0.5F);
      stielhandgranateModel[19].setRotationPoint(0.0F, -20.0F, 0.0F);
      stielhandgranateModel[19].rotateAngleZ = (float) -Math.PI;
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
