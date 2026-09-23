
package com.flansmod.client.model.Manus_WH40K.Armor;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Armor_CatachanBandana_1 extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Armor_CatachanBandana_1() {
      headModel = new ModelRendererTurbo[9];
      headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 441, 1, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 121, 89, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 161, 105, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 353, 105, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 457, 105, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 497, 41, textureX, textureY);
      headModel[8] = new ModelRendererTurbo(this, 113, 49, textureX, textureY);
      headModel[0].addBox(-17.0F, -24.0F, -17.0F, 1, 6, 17, 0.0F);
      headModel[1].addShapeBox(-16.0F, -23.0F, 16.0F, 32, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[2].addBox(-2.5F, -23.5F, 16.0F, 5, 5, 2, 0.0F);
      headModel[3].addBox(16.0F, -24.0F, -17.0F, 1, 6, 17, 0.0F);
      headModel[4].addBox(-16.0F, -24.0F, -17.0F, 32, 6, 1, 0.0F);
      headModel[5].addShapeBox(-17.0F, -24.0F, 0.0F, 1, 6, 17, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      headModel[6].addShapeBox(16.0F, -24.0F, 0.0F, 1, 6, 17, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      headModel[7].addShapeBox(-2.0F, -18.5F, 16.0F, 4, 10, 1, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -1.5F, -1.0F, 0.0F, 2.0F, 0.0F, 0.0F, 2.0F, 0.0F, -0.5F, -1.5F, -1.0F, -0.5F);
      headModel[8].addShapeBox(-2.0F, -18.5F, 16.0F, 4, 8, 1, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 2.0F, 0.0F, -0.5F, -1.5F, -1.0F, -0.5F, -1.5F, -1.0F, 0.0F, 2.0F, 0.0F, 0.0F);
   }
}
