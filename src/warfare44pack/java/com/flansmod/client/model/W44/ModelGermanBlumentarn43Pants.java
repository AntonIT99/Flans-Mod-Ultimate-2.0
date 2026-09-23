
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanBlumentarn43Pants extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelGermanBlumentarn43Pants() {
      leftLegModel = new ModelRendererTurbo[1];
      leftLegModel[0] = new ModelRendererTurbo(this, 97, 66, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 8, 4, 0.0F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F, 0.1F, 2.0F, 0.1F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[1];
      rightLegModel[0] = new ModelRendererTurbo(this, 76, 66, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 8, 4, 0.0F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F, 0.105F, 2.0F, 0.1F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
