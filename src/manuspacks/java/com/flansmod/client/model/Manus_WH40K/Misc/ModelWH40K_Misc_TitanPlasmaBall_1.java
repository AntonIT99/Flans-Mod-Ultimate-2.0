
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWH40K_Misc_TitanPlasmaBall_1 extends ModelBase {
   int textureX = 512;
   int textureY = 512;
   public ModelRendererTurbo[] wh40k_misc_titanplasmaball_1Model = new ModelRendererTurbo[3];

   public ModelWH40K_Misc_TitanPlasmaBall_1() {
      wh40k_misc_titanplasmaball_1Model[0] = new ModelRendererTurbo(this, -138, 238, textureX, textureY);
      wh40k_misc_titanplasmaball_1Model[1] = new ModelRendererTurbo(this, 62, -166, textureX, textureY);
      wh40k_misc_titanplasmaball_1Model[2] = new ModelRendererTurbo(this, 61, 33, textureX, textureY);
      wh40k_misc_titanplasmaball_1Model[0].addShapeBox(0.0F, 0.0F, 0.0F, 200, 1, 200, 0.0F, 0.0F, -0.49F, 0.0F, -180.0F, -0.49F, 0.0F, -180.0F, -0.49F, -180.0F, 0.0F, -0.49F, -180.0F, 0.0F, -0.49F, 0.0F, -180.0F, -0.49F, 0.0F, -180.0F, -0.49F, -180.0F, 0.0F, -0.49F, -180.0F);
      wh40k_misc_titanplasmaball_1Model[0].setRotationPoint(-10.0F, -0.5F, -10.0F);
      wh40k_misc_titanplasmaball_1Model[1].addShapeBox(0.0F, 0.0F, 0.0F, 1, 200, 200, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, -180.0F, -0.49F, 0.0F, -180.0F, -0.49F, -180.0F, 0.0F, -0.49F, -180.0F, 0.0F, -0.49F, -180.0F, -180.0F, -0.49F, -180.0F, -180.0F);
      wh40k_misc_titanplasmaball_1Model[1].setRotationPoint(-0.5F, -10.0F, -10.0F);
      wh40k_misc_titanplasmaball_1Model[2].addShapeBox(0.0F, 0.0F, 0.0F, 200, 200, 1, 0.0F, 0.0F, 0.0F, -0.49F, -180.0F, 0.0F, -0.49F, -180.0F, 0.0F, -0.49F, 0.0F, 0.0F, -0.49F, 0.0F, -180.0F, -0.49F, -180.0F, -180.0F, -0.49F, -180.0F, -180.0F, -0.49F, 0.0F, -180.0F, -0.49F);
      wh40k_misc_titanplasmaball_1Model[2].setRotationPoint(-10.0F, -10.0F, -0.5F);
   }
}
