
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanM42AirbornePants extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelAmericanM42AirbornePants() {
      leftLegModel = new ModelRendererTurbo[5];
      leftLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      leftLegModel[1] = new ModelRendererTurbo(this, 57, 57, textureX, textureY);
      leftLegModel[2] = new ModelRendererTurbo(this, 97, 57, textureX, textureY);
      leftLegModel[3] = new ModelRendererTurbo(this, 1, 65, textureX, textureY);
      leftLegModel[4] = new ModelRendererTurbo(this, 17, 65, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 11, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[1].addShapeBox(-2.0F, 1.9F, -2.15F, 4, 4, 1, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F);
      leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[2].addShapeBox(1.1F, 1.5F, -1.5F, 1, 3, 3, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.25F, 0.1F, 0.1F, -0.25F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      leftLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[3].addShapeBox(1.15F, 0.5F, -1.5F, 1, 1, 3, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.1F);
      leftLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[4].addShapeBox(-2.0F, 2.0F, -2.0F, 4, 1, 4, 0.0F, 0.2F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F);
      leftLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[5];
      rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      rightLegModel[1] = new ModelRendererTurbo(this, 1, 57, textureX, textureY);
      rightLegModel[2] = new ModelRendererTurbo(this, 97, 49, textureX, textureY);
      rightLegModel[3] = new ModelRendererTurbo(this, 73, 57, textureX, textureY);
      rightLegModel[4] = new ModelRendererTurbo(this, 41, 65, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 11, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[1].addShapeBox(-2.0F, 1.9F, -2.15F, 4, 4, 1, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F, -0.25F, 0.25F, 0.0F);
      rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[2].addShapeBox(-2.15F, 0.5F, -1.5F, 1, 1, 3, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.1F);
      rightLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[3].addShapeBox(-2.1F, 1.5F, -1.5F, 1, 3, 3, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.25F, 0.1F, 0.1F, -0.25F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      rightLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[4].addShapeBox(-2.0F, 2.0F, -2.0F, 4, 1, 4, 0.0F, 0.3F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F);
      rightLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
