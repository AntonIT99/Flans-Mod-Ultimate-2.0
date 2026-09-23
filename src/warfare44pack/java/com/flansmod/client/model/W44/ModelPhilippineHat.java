
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelPhilippineHat extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelPhilippineHat() {
      headModel = new ModelRendererTurbo[7];
      headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 81, 1, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 1, 17, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 33, 17, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 65, 17, textureX, textureY);
      headModel[0].addShapeBox(-4.5F, -4.0F, -4.5F, 9, 4, 4, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.5F, 0.0F, -4.0F, 9, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.0F, -10.5F, -4.1F, 8, 2, 8, 0.0F, -0.9F, -0.6F, -1.5F, -0.9F, -0.6F, -1.5F, -0.9F, -0.7F, -1.25F, -0.9F, -0.7F, -1.25F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.3F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-4.0F, -8.5F, -5.1F, 8, 3, 5, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 1.5F, 1.0F, 0.1F, 1.5F, 1.0F, 0.1F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(-4.0F, -8.5F, 0.1F, 8, 3, 4, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 1.5F, 1.0F, 0.1F, 1.5F, 1.0F, 0.1F, 0.1F, 1.6F, 0.6F, 0.1F, 1.6F, 0.6F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.0F, -4.5F, 0.3F, 8, 1, 4, 0.0F, 1.5F, 0.0F, 0.3F, 1.5F, 0.0F, 0.3F, 0.1F, -0.6F, 0.4F, 0.1F, -0.6F, 0.4F, 2.9F, -0.2F, 0.3F, 2.9F, -0.2F, 0.3F, 1.3F, 0.1F, 1.5F, 1.3F, 0.1F, 1.5F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-4.0F, -4.5F, -4.3F, 8, 1, 4, 0.0F, 0.0F, 0.7F, 0.7F, 0.0F, 0.7F, 0.7F, 1.5F, 0.0F, 0.3F, 1.5F, 0.0F, 0.3F, 1.2F, -1.3F, 1.8F, 1.2F, -1.3F, 1.8F, 2.9F, -0.2F, 0.3F, 2.9F, -0.2F, 0.3F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
