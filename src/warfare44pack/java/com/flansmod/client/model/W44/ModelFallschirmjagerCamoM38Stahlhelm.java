
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelFallschirmjagerCamoM38Stahlhelm extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelFallschirmjagerCamoM38Stahlhelm() {
      headModel = new ModelRendererTurbo[8];
      headModel[0] = new ModelRendererTurbo(this, 73, 41, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 105, 49, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 1, 57, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 41, 57, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 81, 57, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 41, 73, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 81, 73, textureX, textureY);
      headModel[0].addShapeBox(-4.5F, -4.0F, -4.5F, 9, 4, 4, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.5F, 0.0F, -4.0F, 9, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.0F, -5.0F, -4.5F, 8, 1, 8, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, -5.0F, 0.5F, 0.0F, -5.0F, 0.6F, -0.5F, 0.25F, 0.6F, -0.5F, 0.25F, 0.75F, 0.0F, -5.0F, 0.75F, 0.0F, -5.0F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-4.0F, -5.0F, -1.5F, 8, 1, 8, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, -2.0F, 0.5F, 0.0F, -2.0F, 0.75F, 0.0F, 0.0F, 0.75F, 0.0F, 0.0F, 0.75F, 0.0F, -1.75F, 0.75F, 0.0F, -1.75F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(-4.5F, -6.75F, -4.5F, 9, 1, 9, 0.0F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.5F, -8.0F, -4.5F, 9, 2, 9, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-4.5F, -9.0F, -4.5F, 9, 1, 9, 0.0F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[7].addShapeBox(-4.5F, -6.5F, -4.5F, 9, 2, 9, 0.0F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
