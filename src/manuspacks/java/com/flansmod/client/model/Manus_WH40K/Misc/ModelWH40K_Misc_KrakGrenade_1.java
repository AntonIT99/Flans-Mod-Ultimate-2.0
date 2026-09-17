
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;


public class ModelWH40K_Misc_KrakGrenade_1 extends ModelBase {
   int textureX = 128;
   int textureY = 128;
   public ModelRendererTurbo[] krakgrenadeModel = new ModelRendererTurbo[25];

   public ModelWH40K_Misc_KrakGrenade_1() {
      krakgrenadeModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      krakgrenadeModel[1] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      krakgrenadeModel[2] = new ModelRendererTurbo(this, 81, 1, textureX, textureY);
      krakgrenadeModel[3] = new ModelRendererTurbo(this, 33, 25, textureX, textureY);
      krakgrenadeModel[4] = new ModelRendererTurbo(this, 65, 25, textureX, textureY);
      krakgrenadeModel[5] = new ModelRendererTurbo(this, 1, 41, textureX, textureY);
      krakgrenadeModel[6] = new ModelRendererTurbo(this, 25, 57, textureX, textureY);
      krakgrenadeModel[7] = new ModelRendererTurbo(this, 97, 25, textureX, textureY);
      krakgrenadeModel[8] = new ModelRendererTurbo(this, 89, 49, textureX, textureY);
      krakgrenadeModel[9] = new ModelRendererTurbo(this, 57, 65, textureX, textureY);
      krakgrenadeModel[10] = new ModelRendererTurbo(this, 1, 65, textureX, textureY);
      krakgrenadeModel[11] = new ModelRendererTurbo(this, 81, 73, textureX, textureY);
      krakgrenadeModel[12] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      krakgrenadeModel[13] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      krakgrenadeModel[14] = new ModelRendererTurbo(this, 65, 1, textureX, textureY);
      krakgrenadeModel[15] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      krakgrenadeModel[16] = new ModelRendererTurbo(this, 105, 1, textureX, textureY);
      krakgrenadeModel[17] = new ModelRendererTurbo(this, 33, 9, textureX, textureY);
      krakgrenadeModel[18] = new ModelRendererTurbo(this, 113, 9, textureX, textureY);
      krakgrenadeModel[19] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      krakgrenadeModel[20] = new ModelRendererTurbo(this, 73, 17, textureX, textureY);
      krakgrenadeModel[21] = new ModelRendererTurbo(this, 65, 9, textureX, textureY);
      krakgrenadeModel[22] = new ModelRendererTurbo(this, 57, 25, textureX, textureY);
      krakgrenadeModel[23] = new ModelRendererTurbo(this, 121, 1, textureX, textureY);
      krakgrenadeModel[24] = new ModelRendererTurbo(this, 113, 17, textureX, textureY);
      krakgrenadeModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 6, 12, 12, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[0].setRotationPoint(-3.0F, -13.0F, -6.0F);
      krakgrenadeModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 4, 2, 14, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F);
      krakgrenadeModel[1].setRotationPoint(-7.0F, -8.0F, -7.0F);
      krakgrenadeModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 3, 4, 12, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, -4.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F);
      krakgrenadeModel[2].setRotationPoint(-6.0F, -1.0F, -6.0F);
      krakgrenadeModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 3, 12, 12, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      krakgrenadeModel[3].setRotationPoint(-6.0F, -13.0F, -6.0F);
      krakgrenadeModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 3, 12, 12, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[4].setRotationPoint(3.0F, -13.0F, -6.0F);
      krakgrenadeModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 6, 4, 12, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F);
      krakgrenadeModel[5].setRotationPoint(-3.0F, -1.0F, -6.0F);
      krakgrenadeModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 6, 2, 14, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[6].setRotationPoint(-3.0F, -8.0F, -7.0F);
      krakgrenadeModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 3, 4, 12, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F);
      krakgrenadeModel[7].setRotationPoint(3.0F, -1.0F, -6.0F);
      krakgrenadeModel[8].addShapeBox(0.0F, 0.0F, 0.0F, 3, 4, 12, 0.0F, 1.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[8].setRotationPoint(3.0F, -17.0F, -6.0F);
      krakgrenadeModel[9].addShapeBox(0.0F, 0.0F, 0.0F, 6, 4, 12, 0.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F, -1.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[9].setRotationPoint(-3.0F, -17.0F, -6.0F);
      krakgrenadeModel[10].addShapeBox(0.0F, 0.0F, 0.0F, 3, 4, 12, 0.0F, -4.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F, 1.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F);
      krakgrenadeModel[10].setRotationPoint(-6.0F, -17.0F, -6.0F);
      krakgrenadeModel[11].addShapeBox(0.0F, 0.0F, 0.0F, 4, 2, 14, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[11].setRotationPoint(3.0F, -8.0F, -7.0F);
      krakgrenadeModel[12].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[12].setRotationPoint(1.0F, 3.0F, -2.0F);
      krakgrenadeModel[13].addBox(0.0F, 0.0F, 0.0F, 2, 1, 4, 0.0F);
      krakgrenadeModel[13].setRotationPoint(-1.0F, 3.0F, -2.0F);
      krakgrenadeModel[14].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 4, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);
      krakgrenadeModel[14].setRotationPoint(-2.0F, 3.0F, -2.0F);
      krakgrenadeModel[15].addBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F);
      krakgrenadeModel[15].setRotationPoint(-1.0F, 4.0F, -1.0F);
      krakgrenadeModel[16].addBox(0.0F, 0.0F, 0.0F, 3, 2, 4, 0.0F);
      krakgrenadeModel[16].setRotationPoint(-1.5F, 5.0F, -2.0F);
      krakgrenadeModel[17].addBox(0.0F, 0.0F, 0.0F, 6, 1, 1, 0.0F);
      krakgrenadeModel[17].setRotationPoint(-3.5F, 5.5F, -0.5F);
      krakgrenadeModel[18].addShapeBox(-0.5F, -0.5F, -2.5F, 1, 1, 5, 0.0F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.25F);
      krakgrenadeModel[18].setRotationPoint(-3.0F, 6.0F, 0.0F);
      krakgrenadeModel[18].rotateAngleZ = (float) (-Math.PI / 3);
      krakgrenadeModel[19].addShapeBox(-3.5F, -0.5F, -2.5F, 3, 1, 1, 0.0F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F);
      krakgrenadeModel[19].setRotationPoint(-3.0F, 6.0F, 4.0F);
      krakgrenadeModel[19].rotateAngleZ = (float) (-Math.PI / 3);
      krakgrenadeModel[20].addShapeBox(-4.5F, -0.5F, -2.5F, 1, 1, 5, 0.0F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.75F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.75F);
      krakgrenadeModel[20].setRotationPoint(-3.0F, 6.0F, 0.0F);
      krakgrenadeModel[20].rotateAngleZ = (float) (-Math.PI / 3);
      krakgrenadeModel[21].addShapeBox(-3.5F, -0.5F, -6.5F, 3, 1, 1, 0.0F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F, 0.25F, -0.25F, -0.25F);
      krakgrenadeModel[21].setRotationPoint(-3.0F, 6.0F, 4.0F);
      krakgrenadeModel[21].rotateAngleZ = (float) (-Math.PI / 3);
      krakgrenadeModel[22].addShapeBox(0.0F, 0.0F, 0.0F, 2, 2, 6, 0.0F, 0.0F, 3.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F);
      krakgrenadeModel[22].setRotationPoint(-1.0F, 5.0F, -8.0F);
      krakgrenadeModel[23].addShapeBox(0.0F, 0.0F, 0.0F, 2, 10, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[23].setRotationPoint(-1.0F, -8.0F, -8.0F);
      krakgrenadeModel[24].addShapeBox(0.0F, 0.0F, 0.0F, 2, 6, 1, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      krakgrenadeModel[24].setRotationPoint(-1.0F, -14.0F, -8.0F);
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
