
package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanPants extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelAmericanPants() {
      this.leftLegModel = new ModelRendererTurbo[1];
      this.leftLegModel[0] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel = new ModelRendererTurbo[1];
      this.rightLegModel[0] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
