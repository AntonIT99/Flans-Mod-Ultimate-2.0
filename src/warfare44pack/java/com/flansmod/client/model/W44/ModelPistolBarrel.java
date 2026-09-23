
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelPistolBarrel extends ModelAttachment {
   int textureX = 256;
   int textureY = 64;

   public ModelPistolBarrel() {
      attachmentModel = new ModelRendererTurbo[21];
      attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 145, 1, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 169, 1, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 185, 1, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 201, 1, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 233, 1, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 33, 9, textureX, textureY);
      attachmentModel[8] = new ModelRendererTurbo(this, 249, 1, textureX, textureY);
      attachmentModel[9] = new ModelRendererTurbo(this, 49, 9, textureX, textureY);
      attachmentModel[10] = new ModelRendererTurbo(this, 121, 9, textureX, textureY);
      attachmentModel[11] = new ModelRendererTurbo(this, 193, 9, textureX, textureY);
      attachmentModel[12] = new ModelRendererTurbo(this, 209, 9, textureX, textureY);
      attachmentModel[13] = new ModelRendererTurbo(this, 225, 9, textureX, textureY);
      attachmentModel[14] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      attachmentModel[15] = new ModelRendererTurbo(this, 9, 17, textureX, textureY);
      attachmentModel[16] = new ModelRendererTurbo(this, 233, 17, textureX, textureY);
      attachmentModel[17] = new ModelRendererTurbo(this, 25, 25, textureX, textureY);
      attachmentModel[18] = new ModelRendererTurbo(this, 249, 17, textureX, textureY);
      attachmentModel[19] = new ModelRendererTurbo(this, 97, 25, textureX, textureY);
      attachmentModel[20] = new ModelRendererTurbo(this, 105, 25, textureX, textureY);
      attachmentModel[0].addShapeBox(0.01F, -1.5F, -1.5F, 65, 3, 3, 0.0F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F);
      attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[1].addShapeBox(65.0F, -1.5F, -1.5F, 9, 1, 3, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[2].addShapeBox(65.0F, -1.9F, -0.5F, 5, 1, 1, 0.0F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F);
      attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[3].addShapeBox(65.0F, -4.5F, -0.5F, 5, 3, 1, 0.0F, -1.4F, 0.3F, -0.3F, -1.4F, 0.0F, -0.3F, -1.4F, 0.0F, -0.3F, -1.4F, 0.3F, -0.3F, -0.4F, 0.3F, -0.1F, -0.4F, 0.3F, -0.1F, -0.4F, 0.3F, -0.1F, -0.4F, 0.3F, -0.1F);
      attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[4].addShapeBox(65.0F, -0.5F, -1.5F, 9, 1, 3, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[5].addShapeBox(65.0F, 0.5F, -1.5F, 9, 1, 3, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F);
      attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[6].addShapeBox(1.0F, -0.5F, -0.5F, 3, 12, 1, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F);
      attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[7].addShapeBox(4.0F, -0.5F, -0.5F, 3, 12, 1, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -1.0F, 0.1F, 0.0F, -1.0F, 0.1F, 0.0F, 0.0F, 0.1F);
      attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[8].addShapeBox(0.0F, -0.5F, -0.5F, 1, 12, 1, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 1.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 1.0F, 0.1F);
      attachmentModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[9].addShapeBox(8.0F, -0.5F, -0.5F, 32, 7, 1, 0.0F, 0.0F, 0.0F, 0.1F, 2.0F, -0.1F, 0.1F, 2.0F, -0.1F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -4.0F, 0.1F, 0.0F, -4.0F, 0.1F, 0.0F, 0.0F, 0.1F);
      attachmentModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[10].addShapeBox(8.0F, -0.5F, 0.5F, 32, 7, 1, 0.0F, 0.0F, 0.0F, 0.0F, 2.0F, -0.1F, -0.1F, 2.0F, -1.1F, -0.1F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, -0.1F, 0.0F, -5.0F, -0.1F, 0.0F, -1.0F, 0.0F);
      attachmentModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[11].addShapeBox(4.0F, -0.5F, 0.5F, 3, 12, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      attachmentModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[12].addShapeBox(1.0F, -0.5F, 0.5F, 3, 12, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      attachmentModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[13].addShapeBox(0.0F, -0.5F, 0.5F, 1, 12, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[14].addShapeBox(0.0F, -0.5F, -1.5F, 1, 12, 1, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
      attachmentModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[15].addShapeBox(1.0F, -0.5F, -1.5F, 3, 12, 1, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[16].addShapeBox(4.0F, -0.5F, -1.5F, 3, 12, 1, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[17].addShapeBox(8.0F, -0.5F, -1.5F, 32, 7, 1, 0.0F, 0.0F, -1.0F, 0.0F, 2.0F, -1.1F, -0.1F, 2.0F, -0.1F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -5.0F, -0.1F, 0.0F, -4.0F, -0.1F, 0.0F, 0.0F, 0.0F);
      attachmentModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[18].addShapeBox(7.0F, -0.5F, 0.5F, 1, 11, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      attachmentModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[19].addShapeBox(7.0F, -0.5F, -0.5F, 1, 11, 1, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -2.0F, 0.1F, 0.0F, -2.0F, 0.1F, 0.0F, 0.0F, 0.1F);
      attachmentModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[20].addShapeBox(7.0F, -0.5F, -1.5F, 1, 11, 1, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[20].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentFlashOffset = new Vector3f(0.875F, 0.0F, 0.0F);
      flipAll();
   }
}
