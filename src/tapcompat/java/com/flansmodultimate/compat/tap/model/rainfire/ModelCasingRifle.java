package com.flansmodultimate.compat.tap.model.rainfire;

import com.flansmod.client.model.ModelCasing;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelCasingRifle extends ModelCasing {
   int textureX = 32;
   int textureY = 32;

   public ModelCasingRifle() {
      this.casingModel = new ModelRendererTurbo[9];
      this.casingModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.casingModel[1] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.casingModel[2] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.casingModel[3] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.casingModel[4] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.casingModel[5] = new ModelRendererTurbo(this, 9, 9, this.textureX, this.textureY);
      this.casingModel[6] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.casingModel[7] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.casingModel[8] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.casingModel[0].addShapeBox(0.07F, -0.6F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.49F, -0.44F, -0.45F, -0.52F, -0.45F, -0.45F, -0.52F, -0.45F, -0.4F, -0.49F, -0.44F, -0.4F, -0.45F, -0.38F, -0.45F, -0.45F, -0.42F, -0.45F, -0.45F, -0.42F, -0.4F, -0.45F, -0.38F);
      this.casingModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[1].addShapeBox(0.2F, -0.6F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F);
      this.casingModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[2].addShapeBox(0.2F, -0.5F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F);
      this.casingModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[3].addShapeBox(0.2F, -0.4F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.45F, -0.42F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F, -0.4F, -0.52F, -0.45F);
      this.casingModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[4].addShapeBox(0.07F, -0.4F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.45F, -0.38F, -0.45F, -0.45F, -0.42F, -0.45F, -0.45F, -0.42F, -0.4F, -0.45F, -0.38F, -0.4F, -0.49F, -0.44F, -0.45F, -0.52F, -0.46F, -0.45F, -0.52F, -0.46F, -0.4F, -0.49F, -0.44F);
      this.casingModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[5].addShapeBox(0.07F, -0.5F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.45F, -0.38F, -0.45F, -0.45F, -0.42F, -0.45F, -0.45F, -0.42F, -0.4F, -0.45F, -0.38F, -0.4F, -0.45F, -0.38F, -0.45F, -0.45F, -0.42F, -0.45F, -0.45F, -0.42F, -0.4F, -0.45F, -0.38F);
      this.casingModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[6].addShapeBox(-0.75F, -0.6F, -0.5F, 1, 1, 1, 0.0F, 0.0F, -0.49F, -0.45F, 0.22F, -0.49F, -0.45F, 0.22F, -0.49F, -0.45F, 0.0F, -0.49F, -0.45F, 0.0F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.0F, -0.45F, -0.38F);
      this.casingModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[7].addShapeBox(-0.75F, -0.5F, -0.5F, 1, 1, 1, 0.0F, 0.0F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.0F, -0.45F, -0.38F, 0.0F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.0F, -0.45F, -0.38F);
      this.casingModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.casingModel[8].addShapeBox(-0.75F, -0.4F, -0.5F, 1, 1, 1, 0.0F, 0.0F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.22F, -0.45F, -0.38F, 0.0F, -0.45F, -0.38F, 0.0F, -0.49F, -0.45F, 0.22F, -0.49F, -0.45F, 0.22F, -0.49F, -0.45F, 0.0F, -0.49F, -0.45F);
      this.casingModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
