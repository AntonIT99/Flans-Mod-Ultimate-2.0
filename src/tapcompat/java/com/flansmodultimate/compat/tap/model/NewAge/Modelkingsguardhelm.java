package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class Modelkingsguardhelm extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 64;

   public Modelkingsguardhelm() {
      this.headModel = new ModelRendererTurbo[3];
      this.initheadModel_1();
   }

   private void initheadModel_1() {
      this.headModel[0] = new ModelRendererTurbo(this, 73, 25, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 185, 9, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 185, 9, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.5F, -3.95F, -4.5F, 9, 4, 4, 0.0F, 0.0F, -0.25F, -3.0F, 0.0F, -0.25F, -3.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -0.5F, -0.4F, 0.0F, -2.5F, -0.4F, 0.0F, -2.5F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.0F, -9.75F, -4.0F, 8, 6, 8, 0.0F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F, 0.7F, 0.0F, 0.7F, 0.7F, 0.0F, 0.7F, 0.7F, 0.0F, 0.7F, 0.7F, 0.0F, 0.7F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-4.0F, -11.75F, -4.0F, 8, 2, 8, 0.0F, 0.45F, 0.0F, 0.45F, 0.45F, 0.0F, 0.45F, 0.45F, 0.0F, 0.45F, 0.45F, 0.0F, 0.45F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F, 0.75F, 0.0F, 0.75F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
