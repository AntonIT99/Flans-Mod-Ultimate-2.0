//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanSniperPants extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelGermanSniperPants() {
      this.bodyModel = new ModelRendererTurbo[6];
      this.bodyModel[0] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(2.3F, 1.3F, -2.2F, 8, 13, 1, 0.0F, 0.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, 0.0F, 1.5F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(2.3F, 1.3F, 1.2F, 8, 13, 1, 0.0F, 0.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, 0.0F, 1.5F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-3.3F, 1.3F, 1.2F, 8, 13, 1, 0.0F, 0.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, 0.0F, 1.5F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(-3.3F, 1.3F, -2.2F, 8, 13, 1, 0.0F, 0.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, -7.0F, 1.5F, 0.1F, 0.0F, 1.5F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(-3.3F, -6.7F, -0.5F, 8, 13, 1, 0.0F, 0.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(2.3F, -6.7F, -0.5F, 8, 13, 1, 0.0F, 0.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, -7.0F, -6.0F, 1.8F, 0.0F, -6.0F, 1.8F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel = new ModelRendererTurbo[1];
      this.leftLegModel[0] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.0F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel = new ModelRendererTurbo[1];
      this.rightLegModel[0] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.2F, 2.0F, 0.2F, 0.5F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
