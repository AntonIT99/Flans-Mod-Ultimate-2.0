
package com.flansmod.client.model.manus_modern_warfare;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelATMissile extends ModelBase {
   int textureX = 128;
   int textureY = 64;
   public ModelRendererTurbo[] missleModel = new ModelRendererTurbo[8];

   public ModelATMissile() {
      missleModel[0] = new ModelRendererTurbo(this, 9, 0, textureX, textureY);
      missleModel[1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      missleModel[2] = new ModelRendererTurbo(this, 18, 0, textureX, textureY);
      missleModel[3] = new ModelRendererTurbo(this, 18, 10, textureX, textureY);
      missleModel[4] = new ModelRendererTurbo(this, 18, 15, textureX, textureY);
      missleModel[5] = new ModelRendererTurbo(this, 18, 24, textureX, textureY);
      missleModel[6] = new ModelRendererTurbo(this, 33, 0, textureX, textureY);
      missleModel[7] = new ModelRendererTurbo(this, 33, 11, textureX, textureY);
      missleModel[0].addTrapezoid(-1.0F, 28.0F, -1.0F, 2, 4, 2, 0.0F, -1.0F, 5);
      missleModel[1].addBox(-1.0F, 0.0F, -1.0F, 2, 28, 2, 0.0F);
      missleModel[2].addShapeBox(-0.5F, 20.0F, -3.0F, 1, 3, 6, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -2.9999F, -0.4F, 0.0F, -2.9999F, -0.4F, 0.0F, -2.9999F, -0.4F, 0.0F, -2.9999F);
      missleModel[3].addShapeBox(-3.0F, 20.0F, -0.5F, 6, 3, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -2.9999F, 0.0F, -0.4F, -2.9999F, 0.0F, -0.4F, -2.9999F, 0.0F, -0.4F, -2.9999F, 0.0F, -0.4F);
      missleModel[4].addShapeBox(-0.5F, 18.0F, -3.0F, 1, 2, 6, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F);
      missleModel[5].addShapeBox(-3.0F, 18.0F, -0.5F, 6, 2, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F);
      missleModel[6].addShapeBox(-0.5F, 0.0F, -3.0F, 1, 4, 6, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F);
      missleModel[7].addShapeBox(-3.0F, 0.0F, -0.5F, 6, 4, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F);
   }

}
