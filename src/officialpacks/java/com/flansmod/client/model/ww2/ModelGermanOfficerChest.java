//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanOfficerChest extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelGermanOfficerChest() {
      this.bodyModel = new ModelRendererTurbo[8];
      this.bodyModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 133, 1, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 159, 12, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(-4.5F, 0.0F, -2.0F, 16, 18, 4, 0.0F, 0.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, -6.0F, 0.1F, -7.0F, -6.0F, 0.1F, -7.0F, -6.0F, 0.1F, 0.0F, -6.0F, 0.1F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(-4.5F, 0.0F, -2.0F, 16, 18, 4, 0.0F, 0.45F, 0.55F, 0.3F, -6.7F, 0.55F, 0.3F, -6.7F, 0.55F, 0.3F, 0.45F, 0.55F, 0.3F, 0.45F, -5.3F, 0.3F, -6.7F, -5.3F, 0.3F, -6.7F, -5.3F, 0.35F, 0.45F, -5.3F, 0.35F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-4.0F, -1.0F, -2.0F, 2, 1, 4, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 1.0F, -0.5F, 0.5F, 0.0F, -0.5F, 0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(2.0F, -1.0F, -2.0F, 2, 1, 4, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.5F, 1.0F, -0.5F, 0.5F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(-4.0F, -1.0F, -3.0F, 2, 1, 1, 0.0F, -0.5F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, -1.0F, -0.5F, 0.0F, 0.0F, -0.5F, -0.5F, 1.0F, -0.5F, -0.5F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(2.0F, -1.0F, -3.0F, 2, 1, 1, 0.0F, -1.0F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 1.0F, -0.5F, -0.5F, 0.0F, -0.5F, -0.5F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(-5.0F, -0.5F, -3.0F, 3, 4, 1, 0.0F, -0.5F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(2.0F, -0.5F, -3.0F, 3, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel = new ModelRendererTurbo[4];
      this.leftArmModel[0] = new ModelRendererTurbo(this, 89, 65, this.textureX, this.textureY);
      this.leftArmModel[1] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.leftArmModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.leftArmModel[3] = new ModelRendererTurbo(this, 184, 1, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, 7.8F, -2.0F, 4, 1, 4, 0.0F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[1].addShapeBox(0.2F, -2.8F, -0.5F, 2, 1, 1, 0.0F, 0.4F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F);
      this.leftArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[2].addShapeBox(-1.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.2F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, 0.2F, 0.0F, 0.15F, 0.2F, -5.0F, 0.15F, -3.8F, -5.0F, 0.15F, -3.8F, -5.0F, 0.15F, 0.2F, -5.0F, 0.15F);
      this.leftArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[3].addShapeBox(-1.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.5F, 0.3F, 0.45F, -3.5F, 0.3F, 0.45F, -3.5F, 0.3F, 0.45F, 0.5F, 0.3F, 0.45F, 0.5F, -5.5F, 0.45F, -3.5F, -5.5F, 0.45F, -3.5F, -5.5F, 0.45F, 0.5F, -5.5F, 0.45F);
      this.leftArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel = new ModelRendererTurbo[4];
      this.rightArmModel[0] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.rightArmModel[1] = new ModelRendererTurbo(this, 65, 65, this.textureX, this.textureY);
      this.rightArmModel[2] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.rightArmModel[3] = new ModelRendererTurbo(this, 184, 1, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.2F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, 0.2F, 0.0F, 0.15F, 0.2F, -5.0F, 0.15F, -3.8F, -5.0F, 0.15F, -3.8F, -5.0F, 0.15F, 0.2F, -5.0F, 0.15F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[1].addShapeBox(-3.0F, 7.8F, -2.0F, 4, 1, 4, 0.0F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F);
      this.rightArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[2].addShapeBox(-2.2F, -2.8F, -0.5F, 2, 1, 1, 0.0F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F);
      this.rightArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[3].addShapeBox(-3.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.5F, 0.3F, 0.45F, -3.5F, 0.3F, 0.45F, -3.5F, 0.3F, 0.45F, 0.5F, 0.3F, 0.45F, 0.5F, -5.5F, 0.45F, -3.5F, -5.5F, 0.45F, -3.5F, -5.5F, 0.45F, 0.5F, -5.5F, 0.45F);
      this.rightArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel = new ModelRendererTurbo[1];
      this.leftLegModel[0] = new ModelRendererTurbo(this, 133, 49, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-1.5F, 1.1F, -2.0F, 11, 15, 4, 0.0F, 0.5F, 0.6F, 0.3F, -6.5F, 0.6F, 0.3F, -6.5F, 0.6F, 0.3F, 0.5F, 0.6F, 0.3F, 0.5F, -5.3F, 0.5F, -6.5F, -5.3F, 0.5F, -6.5F, -5.3F, 0.5F, 0.5F, -5.3F, 0.5F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel = new ModelRendererTurbo[1];
      this.rightLegModel[0] = new ModelRendererTurbo(this, 133, 29, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.5F, 1.1F, -2.0F, 11, 15, 4, 0.0F, 0.5F, 0.6F, 0.3F, -6.5F, 0.6F, 0.3F, -6.5F, 0.6F, 0.3F, 0.5F, 0.6F, 0.3F, 0.5F, -5.3F, 0.5F, -6.5F, -5.3F, 0.5F, -6.5F, -5.3F, 0.5F, 0.5F, -5.3F, 0.5F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
