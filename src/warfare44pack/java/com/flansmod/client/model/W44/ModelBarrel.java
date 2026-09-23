package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.common.vector.Vector3f;

public class ModelBarrel extends ModelAttachment {
   int textureX = 256;
   int textureY = 64;

   public ModelBarrel() {
      attachmentFlashOffset = new Vector3f(0.0F, 0.0F, 0.0F);
      flipAll();
   }
}
