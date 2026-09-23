package com.flansmod.client.model.Manus_WH40K.Armor;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWH40K_Armor_KriegLegs_1 extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelWH40K_Armor_KriegLegs_1() {
      bodyModel = new ModelRendererTurbo[1];
      bodyModel[0] = new ModelRendererTurbo(this, 401, 73, textureX, textureY);
      bodyModel[0].addShapeBox(-16.0F, 40.0F, -8.0F, 32, 8, 16, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      leftLegModel = new ModelRendererTurbo[3];
      leftLegModel[0] = new ModelRendererTurbo(this, 425, 105, textureX, textureY);
      leftLegModel[1] = new ModelRendererTurbo(this, 329, 121, textureX, textureY);
      leftLegModel[2] = new ModelRendererTurbo(this, 401, 129, textureX, textureY);
      leftLegModel[0].addShapeBox(-8.0F, 28.0F, -8.0F, 16, 3, 16, 0.0F, 0.02F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      leftLegModel[1].addShapeBox(-8.0F, 18.0F, -8.0F, 16, 10, 16, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F);
      leftLegModel[2].addShapeBox(-8.0F, 0.0F, -8.0F, 16, 18, 16, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      rightLegModel = new ModelRendererTurbo[3];
      rightLegModel[0] = new ModelRendererTurbo(this, 1, 145, textureX, textureY);
      rightLegModel[1] = new ModelRendererTurbo(this, 73, 145, textureX, textureY);
      rightLegModel[2] = new ModelRendererTurbo(this, 145, 153, textureX, textureY);
      rightLegModel[0].addShapeBox(-8.0F, 0.0F, -8.0F, 16, 18, 16, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      rightLegModel[1].addShapeBox(-8.0F, 18.0F, -8.0F, 16, 10, 16, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.52F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F);
      rightLegModel[2].addShapeBox(-8.0F, 28.0F, -8.0F, 16, 3, 16, 0.0F, 0.52F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F, 0.02F, 0.0F, 0.52F, 0.52F, 0.0F, 0.52F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
   }
}
