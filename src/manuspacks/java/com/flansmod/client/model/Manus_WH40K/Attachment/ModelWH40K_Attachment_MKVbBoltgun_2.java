
package com.flansmod.client.model.Manus_WH40K.Attachment;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Attachment_MKVbBoltgun_2 extends ModelAttachment {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Attachment_MKVbBoltgun_2() {
      attachmentModel = new ModelRendererTurbo[6];
      attachmentModel[0] = new ModelRendererTurbo(this, 0, 100, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 46, 100, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 92, 100, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 0, 130, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 145, 130, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 292, 130, textureX, textureY);
      attachmentModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 4, 5, 18, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[0].setRotationPoint(0.0F, -9.0F, -9.0F);
      attachmentModel[1].addBox(0.0F, 0.0F, 0.0F, 4, 8, 18, 0.0F);
      attachmentModel[1].setRotationPoint(0.0F, -4.0F, -9.0F);
      attachmentModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 4, 5, 18, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F);
      attachmentModel[2].setRotationPoint(0.0F, 4.0F, -9.0F);
      attachmentModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 51, 6, 21, 0.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[3].setRotationPoint(4.0F, -10.5F, -10.5F);
      attachmentModel[4].addBox(0.0F, 0.0F, 0.0F, 51, 9, 21, 0.0F);
      attachmentModel[4].setRotationPoint(4.0F, -4.5F, -10.5F);
      attachmentModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 51, 6, 21, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, -6.0F);
      attachmentModel[5].setRotationPoint(4.0F, 4.5F, -10.5F);
      flipAll();
   }
}
