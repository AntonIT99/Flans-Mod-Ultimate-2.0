
package com.flansmod.client.model.manus_sifi;

import com.flansmod.client.model.ModelAAGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelImperialThrone extends ModelAAGun {
   int textureX = 128;
   int textureY = 128;

   public ModelImperialThrone() {
      baseModel = new ModelRendererTurbo[4];
      baseModel[0] = new ModelRendererTurbo(this, 0, 99, textureX, textureY);
      baseModel[1] = new ModelRendererTurbo(this, 0, 82, textureX, textureY);
      baseModel[2] = new ModelRendererTurbo(this, 58, 90, textureX, textureY);
      baseModel[3] = new ModelRendererTurbo(this, 0, 99, textureX, textureY);
      baseModel[0].addBox(0.0F, 0.0F, 0.0F, 26, 1, 26, 0.0F);
      baseModel[0].setRotationPoint(-13.0F, -1.0F, -13.0F);
      baseModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 14, 2, 14, 0.0F, -3.0F, 4);
      baseModel[1].setRotationPoint(-7.0F, -3.0F, -7.0F);
      baseModel[2].addTrapezoid(0.0F, 0.0F, 0.0F, 7, 1, 7, 0.0F, -1.5F, 4);
      baseModel[2].setRotationPoint(-3.5F, -4.0F, -3.5F);
      baseModel[3].addBox(0.0F, 0.0F, 0.0F, 4, 4, 4, 0.0F);
      baseModel[3].setRotationPoint(-2.0F, -8.0F, -2.0F);
      seatModel = new ModelRendererTurbo[7];
      seatModel[0] = new ModelRendererTurbo(this, 0, 54, textureX, textureY);
      seatModel[1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      seatModel[2] = new ModelRendererTurbo(this, 79, 0, textureX, textureY);
      seatModel[3] = new ModelRendererTurbo(this, 79, 18, textureX, textureY);
      seatModel[4] = new ModelRendererTurbo(this, 91, 59, textureX, textureY);
      seatModel[5] = new ModelRendererTurbo(this, 41, 0, textureX, textureY);
      seatModel[6] = new ModelRendererTurbo(this, 79, 36, textureX, textureY);
      seatModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 21, 3, 24, 0.0F, 0.0F, 0.0F, 1.5F, 0.0F, 0.0F, 1.5F, 0.0F, 0.0F, 1.5F, 0.0F, 0.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      seatModel[0].setRotationPoint(-8.0F, -11.0F, -12.0F);
      seatModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 6, 29, 24, 0.0F, 0.0F, 0.0F, -3.5F, 0.0F, 0.0F, -3.5F, 0.0F, 0.0F, -3.5F, 0.0F, 0.0F, -3.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      seatModel[1].setRotationPoint(-8.0F, -40.0F, -12.0F);
      seatModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 12, 8, 9, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -1.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      seatModel[2].setRotationPoint(1.0F, -19.0F, -15.0F);
      seatModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 12, 8, 9, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, -1.0F, -5.0F, 0.0F, -3.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F);
      seatModel[3].setRotationPoint(1.0F, -19.0F, 6.0F);
      seatModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 9, 8, 9, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      seatModel[4].setRotationPoint(-8.0F, -19.0F, -15.0F);
      seatModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 9, 8, 9, 0.0F, 0.0F, 0.0F, -5.0F, 0.0F, 0.0F, -5.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F);
      seatModel[5].setRotationPoint(-8.0F, -19.0F, 6.0F);
      seatModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 7, 4, 17, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4827586F, 0.0F, 0.0F, 0.4827586F, 0.0F, 0.0F, 0.4827586F, 0.0F, 0.0F, 0.4827586F);
      seatModel[6].setRotationPoint(-2.0F, -40.0F, -8.5F);
      gunModel = new ModelRendererTurbo[0];
      barrelModel = new ModelRendererTurbo[0][0];
      ammoModel = new ModelRendererTurbo[0][0];
      barrelX = 0;
      barrelY = 0;
      barrelZ = 0;
      flipAll();
   }
}
