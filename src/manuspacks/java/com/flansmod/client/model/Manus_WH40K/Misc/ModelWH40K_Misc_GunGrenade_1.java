
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWH40K_Misc_GunGrenade_1 extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] gungrenadeModel = new ModelRendererTurbo[1];

   public ModelWH40K_Misc_GunGrenade_1() {
      gungrenadeModel[0] = new ModelRendererTurbo(this, 9, 1, textureX, textureY);
      gungrenadeModel[0].addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1, 0.0F);
      gungrenadeModel[0].setRotationPoint(0.0F, -2.0F, 0.0F);
   }
}
