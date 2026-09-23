
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelTripMine extends ModelBase {
   int textureX = 512;
   int textureY = 256;
   public ModelRendererTurbo[] tripmineModel = new ModelRendererTurbo[13];

   public ModelTripMine() {
      tripmineModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      tripmineModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      tripmineModel[2] = new ModelRendererTurbo(this, 65, 1, textureX, textureY);
      tripmineModel[3] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      tripmineModel[4] = new ModelRendererTurbo(this, 129, 1, textureX, textureY);
      tripmineModel[5] = new ModelRendererTurbo(this, 161, 1, textureX, textureY);
      tripmineModel[6] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      tripmineModel[7] = new ModelRendererTurbo(this, 193, 1, textureX, textureY);
      tripmineModel[8] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      tripmineModel[9] = new ModelRendererTurbo(this, 217, 1, textureX, textureY);
      tripmineModel[10] = new ModelRendererTurbo(this, 57, 1, textureX, textureY);
      tripmineModel[11] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      tripmineModel[12] = new ModelRendererTurbo(this, 249, 1, textureX, textureY);
      tripmineModel[0].addShapeBox(-8.5F, -16.0F, 100.0F, 3, 22, 10, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      tripmineModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[0].rotateAngleZ = (float) Math.PI;
      tripmineModel[1].addShapeBox(-1.5F, -16.0F, 100.0F, 3, 22, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      tripmineModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[1].rotateAngleZ = (float) Math.PI;
      tripmineModel[2].addShapeBox(-5.5F, -16.0F, 100.0F, 4, 22, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      tripmineModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[2].rotateAngleZ = (float) Math.PI;
      tripmineModel[3].addShapeBox(-8.5F, -19.0F, 100.0F, 3, 3, 10, 0.0F, -3.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, -3.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      tripmineModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[3].rotateAngleZ = (float) Math.PI;
      tripmineModel[4].addShapeBox(-5.5F, -19.0F, 100.0F, 4, 3, 10, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      tripmineModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[4].rotateAngleZ = (float) Math.PI;
      tripmineModel[5].addShapeBox(-1.5F, -19.0F, 100.0F, 3, 3, 10, 0.0F, 0.0F, 0.0F, -3.0F, -3.0F, 0.0F, -3.0F, -3.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      tripmineModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[5].rotateAngleZ = (float) Math.PI;
      tripmineModel[6].addShapeBox(-4.0F, -23.0F, 104.5F, 1, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F, 0.15F, 0.0F, 0.15F);
      tripmineModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[6].rotateAngleZ = (float) Math.PI;
      tripmineModel[7].addShapeBox(-5.5F, 6.0F, 103.0F, 4, 15, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F);
      tripmineModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[7].rotateAngleZ = (float) Math.PI;
      tripmineModel[8].addShapeBox(-5.5F, 21.0F, 103.0F, 4, 3, 4, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F);
      tripmineModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[8].rotateAngleZ = (float) Math.PI;
      tripmineModel[9].addShapeBox(-5.5F, -5.0F, -105.0F, 4, 26, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F);
      tripmineModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[9].rotateAngleZ = (float) Math.PI;
      tripmineModel[10].addShapeBox(-5.5F, 21.0F, -105.0F, 4, 3, 4, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F);
      tripmineModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[10].rotateAngleZ = (float) Math.PI;
      tripmineModel[11].addShapeBox(-4.0F, -22.0F, -102.5F, 1, 1, 208, 0.0F, -0.15F, -15.15F, -0.15F, -0.15F, -15.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, 15.85F, -0.15F, -0.15F, 15.85F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F);
      tripmineModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[11].rotateAngleZ = (float) Math.PI;
      tripmineModel[12].addShapeBox(-5.5F, -12.0F, -105.0F, 4, 7, 4, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      tripmineModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      tripmineModel[12].rotateAngleZ = (float) Math.PI;
   }

   @Override
   public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
   {
       poseStack.pushPose();
       poseStack.scale(0.25F, 0.25F, 0.25F);
       super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, renderPass);
       poseStack.popPose();
   }
}
