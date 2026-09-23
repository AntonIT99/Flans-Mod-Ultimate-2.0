
package com.flansmod.client.model.Manus_WH40K.Attachment;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Attachment_MKVbBoltgun_1 extends ModelAttachment {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Attachment_MKVbBoltgun_1() {
      attachmentModel = new ModelRendererTurbo[6];
      attachmentModel[0] = new ModelRendererTurbo(this, 0, 190, textureX, textureY);
      attachmentModel[1] = new ModelRendererTurbo(this, 74, 190, textureX, textureY);
      attachmentModel[2] = new ModelRendererTurbo(this, 188, 190, textureX, textureY);
      attachmentModel[3] = new ModelRendererTurbo(this, 303, 190, textureX, textureY);
      attachmentModel[4] = new ModelRendererTurbo(this, 403, 190, textureX, textureY);
      attachmentModel[5] = new ModelRendererTurbo(this, 445, 190, textureX, textureY);
      attachmentModel[0].addBox(0.0F, 0.0F, 0.0F, 24, 6, 12, 0.0F);
      attachmentModel[0].setRotationPoint(-12.0F, -6.0F, -6.0F);
      attachmentModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 38, 13, 18, 0.0F, 0.0F, 0.0F, 0.0F, -2.1176472F, 0.0F, 0.0F, -2.1176472F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -9.0F, 0.0F, 0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[1].setRotationPoint(-16.0F, -19.0F, -9.0F);
      attachmentModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 38, 4, 18, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F, -2.1176472F, 0.0F, 0.0F, -2.1176472F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[2].setRotationPoint(-16.0F, -23.0F, -9.0F);
      attachmentModel[3].addBox(0.0F, 0.0F, 0.0F, 36, 13, 13, 0.0F);
      attachmentModel[3].setRotationPoint(-18.0F, -20.0F, -6.5F);
      attachmentModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 10, 10, 10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F);
      attachmentModel[4].setRotationPoint(-28.0F, -18.5F, -5.0F);
      attachmentModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 12, 16, 16, 0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      attachmentModel[5].setRotationPoint(18.0F, -21.5F, -8.0F);
      renderOffset = 0.0F;
      flipAll();
   }
}
