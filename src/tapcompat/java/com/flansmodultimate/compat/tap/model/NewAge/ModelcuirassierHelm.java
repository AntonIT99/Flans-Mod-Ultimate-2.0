package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelcuirassierHelm extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelcuirassierHelm() {
      this.headModel = new ModelRendererTurbo[20];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.headModel[6] = new ModelRendererTurbo(this, 409, 1, this.textureX, this.textureY);
      this.headModel[7] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[8] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.headModel[9] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.headModel[10] = new ModelRendererTurbo(this, 433, 1, this.textureX, this.textureY);
      this.headModel[11] = new ModelRendererTurbo(this, 449, 1, this.textureX, this.textureY);
      this.headModel[12] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.headModel[13] = new ModelRendererTurbo(this, 145, 1, this.textureX, this.textureY);
      this.headModel[14] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.headModel[15] = new ModelRendererTurbo(this, 465, 1, this.textureX, this.textureY);
      this.headModel[16] = new ModelRendererTurbo(this, 481, 1, this.textureX, this.textureY);
      this.headModel[17] = new ModelRendererTurbo(this, 497, 1, this.textureX, this.textureY);
      this.headModel[18] = new ModelRendererTurbo(this, 193, 9, this.textureX, this.textureY);
      this.headModel[19] = new ModelRendererTurbo(this, 209, 9, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.0F, -9.0F, -4.0F, 8, 3, 8, 0.0F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, 0.3F, 0.2F, 0.3F, 0.3F, 0.9F, -0.5F, 0.9F, 0.9F, -0.5F, 0.9F, 0.9F, -0.5F, 0.9F, 0.9F, -0.5F, 0.9F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.0F, -11.8F, -4.0F, 8, 2, 7, 0.0F, -2.5F, -1.0F, -3.0F, -2.5F, -1.0F, -3.0F, -2.5F, 0.0F, -1.0F, -2.5F, 0.0F, -1.0F, 0.2F, 0.5F, -0.2F, 0.2F, 0.5F, -0.2F, 0.2F, 0.5F, 1.3F, 0.2F, 0.5F, 1.3F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-4.0F, -5.0F, -4.0F, 8, 1, 8, 0.0F, 1.0F, 0.2F, 1.0F, 1.0F, 0.2F, 1.0F, 1.0F, -0.2F, 1.0F, 1.0F, -0.2F, 1.0F, 1.0F, -0.4F, 2.5F, 1.0F, -0.4F, 2.5F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-4.0F, -6.5F, -4.0F, 8, 2, 8, 0.0F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, -0.5F, 0.9F, 0.9F, -0.5F, 0.9F, 0.9F, -0.1F, 0.9F, 0.9F, -0.1F, 0.9F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-1.5F, -14.8F, -1.0F, 3, 3, 3, 0.0F, -0.5F, -2.5F, 0.5F, -0.5F, -2.5F, 0.5F, -0.5F, -0.5F, -3.0F, -0.5F, -0.5F, -3.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(-1.0F, -14.3F, -4.0F, 2, 2, 3, 0.0F, 0.0F, -0.5F, -0.7F, 0.0F, -0.5F, -0.7F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, -1.5F, 0.0F, 0.1F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[6].addShapeBox(-4.0F, -4.5F, 3.0F, 8, 4, 1, 0.0F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, 0.0F, 0.9F, 0.9F, -0.5F, 0.1F, 0.9F, -0.5F, 0.1F, 0.9F, 0.3F, 1.7F, 0.9F, 0.3F, 1.7F);
      this.headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[7].addShapeBox(-5.1F, -4.87F, -1.75F, 1, 1, 1, 0.0F, 0.05F, -0.05F, -0.22F, 0.0F, -0.05F, -0.22F, 0.0F, -0.05F, -0.22F, 0.05F, -0.05F, -0.22F, 0.05F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F);
      this.headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[8].addShapeBox(-5.1F, -4.99F, -1.75F, 1, 1, 1, 0.0F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.05F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F, 0.0F, -0.8F, -0.22F, 0.05F, -0.82F, -0.22F);
      this.headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[9].addShapeBox(-5.1F, -5.51F, -1.75F, 1, 1, 1, 0.0F, 0.05F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F, 0.0F, -0.8F, -0.22F, 0.05F, -0.82F, -0.22F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F);
      this.headModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[10].addShapeBox(-4.0F, -5.0F, -1.6F, 4, 5, 2, 0.0F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.2F, 0.05F, 0.0F, -0.2F, 0.05F, 0.05F, 2.2F, 0.0F, 0.2F, 2.2F, 0.0F, 0.2F, -2.8F, 0.05F, 0.05F, -2.8F);
      this.headModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[11].addShapeBox(0.0F, -5.0F, -1.6F, 4, 5, 2, 0.0F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.4F, 0.05F, 0.0F, -0.2F, 0.05F, 0.0F, -0.2F, 0.0F, 0.2F, 2.2F, 0.05F, 0.05F, 2.2F, 0.05F, 0.05F, -2.8F, 0.0F, 0.2F, -2.8F);
      this.headModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[12].addShapeBox(4.1F, -4.99F, -1.75F, 1, 1, 1, 0.0F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F);
      this.headModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[13].addShapeBox(4.1F, -4.87F, -1.75F, 1, 1, 1, 0.0F, 0.0F, -0.05F, -0.22F, 0.05F, -0.05F, -0.22F, 0.05F, -0.05F, -0.22F, 0.0F, -0.05F, -0.22F, 0.0F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.0F, -0.82F, -0.22F);
      this.headModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[14].addShapeBox(4.1F, -5.51F, -1.75F, 1, 1, 1, 0.0F, 0.0F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.05F, -0.82F, -0.22F, 0.0F, -0.8F, -0.22F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F, 0.0F, 0.0F, -0.37F);
      this.headModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[15].addShapeBox(-1.5F, -15.8F, 2.0F, 3, 4, 3, 0.0F, -0.5F, -2.5F, 2.5F, -0.5F, -2.5F, 2.5F, -0.5F, -1.9F, -5.0F, -0.5F, -1.9F, -5.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[16].addShapeBox(-1.5F, -10.8F, 2.0F, 3, 4, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.9F, -1.0F, 0.0F, -1.9F, -1.0F, 0.0F, -2.5F, 2.5F, 0.0F, -2.5F, 2.5F);
      this.headModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[17].addShapeBox(-1.5F, -11.2F, 5.45F, 3, 4, 3, 0.0F, 0.0F, -2.5F, 2.5F, 0.0F, -2.5F, 2.5F, 0.0F, -1.9F, -1.0F, 0.0F, -1.9F, -1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[18].addShapeBox(-1.5F, -7.2F, 5.45F, 3, 13, 3, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.5F, -0.5F, 0.0F, -2.5F, -0.5F, 0.0F, -3.9F, 0.5F, 0.0F, -3.9F, 0.5F);
      this.headModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[19].addShapeBox(1.0F, -14.8F, 0.0F, 2, 5, 2, 0.0F, 0.5F, 4.0F, 0.5F, 0.5F, 4.0F, 0.5F, 0.5F, 3.5F, 0.5F, 0.5F, 3.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
