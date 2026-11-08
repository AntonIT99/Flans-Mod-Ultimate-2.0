package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelValkyrie extends ModelMG {
   int textureX = 512;
   int textureY = 512;

   public ModelValkyrie() {
      this.bipodModel = new ModelRendererTurbo[4];
      this.bipodModel[0] = new ModelRendererTurbo(this, 100, 200, this.textureX, this.textureY);
      this.bipodModel[1] = new ModelRendererTurbo(this, 100, 200, this.textureX, this.textureY);
      this.bipodModel[2] = new ModelRendererTurbo(this, 100, 250, this.textureX, this.textureY);
      this.bipodModel[3] = new ModelRendererTurbo(this, 100, 250, this.textureX, this.textureY);
      this.bipodModel[0].addBox(-1.0F, 0.0F, 0.0F, 1, 12, 1, 0.0F);
      this.bipodModel[0].setRotationPoint(-0.5F, 10.0F, -0.5F);
      this.bipodModel[0].rotateAngleZ = -2.86234F;
      this.bipodModel[1].addBox(-1.0F, 0.0F, 0.0F, 1, 12, 1, 0.0F);
      this.bipodModel[1].setRotationPoint(-0.5F, 10.5F, -0.5F);
      this.bipodModel[1].rotateAngleZ = 2.86234F;
      this.bipodModel[2].addBox(2.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      this.bipodModel[2].setRotationPoint(-7.0F, 0.0F, -0.5F);
      this.bipodModel[3].addBox(-2.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      this.bipodModel[3].setRotationPoint(5.0F, 0.0F, -0.5F);
      this.gunModel = new ModelRendererTurbo[11];
      this.gunModel[0] = new ModelRendererTurbo(this, 100, 225, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 200, 150, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 200, 180, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 200, 220, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 200, 230, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 200, 265, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 200, 280, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 200, 300, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 200, 320, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 34, 0, this.textureX, this.textureY);
      this.gunModel[0].addBox(-1.0F, -1.0F, 0.0F, 2, 3, 1, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[1].addBox(-1.5F, -1.5F, 1.0F, 3, 3, 20, 0.0F);
      this.gunModel[1].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[2].addBox(-0.5F, -0.5F, -7.0F, 1, 1, 32, 0.0F);
      this.gunModel[2].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[3].addBox(-2.0F, -2.0F, 22.0F, 4, 4, 2, 0.0F);
      this.gunModel[3].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[4].addBox(-2.0F, -2.2F, 24.5F, 4, 5, 12, 0.0F);
      this.gunModel[4].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[5].addBox(2.0F, -1.0F, 25.0F, 2, 1, 1, 0.0F);
      this.gunModel[5].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[6].addBox(-1.0F, -1.0F, -6.8F, 2, 2, 3, 0.0F);
      this.gunModel[6].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[7].addBox(0.0F, 1.0F, -6.8F, 0, 1, 1, 0.0F);
      this.gunModel[7].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[8].addBox(-0.5F, -5.5F, 36.0F, 1, 4, 1, 0.0F);
      this.gunModel[8].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[8].rotateAngleX = 14.0F;
      this.gunModel[9].addBox(-1.5F, -1.8F, 36.5F, 3, 4, 10);
      this.gunModel[9].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.gunModel[10].addBox(-1.5F, -3.0F, 46.5F, 3, 5, 1, 0.0F);
      this.gunModel[10].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.ammoModel = new ModelRendererTurbo[2];
      this.ammoModel[0] = new ModelRendererTurbo(this, 490, 200, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 490, 200, this.textureX, this.textureY);
      this.ammoModel[0].addBox(-4.0F, -3.5F, 28.5F, 2, 5, 5, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 11.0F, -0.5F);
      this.ammoModel[0].addBox(2.0F, -3.5F, 28.5F, 2, 5, 5, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 11.0F, -0.5F);
   }
}
