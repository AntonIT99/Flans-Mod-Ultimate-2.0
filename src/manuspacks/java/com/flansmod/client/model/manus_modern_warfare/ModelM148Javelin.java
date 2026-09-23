
package com.flansmod.client.model.manus_modern_warfare;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelM148Javelin extends ModelMG {
   int textureX = 128;
   int textureY = 64;

   public ModelM148Javelin() {
      bipodModel = new ModelRendererTurbo[4];
      bipodModel[0] = new ModelRendererTurbo(this, 0, 25, textureX, textureY);
      bipodModel[1] = new ModelRendererTurbo(this, 21, 20, textureX, textureY);
      bipodModel[2] = new ModelRendererTurbo(this, 26, 20, textureX, textureY);
      bipodModel[3] = new ModelRendererTurbo(this, 21, 16, textureX, textureY);
      bipodModel[0].addBox(0.0F, 0.0F, 0.0F, 6, 1, 4, 0.0F);
      bipodModel[0].setRotationPoint(-3.0F, 0.0F, -2.0F);
      bipodModel[1].addBox(0.0F, 0.0F, 0.0F, 1, 7, 1, 0.0F);
      bipodModel[1].setRotationPoint(-2.5F, 1.0F, -0.5F);
      bipodModel[1].rotateAngleZ = (float) (-Math.PI / 12);
      bipodModel[2].addBox(-1.0F, 0.0F, 0.0F, 1, 7, 1, 0.0F);
      bipodModel[2].setRotationPoint(2.5F, 1.0F, -0.5F);
      bipodModel[2].rotateAngleZ = (float) (Math.PI / 12);
      bipodModel[3].addBox(0.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      bipodModel[3].setRotationPoint(-1.0F, 7.0F, -0.5F);
      gunModel = new ModelRendererTurbo[8];
      gunModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 0, 14, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 45, 0, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 21, 10, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 45, 11, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 45, 14, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 45, 20, textureX, textureY);
      gunModel[0].addBox(5.0F, -2.0F, -8.0F, 4, 4, 32, 0.0F);
      gunModel[0].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[1].addBox(4.0F, -3.0F, 20.0F, 6, 6, 3, 0.0F);
      gunModel[1].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[1].rotateAngleZ = -0.01745329F;
      gunModel[2].addTrapezoid(4.0F, -3.0F, 23.0F, 6, 6, 1, 0.0F, -1.0F, 1);
      gunModel[2].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[3].addBox(-3.0F, -3.0F, -1.0F, 9, 4, 4, 0.0F);
      gunModel[3].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[4].addBox(3.0F, -6.0F, 0.0F, 1, 3, 1, 0.0F);
      gunModel[4].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[5].addBox(-3.0F, 1.0F, 3.0F, 6, 0, 1, 0.0F);
      gunModel[5].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[6].addBox(-3.0F, -2.0F, 3.0F, 0, 3, 1, 0.0F);
      gunModel[6].setRotationPoint(0.0F, 11.0F, 0.0F);
      gunModel[7].addBox(-2.0F, -2.0F, -1.5F, 4, 2, 1, 0.0F);
      gunModel[7].setRotationPoint(0.0F, 11.0F, 0.0F);
      ammoModel = new ModelRendererTurbo[0];
   }
}
