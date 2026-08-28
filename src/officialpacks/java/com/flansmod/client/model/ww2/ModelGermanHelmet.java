//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanHelmet extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelGermanHelmet() {
      this.headModel = new ModelRendererTurbo[9];
      this.headModel[0] = new ModelRendererTurbo(this, 35, 95, this.textureX, this.textureY);
      this.headModel[1] = new ModelRendererTurbo(this, 75, 90, this.textureX, this.textureY);
      this.headModel[2] = new ModelRendererTurbo(this, 75, 90, this.textureX, this.textureY);
      this.headModel[3] = new ModelRendererTurbo(this, 50, 110, this.textureX, this.textureY);
      this.headModel[4] = new ModelRendererTurbo(this, 55, 110, this.textureX, this.textureY);
      this.headModel[5] = new ModelRendererTurbo(this, 35, 90, this.textureX, this.textureY);
      this.headModel[6] = new ModelRendererTurbo(this, 45, 85, this.textureX, this.textureY);
      this.headModel[7] = new ModelRendererTurbo(this, 15, 105, this.textureX, this.textureY);
      this.headModel[8] = new ModelRendererTurbo(this, 45, 85, this.textureX, this.textureY);
      this.headModel[0].addShapeBox(-4.5F, -0.5F, -3.0F, 9, 1, 1, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[1].addShapeBox(-4.5F, -4.0F, -3.0F, 1, 4, 1, 0.0F, 0.5F, -0.5F, -0.5F, -1.0F, -0.5F, -0.5F, -1.0F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[2].addShapeBox(3.5F, -4.0F, -3.0F, 1, 4, 1, 0.0F, -1.0F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -1.0F, -0.5F, 0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F);
      this.headModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[3].addShapeBox(-3.5F, -5.0F, 3.5F, 7, 3, 1, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 1.5F, -0.5F, -1.0F, 1.5F, -0.5F, -1.0F, 1.5F, -0.5F, 1.0F, 1.5F, -0.5F, 1.0F);
      this.headModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[4].addShapeBox(-4.0F, -8.0F, -4.0F, 16, 1, 16, 0.0F, -0.1F, 0.2F, -0.1F, -8.1F, 0.2F, -0.1F, -8.1F, 0.2F, -8.1F, -0.1F, 0.2F, -8.1F, 0.3F, -0.5F, 0.3F, -7.7F, -0.5F, 0.3F, -7.7F, -0.5F, -7.7F, 0.3F, -0.5F, -7.7F);
      this.headModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[5].addShapeBox(-4.5F, -6.0F, -5.0F, 9, 1, 1, 0.0F, -0.1F, 0.0F, -1.0F, -0.1F, 0.0F, -1.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.headModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[6].addShapeBox(3.5F, -5.0F, -3.0F, 1, 3, 16, 0.0F, -0.5F, 0.0F, 2.5F, 0.0F, 0.0F, 2.5F, 0.0F, 0.0F, -8.5F, -0.5F, 0.0F, -8.5F, -1.5F, -0.5F, 0.0F, 1.0F, -0.5F, 0.0F, 1.0F, -0.5F, -7.5F, -1.5F, -0.5F, -7.5F);
      this.headModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[7].addShapeBox(-4.0F, -7.0F, -4.0F, 8, 2, 16, 0.0F, 0.3F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F, 0.3F, 0.5F, -7.7F, 0.3F, 0.5F, -7.7F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, -7.5F, 0.5F, 0.0F, -7.5F);
      this.headModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.headModel[8].addShapeBox(-4.5F, -5.0F, -3.0F, 1, 3, 16, 0.0F, 0.0F, 0.0F, 2.5F, -0.5F, 0.0F, 2.5F, -0.5F, 0.0F, -8.5F, 0.0F, 0.0F, -8.5F, 1.0F, -0.5F, 0.0F, -1.5F, -0.5F, 0.0F, -1.5F, -0.5F, -7.5F, 1.0F, -0.5F, -7.5F);
      this.headModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
