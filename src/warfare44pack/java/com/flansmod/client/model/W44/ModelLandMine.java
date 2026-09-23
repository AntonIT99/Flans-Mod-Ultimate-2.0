
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelLandMine extends ModelBase {
   int textureX = 64;
   int textureY = 64;
   public ModelRendererTurbo[] landmineModel = new ModelRendererTurbo[13];

   public ModelLandMine() {
      landmineModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      landmineModel[1] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      landmineModel[2] = new ModelRendererTurbo(this, 1, 25, textureX, textureY);
      landmineModel[3] = new ModelRendererTurbo(this, 1, 33, textureX, textureY);
      landmineModel[4] = new ModelRendererTurbo(this, 33, 33, textureX, textureY);
      landmineModel[5] = new ModelRendererTurbo(this, 1, 41, textureX, textureY);
      landmineModel[6] = new ModelRendererTurbo(this, 33, 41, textureX, textureY);
      landmineModel[7] = new ModelRendererTurbo(this, 1, 49, textureX, textureY);
      landmineModel[8] = new ModelRendererTurbo(this, 33, 49, textureX, textureY);
      landmineModel[9] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      landmineModel[10] = new ModelRendererTurbo(this, 49, 9, textureX, textureY);
      landmineModel[11] = new ModelRendererTurbo(this, 41, 17, textureX, textureY);
      landmineModel[12] = new ModelRendererTurbo(this, 41, 17, textureX, textureY);
      landmineModel[0].addShapeBox(-7.0F, 0.0F, -4.0F, 14, 2, 8, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[0].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[0].rotateAngleZ = (float) Math.PI;
      landmineModel[1].addShapeBox(-7.0F, 0.0F, -7.0F, 14, 2, 3, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[1].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[1].rotateAngleZ = (float) Math.PI;
      landmineModel[2].addShapeBox(-7.0F, 0.0F, 4.0F, 14, 2, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F);
      landmineModel[2].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[2].rotateAngleZ = (float) Math.PI;
      landmineModel[3].addShapeBox(-6.0F, -0.5F, -3.0F, 12, 1, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[3].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[3].rotateAngleZ = (float) Math.PI;
      landmineModel[4].addShapeBox(-6.0F, -0.5F, 3.0F, 12, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F);
      landmineModel[4].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[4].rotateAngleZ = (float) Math.PI;
      landmineModel[5].addShapeBox(-5.0F, -1.0F, -2.0F, 10, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[5].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[5].rotateAngleZ = (float) Math.PI;
      landmineModel[6].addShapeBox(-5.0F, -1.0F, -5.0F, 10, 1, 3, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[6].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[6].rotateAngleZ = (float) Math.PI;
      landmineModel[7].addShapeBox(-5.0F, -1.0F, 2.0F, 10, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F);
      landmineModel[7].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[7].rotateAngleZ = (float) Math.PI;
      landmineModel[8].addShapeBox(-6.0F, -0.5F, -6.0F, 12, 1, 3, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[8].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[8].rotateAngleZ = (float) Math.PI;
      landmineModel[9].addShapeBox(-9.5F, 0.5F, -5.0F, 4, 1, 1, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      landmineModel[9].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[9].rotateAngleZ = (float) Math.PI;
      landmineModel[10].addShapeBox(-9.5F, 0.5F, 4.0F, 4, 1, 1, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      landmineModel[10].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[10].rotateAngleZ = (float) Math.PI;
      landmineModel[11].addShapeBox(-10.5F, -0.5F, -5.0F, 1, 1, 10, 0.0F, 0.0F, 0.3F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F, -1.0F, 0.0F, -0.3F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, -1.0F);
      landmineModel[11].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[11].rotateAngleZ = (float) Math.PI;
      landmineModel[12].addShapeBox(-1.0F, -2.25F, -1.0F, 2, 2, 2, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      landmineModel[12].setRotationPoint(0.0F, -0.75F, 0.0F);
      landmineModel[12].rotateAngleZ = (float) Math.PI;
   }

   @Override
   public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
   {
       poseStack.pushPose();
       poseStack.scale(0.7F, 0.7F, 0.7F);
       super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
       poseStack.popPose();
   }
}
