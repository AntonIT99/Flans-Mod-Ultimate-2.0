
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWH40K_Misc_TitanBolt_1 extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] titanboltModel = new ModelRendererTurbo[2];

   public ModelWH40K_Misc_TitanBolt_1() {
      titanboltModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      titanboltModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      titanboltModel[0].addBox(0.0F, 0.0F, 0.0F, 2, 4, 2, 0.0F);
      titanboltModel[0].setRotationPoint(-1.0F, 0.0F, -1.0F);
      titanboltModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 2, 2, 2, 0.0F, -0.75F, 5);
      titanboltModel[1].setRotationPoint(-1.0F, 4.0F, -1.0F);
   }
}
