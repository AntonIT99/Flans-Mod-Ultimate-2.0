
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelOpticalSight extends ModelAttachment {
   int textureX = 128;
   int textureY = 128;

   public ModelOpticalSight() {
      attachmentModel = new ModelRendererTurbo[20];
      attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 81, 1, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 97, 9, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 1, 25, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 49, 25, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 1, 33, textureX, textureY);
      attachmentModel[8] = new ModelRendererTurbo(this, 1, 49, textureX, textureY);
      attachmentModel[9] = new ModelRendererTurbo(this, 81, 25, textureX, textureY);
      attachmentModel[10] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      attachmentModel[11] = new ModelRendererTurbo(this, 113, 1, textureX, textureY);
      attachmentModel[12] = new ModelRendererTurbo(this, 113, 9, textureX, textureY);
      attachmentModel[13] = new ModelRendererTurbo(this, 97, 33, textureX, textureY);
      attachmentModel[14] = new ModelRendererTurbo(this, 97, 25, textureX, textureY);
      attachmentModel[15] = new ModelRendererTurbo(this, 113, 25, textureX, textureY);
      attachmentModel[16] = new ModelRendererTurbo(this, 97, 49, textureX, textureY);
      attachmentModel[17] = new ModelRendererTurbo(this, 81, 49, textureX, textureY);
      attachmentModel[18] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      attachmentModel[19] = new ModelRendererTurbo(this, 1, 81, textureX, textureY);
      attachmentModel[0].addShapeBox(-7.0F, -2.0F, 3.5F, 15, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[0].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[1].addShapeBox(-12.0F, -3.0F, 2.5F, 20, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F);
      attachmentModel[1].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[2].addShapeBox(-25.0F, -4.0F, -4.5F, 35, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[2].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[3].addShapeBox(-6.0F, -2.0F, -5.0F, 2, 2, 10, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F);
      attachmentModel[3].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[4].addShapeBox(5.0F, -2.0F, -5.0F, 2, 2, 10, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F, -0.35F, -0.35F, 0.0F);
      attachmentModel[4].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[5].addShapeBox(-12.0F, -3.0F, -4.5F, 20, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, -5.0F, 0.0F, -1.0F);
      attachmentModel[5].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[6].addShapeBox(-7.0F, -2.0F, -4.5F, 15, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[6].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[7].addShapeBox(-25.0F, -4.75F, -4.5F, 35, 1, 9, 0.0F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F);
      attachmentModel[7].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[8].addShapeBox(-25.0F, -5.5F, -4.5F, 35, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[8].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[9].addShapeBox(6.5F, -6.5F, -4.5F, 3, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[9].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[10].addShapeBox(6.5F, -6.5F, -8.5F, 3, 1, 4, 0.0F, 0.0F, 3.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[10].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[11].addShapeBox(6.5F, -6.5F, 4.5F, 3, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F);
      attachmentModel[11].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[12].addShapeBox(6.5F, -22.5F, 4.5F, 3, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, -1.0F, 0.0F, 3.0F, -1.0F);
      attachmentModel[12].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[13].addShapeBox(6.5F, -22.5F, -4.5F, 3, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[13].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[14].addShapeBox(6.5F, -22.5F, -8.5F, 3, 1, 4, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, -1.0F, 0.0F, 3.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[14].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[15].addShapeBox(6.5F, -18.5F, -8.5F, 3, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[15].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[16].addShapeBox(6.5F, -18.5F, 7.5F, 3, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[16].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[17].addShapeBox(8.0F, -19.5F, -5.5F, 1, 11, 11, 0.0F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F, -0.3F, -3.25F, -3.25F);
      attachmentModel[17].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[18].addShapeBox(-24.5F, -5.6F, -4.4F, 34, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[18].setRotationPoint(0.0F, -2.5F, 0.0F);
      attachmentModel[19].addShapeBox(-24.5F, -5.6F, 3.4F, 34, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[19].setRotationPoint(0.0F, -2.5F, 0.0F);
      renderOffset = 0.085F;
      flipAll();
   }
}
