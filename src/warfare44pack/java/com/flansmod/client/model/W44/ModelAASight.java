
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAASight extends ModelAttachment {
   int textureX = 64;
   int textureY = 64;

   public ModelAASight() {
      attachmentModel = new ModelRendererTurbo[15];
      attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 49, 1, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 25, 9, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 57, 1, textureX, textureY);
      attachmentModel[8] = new ModelRendererTurbo(this, 49, 9, textureX, textureY);
      attachmentModel[9] = new ModelRendererTurbo(this, 17, 17, textureX, textureY);
      attachmentModel[10] = new ModelRendererTurbo(this, 17, 25, textureX, textureY);
      attachmentModel[11] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      attachmentModel[12] = new ModelRendererTurbo(this, 33, 25, textureX, textureY);
      attachmentModel[13] = new ModelRendererTurbo(this, 49, 25, textureX, textureY);
      attachmentModel[14] = new ModelRendererTurbo(this, 1, 33, textureX, textureY);
      attachmentModel[0].addShapeBox(69.5F, -1.0F, -1.5F, 2, 5, 3, 0.0F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 3.0F, 0.0F, -0.3F, 3.0F, 0.0F, -0.3F, 3.0F, 0.0F, -0.3F, 3.0F, 0.0F);
      attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[1].addShapeBox(70.0F, -18.0F, -0.5F, 1, 16, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[2].addShapeBox(70.0F, -10.5F, -8.0F, 1, 1, 16, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[3].addShapeBox(70.0F, -19.0F, -4.5F, 1, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[4].addShapeBox(70.0F, -19.0F, -9.5F, 1, 1, 5, 0.0F, 0.0F, -4.5F, -0.5F, 0.0F, -4.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.5F, -1.5F, 0.0F, 3.5F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[5].addShapeBox(70.0F, -14.5F, -9.0F, 1, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[6].addShapeBox(70.0F, -19.0F, 4.5F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.5F, -0.5F, 0.0F, -4.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.5F, -1.5F, 0.0F, 3.5F, -1.5F);
      attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[7].addShapeBox(70.0F, -14.5F, 8.0F, 1, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[8].addShapeBox(70.0F, -2.0F, 4.5F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.5F, -1.5F, 0.0F, 3.5F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.5F, -0.5F, 0.0F, -4.5F, -0.5F);
      attachmentModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[9].addShapeBox(70.0F, -2.0F, -9.5F, 1, 1, 5, 0.0F, 0.0F, 3.5F, -1.5F, 0.0F, 3.5F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.5F, -0.5F, 0.0F, -4.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[10].addShapeBox(70.0F, -2.0F, -4.5F, 1, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[11].addShapeBox(70.0F, -12.5F, -3.5F, 1, 5, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[12].addShapeBox(70.0F, -13.5F, -2.5F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      attachmentModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[13].addShapeBox(70.0F, -12.5F, 2.5F, 1, 5, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[14].addShapeBox(70.0F, -7.5F, -2.5F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      renderOffset = 0.09F;
      flipAll();
   }
}
