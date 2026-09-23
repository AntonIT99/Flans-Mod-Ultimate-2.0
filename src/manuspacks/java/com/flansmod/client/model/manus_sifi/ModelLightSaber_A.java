
package com.flansmod.client.model.manus_sifi;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelLightSaber_A extends ModelGun {
   int textureX = 512;
   int textureY = 512;

   public ModelLightSaber_A() {
      gunModel = new ModelRendererTurbo[20];
      gunModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 0, 325, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 38, 325, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 77, 325, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 0, 345, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 35, 345, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 70, 345, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 35, 345, textureX, textureY);
      gunModel[8] = new ModelRendererTurbo(this, 70, 345, textureX, textureY);
      gunModel[9] = new ModelRendererTurbo(this, 35, 345, textureX, textureY);
      gunModel[10] = new ModelRendererTurbo(this, 70, 345, textureX, textureY);
      gunModel[11] = new ModelRendererTurbo(this, 35, 345, textureX, textureY);
      gunModel[12] = new ModelRendererTurbo(this, 70, 345, textureX, textureY);
      gunModel[13] = new ModelRendererTurbo(this, 0, 365, textureX, textureY);
      gunModel[14] = new ModelRendererTurbo(this, 35, 365, textureX, textureY);
      gunModel[15] = new ModelRendererTurbo(this, 0, 395, textureX, textureY);
      gunModel[16] = new ModelRendererTurbo(this, 15, 395, textureX, textureY);
      gunModel[17] = new ModelRendererTurbo(this, 25, 395, textureX, textureY);
      gunModel[18] = new ModelRendererTurbo(this, 38, 395, textureX, textureY);
      gunModel[19] = new ModelRendererTurbo(this, 0, 409, textureX, textureY);
      gunModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 35, 208, 35, 0.0F, 0.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F, -30.0F, 0.0F, -30.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F, 0.0F, -30.0F, 0.0F, 0.0F, -30.0F, 0.0F, -30.0F, 0.0F, 0.0F, -30.0F);
      gunModel[0].setRotationPoint(-2.5F, -215.0F, -2.5F);
      gunModel[0].glow = true;
      gunModel[1].addBox(0.0F, 0.0F, 0.0F, 9, 4, 9, 0.0F);
      gunModel[1].setRotationPoint(-4.5F, -7.0F, -4.5F);
      gunModel[2].addBox(0.0F, 0.0F, 0.0F, 9, 2, 9, 0.0F);
      gunModel[2].setRotationPoint(-4.5F, -3.0F, -4.5F);
      gunModel[3].addBox(0.0F, 0.0F, 0.0F, 7, 12, 7, 0.0F);
      gunModel[3].setRotationPoint(-3.5F, -1.0F, -3.5F);
      gunModel[4].addBox(0.0F, 0.0F, 0.0F, 8, 9, 8, 0.0F);
      gunModel[4].setRotationPoint(-4.0F, 11.0F, -4.0F);
      gunModel[5].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 4);
      gunModel[5].setRotationPoint(-4.0F, 20.0F, -4.0F);
      gunModel[6].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 5);
      gunModel[6].setRotationPoint(-4.0F, 21.0F, -4.0F);
      gunModel[7].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 4);
      gunModel[7].setRotationPoint(-4.0F, 22.0F, -4.0F);
      gunModel[8].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 5);
      gunModel[8].setRotationPoint(-4.0F, 23.0F, -4.0F);
      gunModel[9].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 4);
      gunModel[9].setRotationPoint(-4.0F, 24.0F, -4.0F);
      gunModel[10].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 5);
      gunModel[10].setRotationPoint(-4.0F, 25.0F, -4.0F);
      gunModel[11].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 4);
      gunModel[11].setRotationPoint(-4.0F, 26.0F, -4.0F);
      gunModel[12].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -0.8F, 5);
      gunModel[12].setRotationPoint(-4.0F, 27.0F, -4.0F);
      gunModel[13].addBox(0.0F, 0.0F, 0.0F, 8, 18, 8, 0.0F);
      gunModel[13].setRotationPoint(-4.0F, 28.0F, -4.0F);
      gunModel[14].addTrapezoid(0.0F, 0.0F, 0.0F, 8, 1, 8, 0.0F, -1.0F, 5);
      gunModel[14].setRotationPoint(-4.0F, 46.0F, -4.0F);
      gunModel[15].addBox(0.0F, 0.0F, 0.0F, 4, 8, 2, 0.0F);
      gunModel[15].setRotationPoint(-2.0F, 37.0F, -6.0F);
      gunModel[16].addBox(0.0F, 0.0F, 0.0F, 1, 8, 3, 0.0F);
      gunModel[16].setRotationPoint(3.5F, 1.0F, -1.5F);
      gunModel[17].addBox(0.0F, 0.0F, 0.0F, 3, 3, 2, 0.0F);
      gunModel[17].setRotationPoint(-1.5F, 1.0F, -5.5F);
      gunModel[18].addBox(0.0F, 0.0F, 0.0F, 3, 3, 2, 0.0F);
      gunModel[18].setRotationPoint(-1.5F, 5.0F, -5.5F);
      gunModel[19].addShapeBox(0.0F, 0.0F, 0.0F, 9, 7, 9, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[19].setRotationPoint(-4.5F, -14.0F, -4.5F);
      translateAll(12.0F, -20.0F, 0.0F);
      gunSlideDistance = 0.0F;
      animationType = EnumAnimationType.NONE;
      flipAll();
   }
}
