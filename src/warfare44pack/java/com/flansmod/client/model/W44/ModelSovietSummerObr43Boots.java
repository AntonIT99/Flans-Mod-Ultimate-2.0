
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelSovietSummerObr43Boots extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelSovietSummerObr43Boots() {
      leftLegModel = new ModelRendererTurbo[1];
      leftLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, 6.1F, -2.3F, 4, 6, 5, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, -0.4F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[1];
      rightLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.0F, 6.1F, -2.3F, 4, 6, 5, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -0.4F, 0.4F, 0.0F, -0.4F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
