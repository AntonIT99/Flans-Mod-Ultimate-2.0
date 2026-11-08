package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelKhakiTurbanBeard extends ModelCustomArmour {
   int textureX = 128;
   int textureY = 64;

   public ModelKhakiTurbanBeard() {
      this.headModel = new ModelRendererTurbo[13];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 89, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 89, 9, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.headModel[6] = new ModelRendererTurbo(this, 33, 17, this.textureX, this.textureY);
      this.headModel[7] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[8] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[9] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[10] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[11] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[12] = new ModelRendererTurbo(this, 88, 25, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(0.0F, -6.0F, -3.5F, 4, 2, 9, 0.0F, 0.0F, 2.5F, 0.9F, 0.9F, 1.5F, 0.9F, 0.9F, 0.8F, -1.1F, 0.0F, 1.8F, -1.1F, 0.0F, -2.7F, 0.9F, 0.4F, -0.7F, 0.9F, 0.4F, 0.3F, -1.1F, 0.0F, 0.3F, -1.1F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(0.0F, -9.0F, -3.5F, 4, 3, 9, 0.0F, 0.0F, 2.1F, -0.8F, -0.7F, 1.1F, -1.0F, 0.1F, -0.7F, -1.6F, 0.0F, 0.3F, -1.6F, 0.0F, -2.5F, 0.9F, 0.9F, -1.5F, 0.9F, 0.9F, -0.8F, -1.1F, 0.0F, -1.8F, -1.1F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(0.0F, -3.2F, -3.5F, 4, 1, 9, 0.0F, 0.0F, 3.5F, 0.9F, 0.4F, 1.5F, 0.9F, 0.4F, 0.5F, -1.1F, 0.0F, 0.5F, -1.1F, 0.0F, -4.0F, 0.6F, 0.1F, -1.0F, 0.6F, 0.1F, 0.1F, -1.4F, 0.0F, -0.9F, -1.4F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addTrapezoid(-2.0F, -6.2F, -4.1F, 4, 1, 6, 0.0F, 0.0F, 4);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-4.0F, -3.2F, -3.5F, 4, 1, 9, 0.0F, 0.4F, 1.5F, 0.9F, 0.0F, 3.5F, 0.9F, 0.0F, 0.5F, -1.1F, 0.4F, 0.5F, -1.1F, 0.1F, -1.0F, 0.6F, 0.0F, -4.0F, 0.6F, 0.0F, -0.9F, -1.4F, 0.1F, 0.1F, -1.4F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(-4.0F, -6.0F, -3.5F, 4, 2, 9, 0.0F, 0.9F, 1.5F, 0.9F, 0.0F, 2.5F, 0.9F, 0.0F, 1.8F, -1.1F, 0.9F, 0.8F, -1.1F, 0.4F, -0.7F, 0.9F, 0.0F, -2.7F, 0.9F, 0.0F, 0.3F, -1.1F, 0.4F, 0.3F, -1.1F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[6].addShapeBox(-4.0F, -9.0F, -3.5F, 4, 3, 9, 0.0F, -0.7F, 1.1F, -1.0F, 0.0F, 2.1F, -0.8F, 0.0F, 0.3F, -1.6F, 0.1F, -0.7F, -1.6F, 0.9F, -1.5F, 0.9F, 0.0F, -2.5F, 0.9F, 0.0F, -1.8F, -1.1F, 0.9F, -0.8F, -1.1F);
      this.headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[7].addShapeBox(-3.0F, -5.0F, -4.0F, 2, 1, 1, 0.0F, 0.0F, -0.3F, 0.2F, 0.0F, -0.5F, 0.2F, 0.0F, -0.5F, 0.2F, 0.0F, -0.3F, 0.2F, 0.2F, 0.0F, 0.2F, 0.4F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[8].addShapeBox(1.0F, -5.0F, -4.0F, 2, 1, 1, 0.0F, 0.0F, -0.5F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.5F, 0.2F, 0.4F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[9].addShapeBox(-4.0F, -1.0F, -4.0F, 8, 1, 5, 0.0F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 3.0F, 0.2F, 0.2F, 3.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, -1.0F, 0.2F, 0.2F, -1.0F, 0.2F);
      this.headModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[10].addShapeBox(-2.0F, -2.5F, -4.0F, 2, 1, 1, 0.0F, 0.0F, -0.8F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.8F, 0.2F, 0.5F, 0.7F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.3F, 0.2F, 0.5F, 0.7F, 0.2F);
      this.headModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[11].addShapeBox(0.0F, -2.5F, -4.0F, 2, 1, 1, 0.0F, 0.0F, -0.3F, 0.2F, 0.0F, -0.8F, 0.2F, 0.0F, -0.8F, 0.2F, 0.0F, -0.3F, 0.2F, 0.0F, -0.3F, 0.2F, 0.5F, 0.7F, 0.2F, 0.5F, 0.7F, 0.2F, 0.0F, -0.3F, 0.2F);
      this.headModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[12].addShapeBox(-4.0F, 0.0F, -4.0F, 8, 1, 5, 0.0F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 1.0F, 0.2F, 0.2F, 1.0F, 0.2F, -2.8F, 0.0F, 0.2F, -2.8F, 0.0F, 0.2F, -2.8F, 0.0F, 0.2F, -2.8F, 0.0F, 0.2F);
      this.headModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
