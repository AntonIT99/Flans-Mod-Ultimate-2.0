
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelW44ShellSmall extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] w44shellsmallModel = new ModelRendererTurbo[3];

   public ModelW44ShellSmall() {
      w44shellsmallModel[0] = new ModelRendererTurbo(this, 8, 7, textureX, textureY);
      w44shellsmallModel[1] = new ModelRendererTurbo(this, 14, 7, textureX, textureY);
      w44shellsmallModel[2] = new ModelRendererTurbo(this, 20, 7, textureX, textureY);
      w44shellsmallModel[0].addShapeBox(-0.5F, 4.0F, -0.5F, 1, 5, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      w44shellsmallModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shellsmallModel[1].addShapeBox(-0.5F, 9.0F, -0.5F, 1, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F);
      w44shellsmallModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shellsmallModel[2].addShapeBox(-0.5F, -5.0F, -0.5F, 1, 9, 1, 0.0F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      w44shellsmallModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shellsmallModel[0].glow = true;
      w44shellsmallModel[1].glow = true;
      w44shellsmallModel[2].glow = true;
   }
}
