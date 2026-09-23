
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanOfficerPants extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 128;

   public ModelGermanOfficerPants() {
      leftLegModel = new ModelRendererTurbo[3];
      leftLegModel[0] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      leftLegModel[1] = new ModelRendererTurbo(this, 161, 73, textureX, textureY);
      leftLegModel[2] = new ModelRendererTurbo(this, 457, 33, textureX, textureY);
      leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 8, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[1].addShapeBox(-1.7F, -3.5F, -2.4F, 4, 5, 5, 0.0F, 0.3F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, 0.3F, 0.0F, -0.4F, 0.1F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F);
      leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      leftLegModel[2].addShapeBox(-2.0F, 4.9F, -2.0F, 4, 2, 4, 0.0F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F);
      leftLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel = new ModelRendererTurbo[3];
      rightLegModel[0] = new ModelRendererTurbo(this, 409, 25, textureX, textureY);
      rightLegModel[1] = new ModelRendererTurbo(this, 137, 73, textureX, textureY);
      rightLegModel[2] = new ModelRendererTurbo(this, 433, 33, textureX, textureY);
      rightLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 8, 4, 0.0F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F);
      rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[1].addShapeBox(-2.3F, -3.5F, -2.4F, 4, 5, 5, 0.0F, -0.2F, 0.0F, -0.4F, 0.3F, 0.0F, -0.4F, 0.3F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, 0.2F, 0.0F, 0.2F, 0.1F, 0.0F, 0.2F, 0.1F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F);
      rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      rightLegModel[2].addShapeBox(-2.0F, 4.9F, -2.0F, 4, 2, 4, 0.0F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.105F, 0.0F, 0.1F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F, 0.305F, 0.0F, 0.25F);
      rightLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
