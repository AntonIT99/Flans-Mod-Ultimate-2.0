
package com.flansmod.client.model.manus_modern_warfare;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelLargeBomb extends ModelBase {
   int textureX = 128;
   int textureY = 64;
   public ModelRendererTurbo[] missleModel = new ModelRendererTurbo[5];

   public ModelLargeBomb() {
      missleModel[0] = new ModelRendererTurbo(this, 9, 0, textureX, textureY);
      missleModel[1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      missleModel[2] = new ModelRendererTurbo(this, 18, 0, textureX, textureY);
      missleModel[3] = new ModelRendererTurbo(this, 18, 10, textureX, textureY);
      missleModel[4] = new ModelRendererTurbo(this, 18, 15, textureX, textureY);
      missleModel[0].addTrapezoid(-3.0F, 0.0F, -3.0F, 6, 10, 6, 0.0F, -2.0F, 4);
      missleModel[1].addBox(-3.0F, 10.0F, -3.0F, 6, 16, 6, 0.0F);
      missleModel[2].addTrapezoid(-3.0F, 26.0F, -3.0F, 6, 3, 6, 0.0F, -1.5F, 4);
      missleModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 10, 4, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      missleModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      missleModel[3].rotateAngleY = (float) (-Math.PI / 4);
      missleModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 10, 4, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      missleModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      missleModel[4].rotateAngleY = (float) (Math.PI / 4);
   }

}
