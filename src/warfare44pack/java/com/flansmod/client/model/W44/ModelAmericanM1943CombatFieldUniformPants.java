
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanM1943CombatFieldUniformPants extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelAmericanM1943CombatFieldUniformPants() {
      leftLegModel = new ModelRendererTurbo[2];
      leftLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      leftLegModel[1] = new ModelRendererTurbo(this, 97, 33, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 6, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[1].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 4, 4, 0.0F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F);
      leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[2];
      rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      rightLegModel[1] = new ModelRendererTurbo(this, 57, 33, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 6, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[1].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 4, 4, 0.0F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F, 0.05F, 0.0F, 0.05F);
      rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
