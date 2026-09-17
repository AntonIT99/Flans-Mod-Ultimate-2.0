
package com.flansmod.client.model.W44;

import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelW44BulletCasing extends ModelCasing {
   int textureX = 32;
   int textureY = 32;

   public ModelW44BulletCasing() {
      casingModel = new ModelRendererTurbo[3];
      casingModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      casingModel[1] = new ModelRendererTurbo(this, 9, 1, textureX, textureY);
      casingModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY);
      casingModel[0].addShapeBox(-0.5F, -0.5F, -0.5F, 1, 1, 1, 0.0F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F, 0.25F, -0.35F, -0.15F);
      casingModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      casingModel[1].addShapeBox(-0.5F, -0.15F, -0.5F, 1, 1, 1, 0.0F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F);
      casingModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      casingModel[2].addShapeBox(-0.5F, -0.85F, -0.5F, 1, 1, 1, 0.0F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F, 0.25F, -0.5F, -0.35F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F, 0.25F, -0.3F, -0.15F);
      casingModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      flipAll();
   }
}
