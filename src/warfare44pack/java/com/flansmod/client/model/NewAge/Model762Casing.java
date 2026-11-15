package com.flansmod.client.model.NewAge;

import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class Model762Casing extends ModelCasing {
   int textureX = 128;
   int textureY = 128;

   public Model762Casing() {
      casingModel = new ModelRendererTurbo[6];
      casingModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      casingModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      casingModel[2] = new ModelRendererTurbo(this, 65, 1, textureX, textureY);
      casingModel[3] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      casingModel[4] = new ModelRendererTurbo(this, 113, 1, textureX, textureY);
      casingModel[5] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      casingModel[0].addBox(0.0F, 0.0F, 0.0F, 12, 1, 3, 0.0F);
      casingModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      casingModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 12, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      casingModel[1].setRotationPoint(0.0F, -1.0F, 0.0F);
      casingModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 12, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      casingModel[2].setRotationPoint(0.0F, 1.0F, 0.0F);
      casingModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -0.7F, -1.0F, 0.0F, -0.7F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      casingModel[3].setRotationPoint(12.0F, -1.0F, 0.0F);
      casingModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      casingModel[4].setRotationPoint(12.0F, 0.0F, 0.0F);
      casingModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -0.7F, -1.0F, 0.0F, -0.7F, -1.0F, 0.0F, 0.0F, -1.0F);
      casingModel[5].setRotationPoint(12.0F, 1.0F, 0.0F);
      flipAll();
   }
}
