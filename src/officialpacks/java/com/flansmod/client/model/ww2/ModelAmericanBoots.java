//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelAmericanBoots extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelAmericanBoots() {
      this.leftLegModel = new ModelRendererTurbo[1];
      this.leftLegModel[0] = new ModelRendererTurbo(this, 113, 25, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.2F, 7.2F, -2.0F, 8, 12, 4, 0.0F, 0.1F, -1.0F, 0.35F, -3.5F, -1.0F, 0.35F, -3.5F, -1.0F, 0.35F, 0.1F, -1.0F, 0.35F, 0.15F, -7.0F, 0.35F, -3.5F, -7.0F, 0.35F, -3.59F, -7.0F, 0.35F, 0.15F, -7.0F, 0.35F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel = new ModelRendererTurbo[1];
      this.rightLegModel[0] = new ModelRendererTurbo(this, 81, 25, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.2F, 7.2F, -2.0F, 8, 12, 4, 0.0F, 0.1F, -1.0F, 0.35F, -3.5F, -1.0F, 0.35F, -3.5F, -1.0F, 0.35F, 0.1F, -1.0F, 0.35F, 0.15F, -7.0F, 0.35F, -3.5F, -7.0F, 0.35F, -3.59F, -7.0F, 0.35F, 0.15F, -7.0F, 0.35F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
