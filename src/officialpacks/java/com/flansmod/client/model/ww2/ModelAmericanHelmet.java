//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanHelmet extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelAmericanHelmet() {
      this.headModel = new ModelRendererTurbo[10];
      this.headModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.headModel[6] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.headModel[7] = new ModelRendererTurbo(this, 177, 1, this.textureX, this.textureY);
      this.headModel[8] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.headModel[9] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.5F, -6.5F, -4.5F, 9, 1, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.5F, -7.5F, -4.5F, 9, 1, 9, 0.0F, -0.3F, 1.0F, -0.2F, -0.3F, 1.0F, -0.2F, -0.3F, 0.5F, -0.2F, -0.3F, 0.5F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-4.5F, -8.5F, -4.5F, 18, 1, 18, 0.0F, -1.0F, 0.9F, -1.5F, -10.0F, 0.9F, -1.5F, -10.0F, 0.5F, -10.2F, -1.0F, 0.5F, -10.2F, -0.3F, -1.0F, -0.2F, -9.3F, -1.0F, -0.2F, -9.3F, -0.5F, -9.2F, -0.3F, -0.5F, -9.2F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-4.5F, -5.5F, -5.0F, 9, 1, 10, 0.0F, 0.2F, 0.0F, -0.2F, 0.2F, 0.0F, -0.2F, 0.2F, -1.0F, 0.0F, 0.2F, -1.0F, 0.0F, 0.2F, -0.5F, 0.5F, 0.2F, -0.5F, 0.5F, 0.2F, 0.5F, 0.0F, 0.2F, 0.5F, 0.0F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-4.5F, -0.5F, -3.5F, 9, 1, 1, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(-4.5F, -5.5F, -4.5F, 1, 6, 1, 0.0F, 0.0F, -0.5F, -2.0F, -0.5F, -0.5F, -2.0F, -0.5F, -0.5F, 2.0F, 0.0F, -0.5F, 2.0F, 0.0F, -0.5F, -1.0F, -0.5F, -0.5F, -1.0F, -0.5F, -0.5F, 1.0F, 0.0F, -0.5F, 1.0F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[6].addShapeBox(-4.5F, -6.5F, -5.0F, 1, 3, 1, 0.0F, 0.5F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, 0.5F, -0.5F, 0.0F, 0.0F, 0.5F, -3.0F, -0.5F, 0.5F, -3.0F, -0.5F, -0.5F, 2.0F, 0.0F, -0.5F, 2.0F);
      this.headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[7].addShapeBox(-6.0F, -6.5F, -5.0F, 11, 2, 1, 0.0F, -1.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[8].addShapeBox(3.5F, -5.5F, -4.5F, 1, 6, 1, 0.0F, -0.5F, -0.5F, -2.0F, 0.0F, -0.5F, -2.0F, 0.0F, -0.5F, 2.0F, -0.5F, -0.5F, 2.0F, -0.5F, -0.5F, -1.0F, 0.0F, -0.5F, -1.0F, 0.0F, -0.5F, 1.0F, -0.5F, -0.5F, 1.0F);
      this.headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[9].addShapeBox(3.5F, -6.5F, -5.0F, 1, 3, 1, 0.0F, -1.0F, -0.5F, 0.0F, 0.5F, -0.5F, 0.0F, 0.5F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -0.5F, 0.5F, -3.0F, 0.0F, 0.5F, -3.0F, 0.0F, -0.5F, 2.0F, -0.5F, -0.5F, 2.0F);
      this.headModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
