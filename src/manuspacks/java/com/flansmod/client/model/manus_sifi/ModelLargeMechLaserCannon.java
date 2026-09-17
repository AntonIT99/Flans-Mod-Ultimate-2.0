
package com.flansmod.client.model.manus_sifi;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelLargeMechLaserCannon extends ModelMG {
   int textureX = 512;
   int textureY = 256;

   public ModelLargeMechLaserCannon() {
      bipodModel = new ModelRendererTurbo[0];
      gunModel = new ModelRendererTurbo[23];
      gunModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 55, 0, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 72, 0, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 87, 0, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 105, 0, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 120, 0, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 129, 8, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 276, 8, textureX, textureY);
      gunModel[8] = new ModelRendererTurbo(this, 181, 0, textureX, textureY);
      gunModel[9] = new ModelRendererTurbo(this, 0, 33, textureX, textureY);
      gunModel[10] = new ModelRendererTurbo(this, 399, 0, textureX, textureY);
      gunModel[11] = new ModelRendererTurbo(this, 0, 29, textureX, textureY);
      gunModel[12] = new ModelRendererTurbo(this, 0, 100, textureX, textureY);
      gunModel[13] = new ModelRendererTurbo(this, 114, 108, textureX, textureY);
      gunModel[14] = new ModelRendererTurbo(this, 229, 100, textureX, textureY);
      gunModel[15] = new ModelRendererTurbo(this, 0, 100, textureX, textureY);
      gunModel[16] = new ModelRendererTurbo(this, 0, 118, textureX, textureY);
      gunModel[17] = new ModelRendererTurbo(this, 0, 130, textureX, textureY);
      gunModel[18] = new ModelRendererTurbo(this, 10, 130, textureX, textureY);
      gunModel[19] = new ModelRendererTurbo(this, 10, 140, textureX, textureY);
      gunModel[20] = new ModelRendererTurbo(this, 37, 130, textureX, textureY);
      gunModel[21] = new ModelRendererTurbo(this, 35, 100, textureX, textureY);
      gunModel[22] = new ModelRendererTurbo(this, 35, 110, textureX, textureY);
      gunModel[0].addBox(0.0F, 0.0F, 0.0F, 20, 20, 7, 0.0F);
      gunModel[0].setRotationPoint(-10.0F, -31.0F, -27.0F);
      gunModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 1, 12, 7, 0.0F, 0.5F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F);
      gunModel[1].setRotationPoint(-11.0F, -27.0F, -33.0F);
      gunModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 1, 19, 6, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, -7.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F);
      gunModel[2].setRotationPoint(-11.0F, -27.0F, -26.0F);
      gunModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 1, 12, 7, 0.0F, 0.0F, -3.0F, 0.0F, 0.5F, -3.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.5F, -3.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[3].setRotationPoint(10.0F, -27.0F, -33.0F);
      gunModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 1, 19, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -7.0F, 0.0F, 0.5F, -7.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[4].setRotationPoint(10.0F, -27.0F, -26.0F);
      gunModel[5].addBox(0.0F, 0.0F, 0.0F, 24, 21, 2, 0.0F);
      gunModel[5].setRotationPoint(-12.0F, -31.0F, -20.0F);
      gunModel[6].addBox(0.0F, 0.0F, 0.0F, 2, 27, 45, 0.0F);
      gunModel[6].setRotationPoint(12.0F, -31.0F, -20.0F);
      gunModel[7].addBox(0.0F, 0.0F, 0.0F, 2, 27, 45, 0.0F);
      gunModel[7].setRotationPoint(-14.0F, -31.0F, -20.0F);
      gunModel[8].addBox(0.0F, 0.0F, 0.0F, 24, 6, 45, 0.0F);
      gunModel[8].setRotationPoint(-12.0F, -10.0F, -20.0F);
      gunModel[9].addTrapezoid(0.0F, 0.0F, 0.0F, 18, 16, 49, 0.0F, -5.0F, 5);
      gunModel[9].setRotationPoint(-9.0F, -12.0F, -25.0F);
      gunModel[10].addFlexBox(0.0F, 0.0F, 0.0F, 7, 12, 49, 0.0F, -2.0F, -2.0F, -5.0F, -3.0F, 5);
      gunModel[10].setRotationPoint(-3.5F, -10.0F, -24.5F);
      gunModel[11].addBox(0.0F, 0.0F, 0.0F, 13, 13, 7, 0.0F);
      gunModel[11].setRotationPoint(-6.5F, -27.5F, -18.0F);
      gunModel[12].addShapeBox(0.0F, 0.0F, 0.0F, 24, 6, 64, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[12].setRotationPoint(-12.0F, -34.0F, -18.0F);
      gunModel[13].addBox(0.0F, 0.0F, 0.0F, 24, 15, 64, 0.0F);
      gunModel[13].setRotationPoint(-12.0F, -28.0F, -18.0F);
      gunModel[14].addShapeBox(0.0F, 0.0F, 0.0F, 24, 6, 64, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F, -11.9999F, 0.0F, 0.0F);
      gunModel[14].setRotationPoint(-12.0F, -13.0F, -18.0F);
      gunModel[15].addBox(0.0F, 0.0F, 0.0F, 8, 8, 8, 0.0F);
      gunModel[15].setRotationPoint(-4.0F, -28.0F, 45.0F);
      gunModel[16].addBox(0.0F, 0.0F, 0.0F, 4, 4, 6, 0.0F);
      gunModel[16].setRotationPoint(-2.0F, -17.0F, 45.0F);
      gunModel[17].addBox(0.0F, 0.0F, 0.0F, 1, 11, 3, 0.0F);
      gunModel[17].setRotationPoint(9.0F, -26.0F, 45.0F);
      gunModel[18].addShapeBox(0.0F, 0.0F, 0.0F, 10, 6, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F);
      gunModel[18].setRotationPoint(0.0F, -32.0F, 45.0F);
      gunModel[19].addShapeBox(0.0F, 0.0F, 0.0F, 10, 6, 3, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.9999F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, 0.0F, 0.0F);
      gunModel[19].setRotationPoint(-10.0F, -32.0F, 45.0F);
      gunModel[20].addBox(0.0F, 0.0F, 0.0F, 1, 11, 3, 0.0F);
      gunModel[20].setRotationPoint(-10.0F, -26.0F, 45.0F);
      gunModel[21].addShapeBox(0.0F, 0.0F, 0.0F, 10, 6, 3, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.9999F, 0.0F);
      gunModel[21].setRotationPoint(-10.0F, -15.0F, 45.0F);
      gunModel[22].addShapeBox(0.0F, 0.0F, 0.0F, 10, 6, 3, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, -5.9999F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[22].setRotationPoint(0.0F, -15.0F, 45.0F);
      ammoModel = new ModelRendererTurbo[0];
      flipAll();
   }
}
