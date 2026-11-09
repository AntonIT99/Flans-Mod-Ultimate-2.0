package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelDetonator extends ModelBase {
   int textureX = 512;
   int textureY = 512;
   public ModelRendererTurbo[] detonatorModel = new ModelRendererTurbo[2];

   public ModelDetonator() {
      this.detonatorModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.detonatorModel[1] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.detonatorModel[0].addBox(0.0F, 0.0F, 0.0F, 2, 6, 4, 0.0F);
      this.detonatorModel[0].setRotationPoint(-1.0F, -3.0F, -2.0F);
      this.detonatorModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 1, 8, 1, 0.0F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F);
      this.detonatorModel[1].setRotationPoint(-0.5F, 2.0F, -0.5F);
   }


   public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5) {
   }
}
