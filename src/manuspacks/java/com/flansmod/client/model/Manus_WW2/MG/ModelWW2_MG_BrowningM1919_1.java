
package com.flansmod.client.model.Manus_WW2.MG;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWW2_MG_BrowningM1919_1 extends ModelMG {
   int textureX = 128;
   int textureY = 32;

   public ModelWW2_MG_BrowningM1919_1() {
      bipodModel = new ModelRendererTurbo[9];
      bipodModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      bipodModel[1] = new ModelRendererTurbo(this, 0, 4, textureX, textureY);
      bipodModel[2] = new ModelRendererTurbo(this, 0, 8, textureX, textureY);
      bipodModel[3] = new ModelRendererTurbo(this, 0, 16, textureX, textureY);
      bipodModel[4] = new ModelRendererTurbo(this, 24, 0, textureX, textureY);
      bipodModel[5] = new ModelRendererTurbo(this, 29, 0, textureX, textureY);
      bipodModel[6] = new ModelRendererTurbo(this, 0, 13, textureX, textureY);
      bipodModel[7] = new ModelRendererTurbo(this, 9, 8, textureX, textureY);
      bipodModel[8] = new ModelRendererTurbo(this, 9, 0, textureX, textureY);
      bipodModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[0].setRotationPoint(-1.0F, -0.5F, 7.0F);
      bipodModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[1].setRotationPoint(-7.0F, -0.5F, -9.0F);
      bipodModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      bipodModel[2].setRotationPoint(5.0F, -0.5F, -9.0F);
      bipodModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 9, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, -0.5F, 0.0F, -0.25F, -0.5F, 0.0F, -0.25F, -0.5F, 0.0F, -0.25F, -0.5F, 0.0F);
      bipodModel[3].setRotationPoint(-0.5F, -4.5F, 1.0F);
      bipodModel[3].rotateAngleX = -0.4886922F;
      bipodModel[4].addShapeBox(-1.0F, 0.0F, 0.0F, 1, 10, 1, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F);
      bipodModel[4].setRotationPoint(0.0F, -4.5F, -1.0F);
      bipodModel[4].rotateAngleX = -1.151917F;
      bipodModel[4].rotateAngleY = -0.6632251F;
      bipodModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 1, 10, 1, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      bipodModel[5].setRotationPoint(0.0F, -4.5F, -1.0F);
      bipodModel[5].rotateAngleX = -1.151917F;
      bipodModel[5].rotateAngleY = 0.6806784F;
      bipodModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 7, 1, 1, 0.0F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F);
      bipodModel[6].setRotationPoint(-3.5F, -2.7F, -4.0F);
      bipodModel[6].rotateAngleX = -1.151917F;
      bipodModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F);
      bipodModel[7].setRotationPoint(-1.0F, -4.5F, -1.0F);
      bipodModel[8].addShapeBox(0.0F, 0.0F, 0.0F, 1, 2, 1, 0.0F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F);
      bipodModel[8].setRotationPoint(-0.5F, -6.0F, -0.5F);
      gunModel = new ModelRendererTurbo[11];
      gunModel[0] = new ModelRendererTurbo(this, 35, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 35, 0, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 63, 0, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 51, 0, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 35, 0, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 35, 5, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 50, 0, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 65, 0, textureX, textureY);
      gunModel[8] = new ModelRendererTurbo(this, 78, 0, textureX, textureY);
      gunModel[9] = new ModelRendererTurbo(this, 78, 3, textureX, textureY);
      gunModel[10] = new ModelRendererTurbo(this, 83, 3, textureX, textureY);
      gunModel[0].addBox(-0.5F, -1.0F, 0.5F, 1, 1, 12, 0.0F);
      gunModel[0].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[1].flip = true;
      gunModel[1].addBox(-0.5F, -1.0F, 0.5F, 1, 1, 12, 0.0F);
      gunModel[1].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[2].addShapeBox(-0.5F, -1.0F, 0.5F, 1, 1, 12, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F);
      gunModel[2].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[3].addBox(-1.0F, -1.5F, -7.5F, 2, 2, 8, 0.0F);
      gunModel[3].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[4].addBox(-0.5F, -1.0F, -9.5F, 1, 1, 2, 0.0F);
      gunModel[4].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[5].addShapeBox(-0.5F, 0.0F, -10.5F, 1, 2, 2, 0.0F, -0.15F, 0.0F, -1.0F, -0.15F, 0.0F, -1.0F, -0.15F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.15F, 0.0F, -0.6F, -0.15F, 0.0F, -0.6F);
      gunModel[5].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[6].addBox(-0.9F, -1.0F, -2.5F, 2, 1, 2, 0.0F);
      gunModel[6].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[7].addBox(-1.1F, -1.0F, -2.5F, 2, 1, 2, 0.0F);
      gunModel[7].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[8].addShapeBox(1.0F, -1.0F, -3.5F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F);
      gunModel[8].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[9].addShapeBox(-0.5F, -2.0F, -0.6F, 1, 2, 1, 0.0F, -0.35F, 0.0F, -0.4F, -0.35F, 0.0F, -0.4F, -0.35F, 0.0F, -0.1F, -0.35F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[9].setRotationPoint(0.0F, -6.0F, 0.0F);
      gunModel[10].addShapeBox(-0.5F, -2.0F, -6.5F, 1, 2, 2, 0.0F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.1F, -0.2F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[10].setRotationPoint(0.0F, -6.0F, 0.0F);
      ammoModel = new ModelRendererTurbo[1];
      ammoModel[0] = new ModelRendererTurbo(this, 22, 14, textureX, textureY);
      ammoModel[0].addBox(-6.0F, -3.0F, -2.5F, 5, 3, 2, 0.0F);
      ammoModel[0].setRotationPoint(0.0F, -6.0F, 0.0F);
      flipAll();
   }
}
