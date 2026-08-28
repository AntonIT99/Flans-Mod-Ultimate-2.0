//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanSniperPants extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelAmericanSniperPants() {
      this.leftLegModel = new ModelRendererTurbo[2];
      this.leftLegModel[0] = new ModelRendererTurbo(this, 153, 33, this.textureX, this.textureY);
      this.leftLegModel[1] = new ModelRendererTurbo(this, 105, 49, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, 0.0F, -2.0F, 4, 5, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[1].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 3, 4, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F);
      this.leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel = new ModelRendererTurbo[2];
      this.rightLegModel[0] = new ModelRendererTurbo(this, 129, 33, this.textureX, this.textureY);
      this.rightLegModel[1] = new ModelRendererTurbo(this, 81, 49, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, 0.0F, -2.0F, 4, 5, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[1].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 3, 4, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F);
      this.rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
