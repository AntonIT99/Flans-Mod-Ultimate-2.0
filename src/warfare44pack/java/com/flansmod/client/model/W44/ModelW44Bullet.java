
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelW44Bullet extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] w44bulletModel = new ModelRendererTurbo[3];

   public ModelW44Bullet() {
      w44bulletModel[0] = new ModelRendererTurbo(this, 9, 12, textureX, textureY);
      w44bulletModel[1] = new ModelRendererTurbo(this, 9, 8, textureX, textureY);
      w44bulletModel[2] = new ModelRendererTurbo(this, 18, 10, textureX, textureY);
      w44bulletModel[0].addShapeBox(-0.5F, 5.0F, -0.5F, 1, 5, 1, 0.0F, -0.15F, 2.0F, -0.15F, -0.15F, 2.0F, -0.15F, -0.15F, 2.0F, -0.15F, -0.15F, 2.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F);
      w44bulletModel[0].setRotationPoint(0.0F, 20.0F, 0.0F);
      w44bulletModel[1].addShapeBox(-0.5F, 10.0F, -0.5F, 1, 1, 1, 0.0F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F);
      w44bulletModel[1].setRotationPoint(0.0F, 20.0F, 0.0F);
      w44bulletModel[2].addShapeBox(-0.5F, -4.0F, -0.5F, 1, 7, 1, 0.0F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F, -0.45F, 0.0F, -0.45F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F);
      w44bulletModel[2].setRotationPoint(0.0F, 20.0F, 0.0F);
      w44bulletModel[0].glow = true;
      w44bulletModel[1].glow = true;
      w44bulletModel[2].glow = true;
   }
}
