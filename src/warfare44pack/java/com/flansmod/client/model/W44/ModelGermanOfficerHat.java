
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanOfficerHat extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 128;

   public ModelGermanOfficerHat() {
      headModel = new ModelRendererTurbo[7];
      headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 305, 1, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 377, 1, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 433, 9, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 305, 25, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 345, 25, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 305, 41, textureX, textureY);
      headModel[0].addShapeBox(-11.75F, -8.2F, -4.6F, 150, 68, 1, 0.0F, -10.5F, 0.0F, 0.5F, -137.0F, 0.0F, 0.5F, -137.0F, 0.0F, 0.0F, -10.5F, 0.0F, 0.0F, -10.5F, -67.0F, 0.0F, -137.0F, -67.0F, 0.0F, -137.0F, -67.0F, 0.0F, -10.5F, -67.0F, 0.0F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.0F, -7.3F, -4.0F, 16, 2, 16, 0.0F, -0.1F, 0.0F, 0.2F, -8.1F, 0.0F, 0.2F, -8.1F, 0.0F, -7.8F, -0.1F, 0.0F, -7.8F, 0.2F, 0.0F, 0.2F, -7.8F, 0.0F, 0.2F, -7.8F, 0.0F, -7.8F, 0.2F, 0.0F, -7.8F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.0F, -8.3F, -4.0F, 16, 2, 16, 0.0F, 1.0F, 0.4F, 1.0F, -7.2F, 0.4F, 1.0F, -7.2F, -0.3F, -7.0F, 1.0F, -0.3F, -7.0F, 0.0F, -0.2F, 0.2F, -8.0F, -0.2F, 0.2F, -8.0F, -0.2F, -7.8F, 0.0F, -0.2F, -7.8F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-4.0F, -5.8F, -5.0F, 16, 1, 16, 0.0F, 0.2F, -0.5F, -0.3F, -7.8F, -0.5F, -0.3F, -7.8F, -0.5F, -6.8F, 0.2F, -0.5F, -6.8F, 0.2F, 0.0F, 0.6F, -7.8F, 0.0F, 0.6F, -7.8F, 0.0F, -6.8F, 0.2F, 0.0F, -6.8F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(-4.0F, -6.45F, -4.01F, 8, 1, 8, 0.0F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.0F, -6.2F, -4.01F, 8, 1, 8, 0.0F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.2F, -0.4F, 0.2F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F, 0.25F, -0.4F, 0.25F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-1.6F, -6.9F, -4.3F, 95, 59, 1, 0.0F, 0.0F, 0.0F, 0.0F, -92.0F, 0.0F, 0.0F, -92.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -57.5F, 0.0F, -92.0F, -57.5F, 0.0F, -92.0F, -57.5F, 0.0F, 0.0F, -57.5F, 0.0F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
