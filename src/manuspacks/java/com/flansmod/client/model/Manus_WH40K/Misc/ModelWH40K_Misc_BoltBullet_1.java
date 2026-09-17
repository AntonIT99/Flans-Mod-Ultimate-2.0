
package com.flansmod.client.model.Manus_WH40K.Misc;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelWH40K_Misc_BoltBullet_1 extends ModelBase {
   int textureX = 32;
   int textureY = 32;
   public ModelRendererTurbo[] boltbulletModel = new ModelRendererTurbo[2];

   public ModelWH40K_Misc_BoltBullet_1() {
      boltbulletModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY);
      boltbulletModel[1] = new ModelRendererTurbo(this, 9, 1, textureX, textureY);
      boltbulletModel[0].addShapeBox(-0.5F, 0.0F, -0.5F, 1, 1, 1, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F);
      boltbulletModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      boltbulletModel[1].addShapeBox(-0.5F, 0.0F, -0.5F, 1, 1, 1, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.35F, -0.5F, -0.35F, -0.35F, -0.5F, -0.35F, -0.35F, -0.5F, -0.35F, -0.35F, -0.5F, -0.35F);
      boltbulletModel[1].setRotationPoint(0.0F, 1.0F, 0.0F);
   }
}
