
package com.flansmod.client.model.Manus_WW2.MG;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWW2_MG_BordkanoneBK37_1 extends ModelMG {
   int textureX = 128;
   int textureY = 64;

   public ModelWW2_MG_BordkanoneBK37_1() {
      bipodModel = new ModelRendererTurbo[0];
      gunModel = new ModelRendererTurbo[5];
      gunModel[0] = new ModelRendererTurbo(this, 90, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 52, 0, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 12, 0, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 24, 8, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 49, 0, textureX, textureY);
      gunModel[0].addTrapezoid(-4.0F, -4.0F, -20.0F, 8, 8, 11, 0.0F, -3.5F, 0);
      gunModel[0].setRotationPoint(0.0F, -3.0F, 0.0F);
      gunModel[1].addBox(-4.0F, -4.0F, -9.0F, 8, 8, 21, 0.0F);
      gunModel[1].setRotationPoint(0.0F, -3.0F, 0.0F);
      gunModel[2].addTrapezoid(-4.0F, -4.0F, 12.0F, 8, 8, 8, 0.0F, -3.0F, 1);
      gunModel[2].setRotationPoint(0.0F, -3.0F, 0.0F);
      gunModel[3].addBox(-0.5F, -0.5F, 20.0F, 1, 1, 22, 0.0F);
      gunModel[3].setRotationPoint(0.0F, -3.0F, 0.0F);
      gunModel[4].addTrapezoid(-1.0F, -1.0F, 42.0F, 2, 2, 10, 0.0F, -0.2F, 0);
      gunModel[4].setRotationPoint(0.0F, -3.0F, 0.0F);
      ammoModel = new ModelRendererTurbo[2];
      ammoModel[0] = new ModelRendererTurbo(this, 0, 32, textureX, textureY);
      ammoModel[1] = new ModelRendererTurbo(this, 0, 41, textureX, textureY);
      ammoModel[0].addBox(-15.0F, -1.0F, -2.0F, 11, 2, 6, 0.0F);
      ammoModel[0].setRotationPoint(0.0F, -3.0F, 0.0F);
      ammoModel[1].addBox(4.0F, -1.0F, -7.0F, 8, 2, 11, 0.0F);
      ammoModel[1].setRotationPoint(0.0F, -3.0F, 0.0F);
      flipAll();
   }
}
