package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelFRchechia extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 64;

   public ModelFRchechia() {
      this.headModel = new ModelRendererTurbo[6];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.0F, -10.0F, -4.0F, 8, 5, 8, 0.0F, 0.1F, -1.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -1.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.0F, -10.0F, -5.2F, 8, 5, 1, 0.0F, -1.9F, -0.5F, 0.1F, -1.9F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -1.0F, 0.1F, -1.9F, 0.0F, -0.4F, -1.9F, 0.0F, -0.4F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-5.2F, -9.0F, -4.0F, 1, 4, 8, 0.0F, 0.1F, -0.5F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, -0.5F, -1.9F, -0.4F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -0.4F, 0.0F, -1.9F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-4.0F, -10.0F, 4.2F, 8, 5, 1, 0.0F, 0.1F, -1.0F, 0.1F, 0.1F, 0.0F, 0.1F, -1.9F, 0.0F, 0.1F, -1.9F, -0.5F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -1.9F, 0.0F, -0.4F, -1.9F, 0.0F, -0.4F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(4.2F, -10.0F, -4.0F, 1, 5, 8, 0.0F, 0.1F, 0.0F, 0.1F, 0.6F, 0.0F, -1.9F, 0.6F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -0.4F, 0.0F, -1.9F, -0.4F, 0.0F, -1.9F, 0.1F, 0.0F, 0.1F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(0.0F, -11.0F, -2.0F, 3, 7, 8, 0.0F, -0.9F, -1.0F, 0.1F, -0.9F, -1.0F, 0.1F, 0.1F, 0.0F, -0.9F, 0.1F, 0.0F, 0.1F, -0.9F, 0.25F, 0.1F, 0.1F, 0.0F, 0.1F, 1.1F, 0.5F, 0.1F, 0.1F, 0.75F, 1.1F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
