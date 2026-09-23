
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanMPOfficerHelmet extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelAmericanMPOfficerHelmet() {
      headModel = new ModelRendererTurbo[8];
      headModel[0] = new ModelRendererTurbo(this, 23, 108, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 49, 1, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 41, 9, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 33, 17, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 73, 25, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 33, 33, textureX, textureY);
      headModel[0].addShapeBox(-6.0F, -12.2F, -4.75F, 12, 12, 1, 0.0F, -4.0F, -4.0F, -0.65F, -4.0F, -4.0F, -0.65F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F, -4.0F, -4.0F, 0.0F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.0F, -8.0F, -4.0F, 8, 3, 4, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.4F, -0.1F, 0.6F, 0.4F, -0.1F, 0.6F, 0.4F, 1.2F, 0.1F, 0.4F, 1.2F, 0.1F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.0F, -9.0F, -4.0F, 8, 1, 8, 0.0F, -0.9F, 0.0F, -0.9F, -0.9F, 0.0F, -0.9F, -0.9F, 0.3F, -1.4F, -0.9F, 0.3F, -1.4F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.3F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-4.0F, -8.0F, 0.2F, 8, 3, 4, 0.0F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.4F, 1.2F, 0.1F, 0.4F, 1.2F, 0.1F, 0.4F, 0.8F, 0.4F, 0.4F, 0.8F, 0.4F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(-4.0F, -4.0F, 0.4F, 8, 1, 4, 0.0F, 0.4F, -0.2F, 0.3F, 0.4F, -0.2F, 0.3F, 0.4F, 0.2F, 0.2F, 0.4F, 0.2F, 0.2F, 0.9F, -0.3F, 0.3F, 0.9F, -0.3F, 0.3F, 0.6F, -0.7F, 0.7F, 0.6F, -0.7F, 0.7F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.0F, -4.0F, -4.2F, 8, 1, 4, 0.0F, 0.4F, 1.1F, 0.4F, 0.4F, 1.1F, 0.4F, 0.4F, -0.2F, 0.3F, 0.4F, -0.2F, 0.3F, 0.6F, -1.6F, 1.3F, 0.6F, -1.6F, 1.3F, 0.9F, -0.3F, 0.3F, 0.9F, -0.3F, 0.3F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-4.5F, -4.0F, -4.5F, 9, 4, 4, 0.0F, 0.0F, -0.25F, -3.0F, 0.0F, -0.25F, -3.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[7].addShapeBox(-4.5F, 0.0F, -4.0F, 9, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F);
      headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
