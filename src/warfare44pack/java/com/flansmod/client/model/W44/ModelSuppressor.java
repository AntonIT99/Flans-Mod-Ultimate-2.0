
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelSuppressor extends ModelAttachment {
   int textureX = 64;
   int textureY = 64;

   public ModelSuppressor() {
      attachmentModel = new ModelRendererTurbo[9];
      attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 1, 25, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 33, 25, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 1, 33, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 1, 41, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 1, 49, textureX, textureY);
      attachmentModel[8] = new ModelRendererTurbo(this, 1, 57, textureX, textureY);
      attachmentModel[0].addShapeBox(-4.0F, -1.0F, -2.0F, 16, 2, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[1].addShapeBox(-4.0F, 1.0F, -2.0F, 16, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[2].addShapeBox(-4.0F, -2.0F, -2.0F, 16, 1, 4, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[3].addShapeBox(12.0F, 0.5F, -1.5F, 12, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[4].addShapeBox(12.0F, -0.5F, -1.5F, 12, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[5].addShapeBox(12.0F, -1.5F, -1.5F, 12, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[6].addShapeBox(-2.25F, 1.25F, -2.5F, 11, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[7].addShapeBox(-2.25F, -1.0F, -2.5F, 11, 2, 5, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F);
      attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentModel[8].addShapeBox(-2.25F, -2.25F, -2.5F, 11, 1, 5, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      attachmentFlashOffset = new Vector3f(0.25F, 0.015625F, 0.0F);
      flipAll();
   }
}
