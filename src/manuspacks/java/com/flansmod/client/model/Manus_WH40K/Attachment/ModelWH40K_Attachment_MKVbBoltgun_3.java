
package com.flansmod.client.model.Manus_WH40K.Attachment;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Attachment_MKVbBoltgun_3 extends ModelAttachment {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Attachment_MKVbBoltgun_3() {
      attachmentModel = new ModelRendererTurbo[8];
      attachmentModel[0] = new ModelRendererTurbo(this, 0, 230, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 75, 230, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 102, 230, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 135, 230, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 166, 230, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 203, 230, textureX, textureY);
      attachmentModel[6] = new ModelRendererTurbo(this, 241, 230, textureX, textureY);
      attachmentModel[7] = new ModelRendererTurbo(this, 297, 230, textureX, textureY);
      attachmentModel[0].addBox(0.0F, 0.0F, 0.0F, 28, 7, 9, 0.0F);
      attachmentModel[0].setRotationPoint(-28.0F, -3.5F, -4.5F);
      attachmentModel[1].addBox(0.0F, 0.0F, 0.0F, 4, 13, 9, 0.0F);
      attachmentModel[1].setRotationPoint(-32.0F, -6.5F, -4.5F);
      attachmentModel[2].addBox(0.0F, 0.0F, 0.0F, 7, 3, 9, 0.0F);
      attachmentModel[2].setRotationPoint(-39.0F, -6.5F, -4.5F);
      attachmentModel[3].addBox(0.0F, 0.0F, 0.0F, 6, 3, 9, 0.0F);
      attachmentModel[3].setRotationPoint(-38.0F, 3.5F, -4.5F);
      attachmentModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 4, 16, 14, 0.0F, 3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[4].setRotationPoint(-42.0F, -19.5F, -7.0F);
      attachmentModel[5].addBox(0.0F, 0.0F, 0.0F, 4, 17, 14, 0.0F);
      attachmentModel[5].setRotationPoint(-42.0F, -3.5F, -7.0F);
      attachmentModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 7, 24, 20, 0.0F, 4.5F, 0.0F, 0.0F, -4.5F, 0.0F, 0.0F, -4.5F, 0.0F, 0.0F, 4.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[6].setRotationPoint(-49.0F, -27.5F, -10.0F);
      attachmentModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 7, 25, 20, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
      attachmentModel[7].setRotationPoint(-49.0F, -3.5F, -10.0F);
      renderOffset = 0.0F;
      flipAll();
   }
}
