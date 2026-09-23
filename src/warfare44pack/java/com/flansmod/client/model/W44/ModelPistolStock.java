
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelPistolStock extends ModelAttachment {
   int textureX = 128;
   int textureY = 128;

   public ModelPistolStock() {
      attachmentModel = new ModelRendererTurbo[9];
      attachmentModel[0] = new ModelRendererTurbo(this, 41, 41, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 73, 49, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 1, 57, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 57, 65, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 49, 81, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 73, 49, textureX, textureY);
      attachmentModel[8] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      attachmentModel[0].addShapeBox(-12.0F, 11.75F, -4.0F, 9, 4, 8, 0.0F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, 0.0F, -0.8F);
      attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[0].rotateAngleZ = 0.06108652F;
      attachmentModel[1].addShapeBox(-28.0F, 11.75F, -4.0F, 16, 4, 8, 0.0F, -7.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F, -7.0F, 2.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, -7.0F, 2.0F, -0.8F);
      attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[1].rotateAngleZ = 0.06108652F;
      attachmentModel[2].addShapeBox(-52.0F, 11.75F, -4.0F, 22, 6, 8, 0.0F, 2.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 2.0F, 0.0F, -0.8F, 2.5F, 7.0F, -0.8F, 0.0F, -2.0F, -0.8F, 0.0F, -2.0F, -0.8F, 2.5F, 7.0F, -0.8F);
      attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[2].rotateAngleZ = 0.06108652F;
      attachmentModel[3].addShapeBox(-12.0F, 4.75F, -4.0F, 9, 7, 8, 0.0F, 0.0F, 0.0F, -1.8F, 0.0F, -1.0F, -0.8F, 0.0F, -1.0F, -0.8F, 0.0F, 0.0F, -1.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F);
      attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[3].rotateAngleZ = 0.06108652F;
      attachmentModel[4].addShapeBox(-28.0F, 4.75F, -4.0F, 16, 7, 8, 0.0F, -7.0F, -2.0F, -1.8F, 0.0F, 0.0F, -1.8F, 0.0F, 0.0F, -1.8F, -7.0F, -2.0F, -1.8F, -7.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F);
      attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[4].rotateAngleZ = 0.06108652F;
      attachmentModel[5].addShapeBox(-52.0F, 4.75F, -4.0F, 22, 7, 8, 0.0F, 3.0F, 1.0F, -1.8F, 0.0F, 2.0F, -1.8F, 0.0F, 2.0F, -1.8F, 3.0F, 1.0F, -1.8F, 2.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 2.0F, 0.0F, -0.8F);
      attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[5].rotateAngleZ = 0.06108652F;
      attachmentModel[6].addShapeBox(-30.0F, 4.75F, -4.0F, 16, 7, 8, 0.0F, -3.0F, 0.0F, -1.8F, -7.0F, -2.0F, -1.8F, -7.0F, -2.0F, -1.8F, -3.0F, 0.0F, -1.8F, -1.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F, -1.0F, 0.0F, -0.8F);
      attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[6].rotateAngleZ = 0.06108652F;
      attachmentModel[7].addShapeBox(-30.0F, 11.75F, -4.0F, 16, 4, 8, 0.0F, 0.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F, -7.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, -7.0F, 2.0F, -0.8F, -7.0F, 2.0F, -0.8F, 0.0F, 0.0F, -0.8F);
      attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[7].rotateAngleZ = 0.06108652F;
      attachmentModel[8].addShapeBox(-37.5F, 4.75F, -4.0F, 16, 7, 8, 0.0F, -7.5F, 2.0F, -1.8F, -5.5F, 0.0F, -1.8F, -5.5F, 0.0F, -1.8F, -7.5F, 2.0F, -1.8F, -7.5F, 0.0F, -0.8F, -7.5F, 0.0F, -0.8F, -7.5F, 0.0F, -0.8F, -7.5F, 0.0F, -0.8F);
      attachmentModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[8].rotateAngleZ = 0.06108652F;
      flipAll();
   }
}
