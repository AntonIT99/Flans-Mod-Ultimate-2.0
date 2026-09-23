
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWH40K_Misc_TitanLaser_1 extends ModelBase {
   int textureX = 32;
   int textureY = 64;
   public ModelRendererTurbo[] titanlaserModel = new ModelRendererTurbo[2];

   public ModelWH40K_Misc_TitanLaser_1() {
      titanlaserModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      titanlaserModel[1] = new ModelRendererTurbo(this, 16, 0, textureX, textureY);
      titanlaserModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 1, 50, 7, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F);
      titanlaserModel[0].setRotationPoint(-0.5F, 0.0F, -3.5F);
      titanlaserModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 7, 50, 1, 0.0F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F);
      titanlaserModel[1].setRotationPoint(-3.5F, 0.0F, -0.5F);
   }
}
