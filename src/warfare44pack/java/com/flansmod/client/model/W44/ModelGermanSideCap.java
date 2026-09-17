
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanSideCap extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 128;

   public ModelGermanSideCap() {
      headModel = new ModelRendererTurbo[13];
      headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 105, 1, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      headModel[8] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      headModel[9] = new ModelRendererTurbo(this, 65, 1, textureX, textureY);
      headModel[10] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      headModel[11] = new ModelRendererTurbo(this, 129, 1, textureX, textureY);
      headModel[12] = new ModelRendererTurbo(this, 153, 1, textureX, textureY);
      headModel[0].addShapeBox(-2.25F, -9.5F, -4.5F, 2, 2, 9, 0.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -1.0F, 0.0F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-2.5F, -7.5F, 3.5F, 2, 1, 1, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, -0.25F, -1.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-2.5F, -7.5F, -4.5F, 2, 1, 1, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.8F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F, -0.8F, -0.6F, 0.0F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-0.5F, -10.5F, -4.0F, 2, 1, 8, 0.0F, -0.5F, 0.25F, -0.75F, -0.5F, 0.25F, -0.75F, -0.5F, 0.25F, -0.75F, -0.5F, 0.25F, -0.75F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(1.25F, -9.5F, -4.5F, 2, 2, 9, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-1.0F, -9.5F, -4.5F, 5, 4, 9, 0.0F, -0.25F, 0.0F, -0.5F, -2.25F, 0.0F, -0.5F, -2.25F, 0.0F, -0.5F, -0.25F, 0.0F, -0.5F, -0.25F, -2.0F, 0.0F, -2.25F, -2.0F, 0.0F, -2.25F, -2.0F, 0.0F, -0.25F, -2.0F, 0.0F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(1.5F, -10.5F, -4.0F, 1, 1, 8, 0.0F, 0.4F, 0.0F, -0.62F, -0.5F, 0.0F, -2.0F, -0.5F, 0.0F, -2.0F, 0.4F, 0.0F, -0.62F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -1.1F, 0.0F, 0.0F, -1.1F, 0.0F, 0.0F, -0.1F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[7].addShapeBox(1.5F, -7.5F, -4.5F, 2, 1, 1, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, -0.6F, 0.0F, -0.8F, -0.6F, 0.0F, -0.8F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F);
      headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[8].addShapeBox(-1.0F, -7.5F, -4.5F, 3, 1, 1, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F, -0.25F, -0.6F, 0.0F);
      headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[9].addShapeBox(1.5F, -7.5F, 3.5F, 2, 1, 1, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F);
      headModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[10].addShapeBox(-0.5F, -7.5F, 3.5F, 2, 1, 1, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F);
      headModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[11].addShapeBox(-1.5F, -10.5F, -4.0F, 1, 1, 8, 0.0F, -0.5F, 0.0F, -2.0F, 0.4F, 0.0F, -0.62F, 0.4F, 0.0F, -0.62F, -0.5F, 0.0F, -2.0F, 0.0F, 0.0F, -1.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -1.1F);
      headModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[12].addShapeBox(-11.25F, -10.4F, -3.8F, 150, 68, 1, 0.0F, -11.0F, 0.0F, -0.3F, -137.5F, 0.0F, -0.3F, -137.5F, 0.0F, 0.0F, -11.0F, 0.0F, 0.0F, -11.0F, -67.25F, 0.1F, -137.5F, -67.25F, 0.1F, -137.5F, -67.25F, 0.0F, -11.0F, -67.25F, 0.0F);
      headModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
