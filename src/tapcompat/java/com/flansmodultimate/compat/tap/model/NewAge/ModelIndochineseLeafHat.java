package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelIndochineseLeafHat extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelIndochineseLeafHat() {
      this.headModel = new ModelRendererTurbo[6];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 89, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-5.0F, -9.17F, -5.0F, 10, 2, 10, 0.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, -0.4F, -1.8F, -2.0F, -0.4F, -1.8F, 1.2F, 0.5F, 1.0F, 1.2F, 0.5F, 1.0F, 1.2F, 0.9F, 1.1F, 1.2F, 0.9F, 1.1F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-1.5F, -11.17F, -1.5F, 3, 2, 3, 0.0F, -1.0F, -0.2F, -1.3F, -1.0F, -0.2F, -1.3F, -1.0F, -0.3F, -1.0F, -1.0F, -0.3F, -1.0F, 1.5F, 0.0F, 1.5F, 1.5F, 0.0F, 1.5F, 1.5F, 0.4F, 1.7F, 1.5F, 0.4F, 1.7F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-6.0F, -6.37F, -6.0F, 12, 1, 12, 0.0F, 0.2F, 0.3F, 0.0F, 0.2F, 0.3F, 0.0F, 0.2F, -0.1F, 0.1F, 0.2F, -0.1F, 0.1F, 0.9F, -0.5F, 0.9F, 0.9F, -0.5F, 0.9F, 0.9F, -0.1F, 0.9F, 0.9F, -0.1F, 0.9F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-1.5F, -11.6F, -1.25F, 3, 1, 3, 0.0F, -1.4F, 0.4F, -1.3F, -1.4F, 0.4F, -1.3F, -1.4F, 0.4F, -1.3F, -1.4F, 0.4F, -1.3F, -1.0F, -0.3F, -1.0F, -1.0F, -0.3F, -1.0F, -1.0F, -0.2F, -1.3F, -1.0F, -0.2F, -1.3F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-4.0F, -7.0F, -1.6F, 4, 7, 2, 0.0F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.2F, 0.05F, 0.0F, -0.2F, 0.05F, 0.05F, 2.2F, 0.0F, 0.2F, 2.2F, 0.0F, 0.2F, -2.8F, 0.05F, 0.05F, -2.8F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(0.0F, -7.0F, -1.6F, 4, 7, 2, 0.0F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.2F, 0.05F, 0.0F, -0.2F, 0.0F, 0.2F, 2.2F, 0.05F, 0.05F, 2.2F, 0.05F, 0.05F, -2.8F, 0.0F, 0.2F, -2.8F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
