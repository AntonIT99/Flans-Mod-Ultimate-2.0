
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelBritishBrodieHelmet extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 128;

   public ModelBritishBrodieHelmet() {
      headModel = new ModelRendererTurbo[13];
      headModel[0] = new ModelRendererTurbo(this, 49, 65, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 89, 65, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 33, 81, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 57, 81, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 73, 81, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 97, 81, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 81, 65, textureX, textureY);
      headModel[8] = new ModelRendererTurbo(this, 89, 65, textureX, textureY);
      headModel[9] = new ModelRendererTurbo(this, 121, 65, textureX, textureY);
      headModel[10] = new ModelRendererTurbo(this, 1, 73, textureX, textureY);
      headModel[11] = new ModelRendererTurbo(this, 1, 89, textureX, textureY);
      headModel[12] = new ModelRendererTurbo(this, 81, 89, textureX, textureY);
      headModel[0].addShapeBox(-4.5F, -8.0F, -4.5F, 9, 2, 9, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F, -0.1F, -0.5F, -0.1F);
      headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[1].addShapeBox(-4.5F, -9.0F, -4.5F, 9, 1, 9, 0.0F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -1.5F, 0.0F, -1.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[2].addShapeBox(-4.5F, -6.5F, -4.5F, 9, 2, 9, 0.0F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F);
      headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[3].addShapeBox(-6.0F, -5.0F, -4.5F, 2, 1, 9, 0.0F, -1.4F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, -1.4F, 0.0F, 0.1F, 0.0F, -0.25F, 0.1F, 0.0F, -0.5F, 0.1F, 0.0F, -0.5F, 0.1F, 0.0F, -0.25F, 0.1F);
      headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[4].addShapeBox(4.0F, -5.0F, -4.5F, 2, 1, 9, 0.0F, 0.0F, 0.0F, 0.1F, -1.4F, 0.0F, 0.1F, -1.4F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -0.5F, 0.1F, 0.0F, -0.25F, 0.1F, 0.0F, -0.25F, 0.1F, 0.0F, -0.5F, 0.1F);
      headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-4.5F, -5.0F, -6.0F, 9, 1, 2, 0.0F, 0.1F, 0.0F, -1.4F, 0.1F, 0.0F, -1.4F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, -0.25F, 0.0F, 0.1F, -0.25F, 0.0F, 0.1F, -0.5F, 0.0F, 0.1F, -0.5F, 0.0F);
      headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[6].addShapeBox(-4.5F, -5.0F, 4.0F, 9, 1, 2, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -1.4F, 0.1F, 0.0F, -1.4F, 0.1F, -0.5F, 0.0F, 0.1F, -0.5F, 0.0F, 0.1F, -0.25F, 0.0F, 0.1F, -0.25F, 0.0F);
      headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[7].addShapeBox(-6.0F, -5.0F, -6.0F, 1, 1, 1, 0.0F, 0.0F, -0.75F, -1.4F, 0.4F, -0.75F, 0.0F, 0.4F, 0.0F, 0.4F, 0.0F, -0.75F, 0.4F, 0.0F, -0.25F, -1.4F, 0.4F, -0.25F, 0.0F, 0.4F, -0.425F, 0.4F, 0.0F, -0.25F, 0.4F);
      headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[8].addShapeBox(5.0F, -5.0F, -6.0F, 1, 1, 1, 0.0F, 0.4F, -0.75F, 0.0F, 0.0F, -0.75F, -1.4F, 0.0F, -0.75F, 0.4F, 0.4F, 0.0F, 0.4F, 0.4F, -0.25F, 0.0F, 0.0F, -0.25F, -1.4F, 0.0F, -0.25F, 0.4F, 0.4F, -0.425F, 0.4F);
      headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[9].addShapeBox(5.0F, -5.0F, 5.0F, 1, 1, 1, 0.0F, 0.4F, 0.0F, 0.4F, 0.0F, -0.75F, 0.4F, 0.0F, -0.75F, -1.4F, 0.4F, -0.75F, 0.0F, 0.4F, -0.425F, 0.4F, 0.0F, -0.25F, 0.4F, 0.0F, -0.25F, -1.4F, 0.4F, -0.25F, 0.0F);
      headModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[10].addShapeBox(-6.0F, -5.0F, 5.0F, 1, 1, 1, 0.0F, 0.0F, -0.75F, 0.4F, 0.4F, 0.0F, 0.4F, 0.4F, -0.75F, 0.0F, 0.0F, -0.75F, -1.4F, 0.0F, -0.25F, 0.4F, 0.4F, -0.425F, 0.4F, 0.4F, -0.25F, 0.0F, 0.0F, -0.25F, -1.4F);
      headModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[11].addShapeBox(-4.5F, -5.0F, -4.5F, 9, 5, 4, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      headModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      headModel[12].addShapeBox(-4.5F, 0.0F, -4.0F, 9, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F, -0.6F, -0.8F, 0.0F);
      headModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
