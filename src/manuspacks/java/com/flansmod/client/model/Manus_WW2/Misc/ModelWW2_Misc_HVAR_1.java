
package com.flansmod.client.model.Manus_WW2.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWW2_Misc_HVAR_1 extends ModelBase {
   int textureX = 64;
   int textureY = 32;
   public ModelRendererTurbo[] hvarModel = new ModelRendererTurbo[4];

   public ModelWW2_Misc_HVAR_1() {
      hvarModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      hvarModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      hvarModel[2] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      hvarModel[3] = new ModelRendererTurbo(this, 17, 9, textureX, textureY);
      hvarModel[0].addShapeBox(-1.0F, -10.0F, -1.0F, 2, 20, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      hvarModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      hvarModel[1].addTrapezoid(-1.0F, 10.0F, -1.0F, 2, 3, 2, 0.0F, -1.0F, 5);
      hvarModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      hvarModel[2].addShapeBox(-2.0F, -10.0F, -2.0F, 4, 2, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F);
      hvarModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      hvarModel[3].addShapeBox(-2.0F, -10.0F, -2.0F, 4, 2, 4, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.9F, 0.0F, 0.0F, 0.0F);
      hvarModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
