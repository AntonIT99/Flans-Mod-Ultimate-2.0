
package com.flansmod.client.model.manus_modern_warfare;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelHKGMG extends ModelMG {
   int textureX = 256;
   int textureY = 32;

   public ModelHKGMG() {
      bipodModel = new ModelRendererTurbo[8];
      bipodModel[0] = new ModelRendererTurbo(this, 248, 0, textureX, textureY);
      bipodModel[1] = new ModelRendererTurbo(this, 248, 0, textureX, textureY);
      bipodModel[2] = new ModelRendererTurbo(this, 248, 0, textureX, textureY);
      bipodModel[3] = new ModelRendererTurbo(this, 242, 4, textureX, textureY);
      bipodModel[4] = new ModelRendererTurbo(this, 242, 4, textureX, textureY);
      bipodModel[5] = new ModelRendererTurbo(this, 242, 4, textureX, textureY);
      bipodModel[6] = new ModelRendererTurbo(this, 243, 0, textureX, textureY);
      bipodModel[7] = new ModelRendererTurbo(this, 230, 0, textureX, textureY);
      bipodModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[0].setRotationPoint(-1.0F, -0.5F, 6.0F);
      bipodModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[1].setRotationPoint(-6.0F, -0.5F, -8.0F);
      bipodModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[2].setRotationPoint(4.0F, -0.5F, -8.0F);
      bipodModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.9999F, 0.0F, 0.0F, -4.9999F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      bipodModel[3].setRotationPoint(-0.5F, -5.0F, 1.0F);
      bipodModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 6, 0.0F, 3.5F, -5.0F, 0.0F, -3.5F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.5F, 0.0F, -1.0F, -3.5F, 0.0F, -1.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F);
      bipodModel[4].setRotationPoint(-2.0F, -5.0F, -7.0F);
      bipodModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 6, 0.0F, -3.5F, -5.0F, 0.0F, 3.5F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -3.5F, 0.0F, -1.0F, 3.5F, 0.0F, -1.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F);
      bipodModel[5].setRotationPoint(1.0F, -5.0F, -7.0F);
      bipodModel[6].addBox(0.0F, 0.0F, 0.0F, 1, 6, 1, 0.0F);
      bipodModel[6].setRotationPoint(-0.5F, -7.0F, -0.5F);
      bipodModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F);
      bipodModel[7].setRotationPoint(-2.0F, -5.0F, -1.0F);
      gunModel = new ModelRendererTurbo[10];
      gunModel[0] = new ModelRendererTurbo(this, 215, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 213, 0, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 196, 0, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 191, 0, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 174, 0, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 169, 0, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 158, 0, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 158, 0, textureX, textureY);
      gunModel[8] = new ModelRendererTurbo(this, 81, 0, textureX, textureY);
      gunModel[9] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      gunModel[0].addBox(-1.5F, -3.5F, -7.0F, 3, 3, 8, 0.0F);
      gunModel[0].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[1].addBox(1.0F, -3.0F, -3.0F, 1, 2, 3, 0.0F);
      gunModel[1].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[2].addShapeBox(-0.5F, -2.5F, 1.0F, 1, 1, 7, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F, -0.15F, -0.15F, 0.0F);
      gunModel[2].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[3].addBox(-0.5F, -2.5F, 8.0F, 1, 1, 4, 0.0F);
      gunModel[3].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[4].addShapeBox(-4.5F, -1.0F, -6.0F, 9, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F);
      gunModel[4].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[5].addBox(-2.0F, -3.0F, -5.5F, 1, 2, 1, 0.0F);
      gunModel[5].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[6].addShapeBox(-2.0F, -6.0F, -11.5F, 1, 3, 7, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, -2.9999F, 0.0F, 0.0F, -2.9999F, 0.0F, 0.0F, -2.5F, 0.0F, -0.5F, -2.5F, 0.0F, -0.5F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      gunModel[6].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[7].addShapeBox(-0.5F, -4.0F, -6.0F, 1, 1, 2, 0.0F, -0.2F, 0.0F, -0.1F, -0.2F, 0.0F, -0.1F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[7].setRotationPoint(0.0F, -5.999F, 0.0F);
      gunModel[8].addShapeBox(-0.5F, -5.0F, -6.0F, 30, 30, 2, 0.0F, 0.0F, 0.0F, 0.0F, -29.0F, 0.0F, 0.0F, -29.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -29.0F, 0.0F, -29.0F, -29.0F, 0.0F, -29.0F, -29.0F, 0.0F, 0.0F, -29.0F, 0.0F);
      gunModel[8].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[9].flip = true;
      gunModel[9].addShapeBox(-0.5F, -5.0F, -6.0F, 30, 30, 2, 0.0F, 0.0F, 0.0F, 0.0F, -29.0F, 0.0F, 0.0F, -29.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -29.0F, 0.0F, -29.0F, -29.0F, 0.0F, -29.0F, -29.0F, 0.0F, 0.0F, -29.0F, 0.0F);
      gunModel[9].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel = new ModelRendererTurbo[5];
      ammoModel[0] = new ModelRendererTurbo(this, 66, 0, textureX, textureY);
      ammoModel[1] = new ModelRendererTurbo(this, 66, 6, textureX, textureY);
      ammoModel[2] = new ModelRendererTurbo(this, 66, 9, textureX, textureY);
      ammoModel[3] = new ModelRendererTurbo(this, 66, 12, textureX, textureY);
      ammoModel[4] = new ModelRendererTurbo(this, 66, 15, textureX, textureY);
      ammoModel[0].addBox(-6.5F, -1.5F, -2.0F, 5, 3, 2, 0.0F);
      ammoModel[0].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel[1].addShapeBox(-3.3F, -2.0F, -0.9F, 1, 1, 1, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.25F, -0.25F, -0.2F, -0.25F, -0.25F, -0.2F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.25F, -0.25F, -0.2F, -0.25F, -0.25F, -0.2F);
      ammoModel[1].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel[2].addShapeBox(-3.3F, -2.0F, -1.9F, 1, 1, 1, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F);
      ammoModel[2].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel[3].addShapeBox(-2.3F, -2.8F, -0.9F, 1, 1, 1, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.25F, -0.25F, -0.2F, -0.25F, -0.25F, -0.2F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.25F, -0.25F, -0.2F, -0.25F, -0.25F, -0.2F);
      ammoModel[3].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel[4].addShapeBox(-2.3F, -2.8F, -1.9F, 1, 1, 1, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F);
      ammoModel[4].setRotationPoint(0.0F, -6.0F, 0.0F);
      flipAll();
   }
}
