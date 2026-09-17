
package com.flansmod.client.model.Manus_WW2.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWW2_Misc_BulletSmall_1 extends ModelBase {
   int textureX = 64;
   int textureY = 64;
   public ModelRendererTurbo[] bulletsmallModel = new ModelRendererTurbo[1];

   public ModelWW2_Misc_BulletSmall_1() {
      bulletsmallModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      bulletsmallModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, -0.5F, 3);
      bulletsmallModel[0].setRotationPoint(-4.0F, -0.5F, -0.5F);
   }
}
