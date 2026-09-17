
package com.flansmod.client.model.Manus_WW2.MG;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWW2_MG_GunPlaneGeneric_1 extends ModelMG {
   int textureX = 128;
   int textureY = 32;

   public ModelWW2_MG_GunPlaneGeneric_1() {
      bipodModel = new ModelRendererTurbo[0];
      gunModel = new ModelRendererTurbo[14];
      gunModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      gunModel[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      gunModel[2] = new ModelRendererTurbo(this, 25, 1, textureX, textureY);
      gunModel[3] = new ModelRendererTurbo(this, 41, 1, textureX, textureY);
      gunModel[4] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      gunModel[5] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      gunModel[6] = new ModelRendererTurbo(this, 57, 1, textureX, textureY);
      gunModel[7] = new ModelRendererTurbo(this, 73, 1, textureX, textureY);
      gunModel[8] = new ModelRendererTurbo(this, 81, 1, textureX, textureY);
      gunModel[9] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      gunModel[10] = new ModelRendererTurbo(this, 97, 1, textureX, textureY);
      gunModel[11] = new ModelRendererTurbo(this, 113, 1, textureX, textureY);
      gunModel[12] = new ModelRendererTurbo(this, 121, 1, textureX, textureY);
      gunModel[13] = new ModelRendererTurbo(this, 33, 1, textureX, textureY);
      gunModel[0].addBox(-1.0F, -4.0F, -8.0F, 2, 3, 13, 0.0F);
      gunModel[1].addShapeBox(-1.5F, -4.5F, 5.0F, 3, 4, 3, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F);
      gunModel[2].addBox(-1.0F, -3.5F, 8.0F, 2, 2, 3, 0.0F);
      gunModel[3].addShapeBox(-1.0F, -3.5F, 11.0F, 2, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F);
      gunModel[4].addBox(-0.5F, -3.0F, 11.0F, 1, 1, 17, 0.0F);
      gunModel[5].addShapeBox(-1.0F, -1.0F, -8.0F, 2, 2, 16, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, -2.0F, -0.4F, 0.0F, -2.0F);
      gunModel[6].addShapeBox(-2.0F, -3.5F, 1.0F, 4, 2, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F);
      gunModel[7].addShapeBox(-0.5F, -4.3F, 7.5F, 1, 1, 2, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F);
      gunModel[8].addShapeBox(-0.5F, -1.7F, 7.5F, 1, 1, 2, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F);
      gunModel[9].addBox(-1.5F, -3.5F, -7.0F, 1, 2, 7, 0.0F);
      gunModel[10].addBox(1.0F, -3.8F, -6.0F, 2, 2, 1, 0.0F);
      gunModel[11].addShapeBox(1.0F, -3.8F, -5.0F, 2, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F);
      gunModel[12].addShapeBox(1.0F, -3.8F, -7.0F, 2, 2, 1, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      gunModel[13].addShapeBox(-0.5F, -3.0F, 25.0F, 1, 1, 1, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F);
      ammoModel = new ModelRendererTurbo[0];
      ammoBoxModel = new ModelRendererTurbo[5];
      ammoBoxModel[0] = new ModelRendererTurbo(this, 1, 20, textureX, textureY);
      ammoBoxModel[1] = new ModelRendererTurbo(this, 1, 9, textureX, textureY);
      ammoBoxModel[2] = new ModelRendererTurbo(this, 25, 9, textureX, textureY);
      ammoBoxModel[3] = new ModelRendererTurbo(this, 41, 9, textureX, textureY);
      ammoBoxModel[4] = new ModelRendererTurbo(this, 57, 9, textureX, textureY);
      ammoBoxModel[0].addBox(4.0F, -4.0F, 0.5F, 7, 4, 4, 0.0F);
      ammoBoxModel[1].addShapeBox(3.0F, -5.0F, 1.5F, 1, 1, 2, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F);
      ammoBoxModel[2].addShapeBox(4.0F, -5.0F, 1.5F, 2, 1, 2, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.4F, 0.0F);
      ammoBoxModel[3].addShapeBox(1.0F, -3.0F, 1.5F, 1, 1, 2, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F);
      ammoBoxModel[4].addShapeBox(2.0F, -5.0F, 1.5F, 1, 3, 2, 0.0F, -1.0F, -0.4F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, -1.0F, -0.4F, 0.0F, 0.0F, -0.6F, 0.0F, -1.0F, -0.4F, 0.0F, -1.0F, -0.4F, 0.0F, 0.0F, -0.6F, 0.0F);
      flipAll();
   }
}
