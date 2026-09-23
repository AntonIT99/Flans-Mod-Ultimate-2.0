
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelJapaneseType98PantsKhaki extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelJapaneseType98PantsKhaki() {
      leftLegModel = new ModelRendererTurbo[1];
      leftLegModel[0] = new ModelRendererTurbo(this, 65, 9, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 10, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[1];
      rightLegModel[0] = new ModelRendererTurbo(this, 41, 9, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.01F, -0.09F, -2.01F, 4, 10, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
