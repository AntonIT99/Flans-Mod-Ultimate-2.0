
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWaffenM42CamoStahlhelm extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelWaffenM42CamoStahlhelm() {
      headModel = new ModelRendererTurbo[7];
      headModel[0] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 25, 17, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 41, 17, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 81, 17, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 1, 33, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 25, 33, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 65, 33, textureX, textureY);
      headModel[0].addShapeBox(-4.5F, -4.0F, -4.5F, 9, 4, 4, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.5F, 0.0F, -4.0F, 9, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.5F, -8.0F, -4.5F, 9, 2, 9, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-4.5F, -9.0F, -4.5F, 9, 1, 9, 0.0F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(-4.5F, -5.0F, -4.5F, 9, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.25F, -0.25F, 0.8F, 0.25F, -0.25F, 0.8F, 0.2F, -0.25F, 0.0F, 0.2F, -0.25F, 0.0F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.5F, -7.0F, -4.5F, 9, 2, 9, 0.0F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-5.5F, -5.0F, -3.5F, 11, 3, 9, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -0.25F, -0.5F, -2.0F, -0.25F, -0.5F, -2.0F, -0.5F, -0.8F, -0.25F, -0.5F, -0.8F, -0.25F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
