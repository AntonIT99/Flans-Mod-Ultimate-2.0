//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanSniperHat extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelGermanSniperHat() {
      this.headModel = new ModelRendererTurbo[9];
      this.headModel[0] = new ModelRendererTurbo(this, 97, 25, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 175, 54, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 6, 87, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 225, 41, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 1, 41, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 17, 49, this.textureX, this.textureY);
      this.headModel[6] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.headModel[7] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.headModel[8] = new ModelRendererTurbo(this, 129, 49, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.0F, -6.0F, -6.0F, 8, 1, 2, 0.0F, 0.0F, -0.5F, -2.0F, 0.0F, -0.5F, -2.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.0F, -8.0F, -4.0F, 16, 3, 16, 0.0F, 0.1F, 0.0F, 0.1F, -7.9F, 0.0F, 0.1F, -7.9F, 0.0F, -7.9F, 0.1F, 0.0F, -7.9F, 0.1F, 0.0F, 0.1F, -7.9F, 0.0F, 0.1F, -7.9F, 0.0F, -7.9F, 0.1F, 0.0F, -7.9F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(-11.75F, -7.7F, -4.3F, 150, 68, 1, 0.0F, -10.5F, 0.0F, 0.0F, -137.0F, 0.0F, 0.0F, -137.0F, 0.0F, 0.0F, -10.5F, 0.0F, 0.0F, -10.5F, -67.0F, 0.0F, -137.0F, -67.0F, 0.0F, -137.0F, -67.0F, 0.0F, -10.5F, -67.0F, 0.0F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-4.0F, -8.26F, -4.5F, 8, 3, 1, 0.0F, 0.3F, -0.5F, -0.3F, 0.3F, -0.5F, -0.3F, 0.3F, -0.5F, -0.3F, 0.3F, -0.5F, -0.3F, 0.3F, 0.0F, -0.3F, 0.3F, 0.0F, -0.3F, 0.3F, 0.0F, -0.3F, 0.3F, 0.0F, -0.3F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-4.0F, -8.26F, -4.1F, 1, 3, 7, 0.0F, 0.3F, -0.5F, -0.3F, -0.7F, -0.5F, -0.3F, -0.7F, -0.5F, -0.3F, 0.3F, -0.5F, -0.3F, 0.3F, 0.0F, -0.3F, -0.7F, 0.0F, -0.3F, -0.7F, 0.0F, -0.3F, 0.3F, 0.0F, -0.3F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(4.0F, -8.26F, -4.1F, 1, 3, 7, 0.0F, 0.3F, -0.5F, -0.3F, -0.7F, -0.5F, -0.3F, -0.7F, -0.5F, -0.3F, 0.3F, -0.5F, -0.3F, 0.3F, 0.0F, -0.3F, -0.7F, 0.0F, -0.3F, -0.7F, 0.0F, -0.3F, 0.3F, 0.0F, -0.3F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[6].addShapeBox(-0.5F, -6.4F, -4.3F, 1, 1, 1, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F);
      this.headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[7].addShapeBox(-0.5F, -6.9F, -4.3F, 1, 1, 1, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F);
      this.headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[8].addShapeBox(-4.0F, -9.0F, -4.0F, 8, 1, 8, 0.0F, -0.9F, -0.5F, -0.9F, -0.9F, -0.5F, -0.9F, -0.9F, -0.5F, -0.9F, -0.9F, -0.5F, -0.9F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
