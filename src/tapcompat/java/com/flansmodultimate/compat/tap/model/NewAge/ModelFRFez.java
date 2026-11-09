package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelFRFez extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelFRFez() {
      this.headModel = new ModelRendererTurbo[6];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 89, 1, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-2.5F, -13.0F, -2.5F, 5, 5, 5, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-2.5F, -13.0F, -3.7F, 5, 5, 1, 0.0F, -1.9F, 0.0F, -0.4F, -1.9F, 0.0F, -0.4F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -1.9F, 0.0F, 0.1F, -1.9F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-3.7F, -13.0F, -2.5F, 1, 5, 5, 0.0F, -0.4F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -0.4F, 0.0F, -1.9F, 0.1F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, -1.9F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-2.5F, -13.0F, 2.7F, 5, 5, 1, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -1.9F, 0.0F, -0.4F, -1.9F, 0.0F, -0.4F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -1.9F, 0.0F, 0.1F, -1.9F, 0.0F, 0.1F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(2.7F, -13.0F, -2.5F, 1, 5, 5, 0.0F, 0.1F, 0.0F, 0.1F, -0.4F, 0.0F, -1.9F, -0.4F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, -1.9F, 0.1F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(-4.5F, -13.6F, -0.5F, 4, 5, 1, 0.0F, -0.7F, -0.4F, -0.2F, 0.3F, -0.1F, -0.2F, 0.3F, -0.1F, -0.2F, -0.7F, -0.2F, -0.2F, -0.2F, 0.0F, -0.2F, 0.3F, 0.0F, -0.2F, 0.3F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
