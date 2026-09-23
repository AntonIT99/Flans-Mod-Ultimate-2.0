
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelSMine extends ModelBase {
   int textureX = 64;
   int textureY = 64;
   public ModelRendererTurbo[] smineModel = new ModelRendererTurbo[24];

   public ModelSMine() {
      smineModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      smineModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      smineModel[2] = new ModelRendererTurbo(this, 1, 25, textureX, textureY);
      smineModel[3] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      smineModel[4] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      smineModel[5] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      smineModel[6] = new ModelRendererTurbo(this, 57, 1, textureX, textureY);
      smineModel[7] = new ModelRendererTurbo(this, 1, 25, textureX, textureY);
      smineModel[8] = new ModelRendererTurbo(this, 25, 25, textureX, textureY);
      smineModel[9] = new ModelRendererTurbo(this, 33, 25, textureX, textureY);
      smineModel[10] = new ModelRendererTurbo(this, 41, 25, textureX, textureY);
      smineModel[11] = new ModelRendererTurbo(this, 49, 25, textureX, textureY);
      smineModel[12] = new ModelRendererTurbo(this, 57, 25, textureX, textureY);
      smineModel[13] = new ModelRendererTurbo(this, 33, 33, textureX, textureY);
      smineModel[14] = new ModelRendererTurbo(this, 41, 33, textureX, textureY);
      smineModel[15] = new ModelRendererTurbo(this, 49, 33, textureX, textureY);
      smineModel[16] = new ModelRendererTurbo(this, 57, 33, textureX, textureY);
      smineModel[17] = new ModelRendererTurbo(this, 33, 41, textureX, textureY);
      smineModel[18] = new ModelRendererTurbo(this, 41, 41, textureX, textureY);
      smineModel[19] = new ModelRendererTurbo(this, 49, 41, textureX, textureY);
      smineModel[20] = new ModelRendererTurbo(this, 1, 49, textureX, textureY);
      smineModel[21] = new ModelRendererTurbo(this, 38, 48, textureX, textureY);
      smineModel[22] = new ModelRendererTurbo(this, 21, 44, textureX, textureY);
      smineModel[23] = new ModelRendererTurbo(this, 57, 41, textureX, textureY);
      smineModel[0].addShapeBox(-5.0F, -1.8F, -5.0F, 3, 13, 10, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      smineModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[0].rotateAngleZ = (float) Math.PI;
      smineModel[1].addShapeBox(2.0F, -1.8F, -5.0F, 3, 13, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      smineModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[1].rotateAngleZ = (float) Math.PI;
      smineModel[2].addShapeBox(-2.0F, -1.8F, -5.0F, 4, 13, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[2].rotateAngleZ = (float) Math.PI;
      smineModel[3].addShapeBox(-3.8F, -2.25F, -1.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[3].rotateAngleZ = (float) Math.PI;
      smineModel[4].addShapeBox(-4.8F, -2.25F, -1.0F, 1, 1, 2, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F);
      smineModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[4].rotateAngleZ = (float) Math.PI;
      smineModel[5].addShapeBox(-2.8F, -2.25F, -1.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
      smineModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[5].rotateAngleZ = (float) Math.PI;
      smineModel[6].addShapeBox(2.5F, -2.25F, -3.65F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
      smineModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[6].rotateAngleZ = (float) Math.PI;
      smineModel[7].addShapeBox(1.5F, -2.25F, -3.65F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[7].rotateAngleZ = (float) Math.PI;
      smineModel[8].addShapeBox(0.5F, -2.25F, -3.65F, 1, 1, 2, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F);
      smineModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[8].rotateAngleZ = (float) Math.PI;
      smineModel[9].addShapeBox(2.5F, -2.25F, 1.65F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
      smineModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[9].rotateAngleZ = (float) Math.PI;
      smineModel[10].addShapeBox(1.5F, -2.25F, 1.65F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[10].rotateAngleZ = (float) Math.PI;
      smineModel[11].addShapeBox(0.5F, -2.25F, 1.65F, 1, 1, 2, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F);
      smineModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[11].rotateAngleZ = (float) Math.PI;
      smineModel[12].addShapeBox(4.0F, -2.25F, -1.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F);
      smineModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[12].rotateAngleZ = (float) Math.PI;
      smineModel[13].addShapeBox(3.0F, -2.25F, -1.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[13].rotateAngleZ = (float) Math.PI;
      smineModel[14].addShapeBox(2.0F, -2.25F, -1.0F, 1, 1, 2, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F);
      smineModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[14].rotateAngleZ = (float) Math.PI;
      smineModel[15].addShapeBox(-0.5F, -8.0F, -0.5F, 1, 6, 1, 0.0F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F);
      smineModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[15].rotateAngleZ = (float) Math.PI;
      smineModel[16].addShapeBox(-0.5F, -9.5F, -0.5F, 1, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[16].rotateAngleZ = (float) Math.PI;
      smineModel[17].addShapeBox(-0.5F, -10.5F, -0.5F, 1, 1, 1, 0.0F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[17].rotateAngleZ = (float) Math.PI;
      smineModel[18].addShapeBox(-0.5F, -12.5F, 0.5F, 1, 3, 1, 0.0F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, 0.7F, -0.3F, 0.0F, 0.7F, -0.3F, 0.0F, -1.3F, -0.3F, 0.0F, -1.3F);
      smineModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[18].rotateAngleZ = (float) Math.PI;
      smineModel[19].addShapeBox(-0.5F, -12.5F, -1.5F, 1, 3, 1, 0.0F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -1.3F, -0.3F, 0.0F, -1.3F, -0.3F, 0.0F, 0.7F, -0.3F, 0.0F, 0.7F);
      smineModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[19].rotateAngleZ = (float) Math.PI;
      smineModel[20].addShapeBox(-2.0F, -2.0F, -4.5F, 4, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      smineModel[20].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[20].rotateAngleZ = (float) Math.PI;
      smineModel[21].addShapeBox(-5.0F, -2.0F, -4.5F, 3, 1, 9, 0.0F, -0.35F, 0.0F, -2.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.35F, 0.0F, -2.75F, -0.35F, 0.0F, -2.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.35F, 0.0F, -2.75F);
      smineModel[21].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[21].rotateAngleZ = (float) Math.PI;
      smineModel[22].addShapeBox(2.0F, -2.0F, -4.5F, 3, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, -0.35F, 0.0F, -2.75F, -0.35F, 0.0F, -2.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.35F, 0.0F, -2.75F, -0.35F, 0.0F, -2.75F, 0.0F, 0.0F, 0.0F);
      smineModel[22].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[22].rotateAngleZ = (float) Math.PI;
      smineModel[23].addShapeBox(-0.5F, -8.75F, -1.0F, 1, 1, 2, 0.0F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F, -0.3F, -0.3F, 0.25F);
      smineModel[23].setRotationPoint(0.0F, 0.0F, 0.0F);
      smineModel[23].rotateAngleZ = (float) Math.PI;
   }

   @Override
   public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
   {
       poseStack.pushPose();
       poseStack.scale(0.3F, 0.3F, 0.3F);
       super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
       poseStack.popPose();
   }
}
