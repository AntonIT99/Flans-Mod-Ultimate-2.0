
package com.flansmod.client.model.Manus_WW2.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWW2_Misc_BombUS_1 extends ModelBase {
   int textureX = 128;
   int textureY = 32;
   public ModelRendererTurbo[] bombusModel = new ModelRendererTurbo[16];

   public ModelWW2_Misc_BombUS_1() {
      bombusModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      bombusModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      bombusModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      bombusModel[3] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      bombusModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY);
      bombusModel[5] = new ModelRendererTurbo(this, 65, 1, textureX, textureY);
      bombusModel[6] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      bombusModel[7] = new ModelRendererTurbo(this, 89, 1, textureX, textureY);
      bombusModel[8] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      bombusModel[9] = new ModelRendererTurbo(this, 113, 1, textureX, textureY);
      bombusModel[10] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      bombusModel[11] = new ModelRendererTurbo(this, 49, 9, textureX, textureY);
      bombusModel[12] = new ModelRendererTurbo(this, 65, 9, textureX, textureY);
      bombusModel[13] = new ModelRendererTurbo(this, 81, 9, textureX, textureY);
      bombusModel[14] = new ModelRendererTurbo(this, 97, 9, textureX, textureY);
      bombusModel[15] = new ModelRendererTurbo(this, 105, 9, textureX, textureY);
      bombusModel[0].addTrapezoid(-2.5F, -20.0F, -2.5F, 5, 10, 5, 0.0F, -2.0F, 4);
      bombusModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[1].addTrapezoid(-2.5F, 5.0F, -2.5F, 5, 5, 5, 0.0F, -1.5F, 5);
      bombusModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[2].addShapeBox(0.0F, -19.0F, -0.5F, 3, 3, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, 0.0F, -0.4F);
      bombusModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[3].addShapeBox(1.0F, -22.0F, -0.5F, 2, 3, 1, 0.0F, -0.5F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F);
      bombusModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[4].addShapeBox(-3.0F, -19.0F, -0.5F, 3, 3, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -2.0F, -0.4F);
      bombusModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[5].addShapeBox(-3.0F, -22.0F, -0.5F, 2, 3, 1, 0.0F, 0.0F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, -0.5F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F);
      bombusModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[6].addShapeBox(-0.5F, -19.0F, 0.0F, 1, 3, 3, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -2.0F, 0.0F, -0.4F, -2.0F, 0.0F);
      bombusModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[7].addShapeBox(-0.5F, -22.0F, 1.0F, 1, 3, 2, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F);
      bombusModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[8].addShapeBox(-0.5F, -19.0F, -3.0F, 1, 3, 3, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -2.0F, 0.0F, -0.4F, -2.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F);
      bombusModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[9].addShapeBox(-0.5F, -22.0F, -3.0F, 1, 3, 2, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F);
      bombusModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[10].addBox(-0.5F, 9.5F, -0.5F, 1, 1, 1, 0.0F);
      bombusModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[11].addShapeBox(-2.0F, -22.0F, 0.0F, 2, 2, 2, 0.0F, -0.5F, 0.0F, -0.1F, -0.1F, 0.0F, -1.5F, -0.1F, 0.0F, -0.3F, -0.3F, 0.0F, -1.9F, -0.5F, 0.0F, -0.1F, -0.1F, 0.0F, -1.5F, -0.1F, 0.0F, -0.3F, -0.3F, 0.0F, -1.9F);
      bombusModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[12].addShapeBox(-2.0F, -22.0F, -2.0F, 2, 2, 2, 0.0F, -0.3F, 0.0F, -1.9F, -0.1F, 0.0F, -0.3F, -0.1F, 0.0F, -1.5F, -0.5F, 0.0F, -0.1F, -0.3F, 0.0F, -1.9F, -0.1F, 0.0F, -0.3F, -0.1F, 0.0F, -1.5F, -0.5F, 0.0F, -0.1F);
      bombusModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[13].addShapeBox(0.0F, -22.0F, -2.0F, 2, 2, 2, 0.0F, -0.1F, 0.0F, -0.3F, -0.3F, 0.0F, -1.9F, -0.5F, 0.0F, -0.1F, -0.1F, 0.0F, -1.5F, -0.1F, 0.0F, -0.3F, -0.3F, 0.0F, -1.9F, -0.5F, 0.0F, -0.1F, -0.1F, 0.0F, -1.5F);
      bombusModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[14].addShapeBox(0.0F, -22.0F, 0.0F, 2, 2, 2, 0.0F, -0.1F, 0.0F, -1.5F, -0.5F, 0.0F, -0.1F, -0.3F, 0.0F, -1.9F, -0.1F, 0.0F, -0.3F, -0.1F, 0.0F, -1.5F, -0.5F, 0.0F, -0.1F, -0.3F, 0.0F, -1.9F, -0.1F, 0.0F, -0.3F);
      bombusModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      bombusModel[15].addBox(-2.5F, -10.0F, -2.5F, 5, 15, 5, 0.0F);
      bombusModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
