
package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelW44ShellLarge extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] w44shelllargeModel = new ModelRendererTurbo[3];

   public ModelW44ShellLarge() {
      w44shelllargeModel[0] = new ModelRendererTurbo(this, 8, 7, textureX, textureY);
      w44shelllargeModel[1] = new ModelRendererTurbo(this, 14, 7, textureX, textureY);
      w44shelllargeModel[2] = new ModelRendererTurbo(this, 20, 7, textureX, textureY);
      w44shelllargeModel[0].addShapeBox(-0.5F, 4.0F, -0.5F, 1, 5, 1, 0.0F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F, 0.35F, 3.0F, 0.35F, 0.35F, 3.0F, 0.35F, 0.35F, 3.0F, 0.35F, 0.35F, 3.0F, 0.35F);
      w44shelllargeModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shelllargeModel[1].addShapeBox(-0.5F, 13.0F, -0.5F, 1, 2, 1, 0.0F, 0.35F, 1.0F, 0.35F, 0.35F, 1.0F, 0.35F, 0.35F, 1.0F, 0.35F, 0.35F, 1.0F, 0.35F, -0.4F, 1.0F, -0.4F, -0.4F, 1.0F, -0.4F, -0.4F, 1.0F, -0.4F, -0.4F, 1.0F, -0.4F);
      w44shelllargeModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shelllargeModel[2].addShapeBox(-0.5F, -5.0F, -0.5F, 1, 9, 1, 0.0F, -0.4F, 4.0F, -0.4F, -0.4F, 4.0F, -0.4F, -0.4F, 4.0F, -0.4F, -0.4F, 4.0F, -0.4F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F, 0.35F, 0.0F, 0.35F);
      w44shelllargeModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      w44shelllargeModel[0].glow = true;
      w44shelllargeModel[1].glow = true;
      w44shelllargeModel[2].glow = true;
   }
}
