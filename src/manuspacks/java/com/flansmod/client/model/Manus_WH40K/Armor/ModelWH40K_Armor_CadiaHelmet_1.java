
package com.flansmod.client.model.Manus_WH40K.Armor;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Armor_CadiaHelmet_1 extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Armor_CadiaHelmet_1() {
      headModel = new ModelRendererTurbo[14];
      headModel[0] = new ModelRendererTurbo(this, 0, 476, textureX, textureY);
      headModel[1] = new ModelRendererTurbo(this, 98, 498, textureX, textureY);
      headModel[2] = new ModelRendererTurbo(this, 0, 490, textureX, textureY);
      headModel[3] = new ModelRendererTurbo(this, 0, 470, textureX, textureY);
      headModel[4] = new ModelRendererTurbo(this, 0, 490, textureX, textureY);
      headModel[5] = new ModelRendererTurbo(this, 210, 0, textureX, textureY);
      headModel[6] = new ModelRendererTurbo(this, 0, 425, textureX, textureY);
      headModel[7] = new ModelRendererTurbo(this, 140, 468, textureX, textureY);
      headModel[8] = new ModelRendererTurbo(this, 110, 419, textureX, textureY);
      headModel[9] = new ModelRendererTurbo(this, 98, 470, textureX, textureY);
      headModel[10] = new ModelRendererTurbo(this, 110, 419, textureX, textureY);
      headModel[11] = new ModelRendererTurbo(this, 170, 438, textureX, textureY);
      headModel[12] = new ModelRendererTurbo(this, 210, 451, textureX, textureY);
      headModel[13] = new ModelRendererTurbo(this, 170, 438, textureX, textureY);
      headModel[0].addTrapezoid(-16.0F, -35.0F, -16.0F, 32, 3, 32, 0.0F, -8.0F, 4);
      headModel[1].addBox(-8.0F, -2.0F, -18.0F, 16, 4, 4, 0.0F);
      headModel[2].addShapeBox(-17.0F, -11.0F, -17.0F, 1, 12, 2, 0.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.0F, 14.0F, 0.0F, 0.0F, 14.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[3].addBox(-17.0F, -1.0F, -17.0F, 34, 2, 2, 0.0F);
      headModel[4].addShapeBox(16.0F, -11.0F, -17.0F, 1, 12, 2, 0.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.0F, 14.0F, 0.0F, 0.0F, 14.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[5].addShapeBox(-6.0F, -24.0F, -18.01F, 120, 33, 1, 0.0F, 0.0F, 0.0F, 0.0F, -108.0F, 0.0F, 0.0F, -108.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -29.7F, 0.0F, -108.0F, -29.7F, 0.0F, -108.0F, -29.7F, 0.0F, 0.0F, -29.7F, 0.0F);
      headModel[6].addTrapezoid(-18.0F, -32.0F, -18.0F, 36, 7, 36, 0.0F, -2.0F, 4);
      headModel[7].addBox(-18.0F, -25.0F, -18.0F, 36, 7, 36, 0.0F);
      headModel[8].addShapeBox(-18.0F, -18.0F, -8.0F, 3, 14, 26, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[9].addBox(-15.0F, -18.0F, 15.0F, 30, 14, 3, 0.0F);
      headModel[10].addShapeBox(15.0F, -18.0F, -8.0F, 3, 14, 26, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      headModel[11].addBox(-19.0F, -21.0F, -3.0F, 3, 12, 16, 0.0F);
      headModel[12].addBox(-8.0F, -21.0F, 16.0F, 16, 12, 3, 0.0F);
      headModel[13].addBox(16.0F, -21.0F, -3.0F, 3, 12, 16, 0.0F);
   }
}
