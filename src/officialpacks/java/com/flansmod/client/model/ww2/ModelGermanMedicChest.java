//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelGermanMedicChest extends ModelCustomArmour {
   int textureX = 256;
   int textureY = 256;

   public ModelGermanMedicChest() {
      this.bodyModel = new ModelRendererTurbo[17];
      this.bodyModel[0] = new ModelRendererTurbo(this, 1, 25, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 97, 17, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 49, 25, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 73, 33, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 97, 33, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 1, 41, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 17, 41, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 49, 41, this.textureX, this.textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 81, 49, this.textureX, this.textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 105, 49, this.textureX, this.textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 1, 57, this.textureX, this.textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 113, 65, this.textureX, this.textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 1, 73, this.textureX, this.textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 113, 73, this.textureX, this.textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 1, 135, this.textureX, this.textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 1, 135, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(-4.5F, 7.3F, -2.2F, 16, 8, 5, 0.0F, 0.1F, 0.0F, 0.1F, -6.9F, 0.0F, 0.1F, -6.9F, 0.0F, -0.51F, 0.1F, 0.0F, -0.5F, 0.1F, -6.7F, 0.1F, -6.9F, -6.7F, 0.1F, -6.9F, -6.7F, -0.5F, 0.1F, -6.7F, -0.5F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(-3.3F, 1.3F, -2.2F, 8, 13, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-3.3F, 0.3F, -2.2F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(2.3F, 0.3F, -2.2F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(2.3F, 1.3F, -2.2F, 8, 13, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(-2.2F, 8.5F, -2.5F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(-3.3F, 0.0F, -2.2F, 8, 7, 5, 0.0F, 0.0F, 0.2F, 0.1F, -7.0F, 0.2F, 0.1F, -7.0F, 0.2F, -0.6F, 0.0F, 0.2F, -0.6F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, -0.6F, 0.0F, -6.7F, -0.6F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(2.3F, 0.0F, -2.2F, 8, 7, 5, 0.0F, 0.0F, 0.2F, 0.1F, -7.0F, 0.2F, 0.1F, -7.0F, 0.2F, -0.6F, 0.0F, 0.2F, -0.6F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, -0.6F, 0.0F, -6.7F, -0.6F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[8].addShapeBox(-3.3F, 0.4F, 1.8F, 8, 10, 1, 0.0F, 0.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, -7.0F, 0.1F, -0.6F, 0.0F, 0.1F, -0.6F, -3.0F, -6.0F, 0.1F, -5.0F, -7.7F, 0.1F, -5.0F, -7.7F, -0.6F, -3.0F, -6.0F, -0.6F);
      this.bodyModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[9].addShapeBox(-4.7F, 0.4F, 1.8F, 8, 10, 1, 0.0F, -7.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -0.6F, -7.0F, 0.1F, -0.6F, -5.0F, -7.7F, 0.1F, -3.0F, -6.0F, 0.1F, -3.0F, -6.0F, -0.6F, -5.0F, -7.7F, -0.6F);
      this.bodyModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[10].addShapeBox(-0.5F, 2.7F, 1.8F, 8, 12, 1, 0.0F, 0.1F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, -0.51F, 0.1F, 0.0F, -0.5F, 0.1F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, -0.5F, 0.1F, -6.7F, -0.5F);
      this.bodyModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[11].addShapeBox(-4.7F, 8.5F, -2.5F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      this.bodyModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[12].addShapeBox(0.3F, 8.5F, -2.5F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      this.bodyModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[13].addShapeBox(2.8F, 8.5F, -2.5F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, -2.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F);
      this.bodyModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[14].addShapeBox(-4.5F, 0.0F, -2.0F, 16, 18, 4, 0.0F, 0.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, -6.0F, 0.1F, -7.0F, -6.0F, 0.1F, -7.0F, -6.0F, 0.1F, 0.0F, -6.0F, 0.1F);
      this.bodyModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[15].addShapeBox(-4.5F, 0.0F, -3.1F, 16, 18, 1, 0.0F, 0.0F, 0.1F, -0.3F, -7.0F, 0.1F, -0.3F, -7.0F, 0.1F, -0.3F, 0.0F, 0.1F, -0.3F, 0.0F, -6.0F, -0.3F, -7.0F, -6.0F, -0.3F, -7.0F, -6.0F, -0.3F, 0.0F, -6.0F, -0.3F);
      this.bodyModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[16].addShapeBox(-4.5F, 0.0F, 2.1F, 16, 18, 1, 0.0F, 0.0F, 0.1F, -0.3F, -7.0F, 0.1F, -0.3F, -7.0F, 0.1F, -0.3F, 0.0F, 0.1F, -0.3F, 0.0F, -6.0F, -0.3F, -7.0F, -6.0F, -0.3F, -7.0F, -6.0F, -0.3F, 0.0F, -6.0F, -0.3F);
      this.bodyModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel = new ModelRendererTurbo[4];
      this.leftArmModel[0] = new ModelRendererTurbo(this, 89, 65, this.textureX, this.textureY);
      this.leftArmModel[1] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.leftArmModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.leftArmModel[3] = new ModelRendererTurbo(this, 119, 121, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, 4.8F, -2.0F, 4, 1, 4, 0.0F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[1].addShapeBox(0.2F, -2.6F, -0.5F, 2, 1, 1, 0.0F, 0.4F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F);
      this.leftArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[2].addShapeBox(-1.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.2F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, 0.2F, 0.0F, 0.15F, 0.2F, -8.0F, 0.15F, -3.8F, -8.0F, 0.15F, -3.8F, -8.0F, 0.15F, 0.2F, -8.0F, 0.15F);
      this.leftArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[3].addShapeBox(2.3F, 0.0F, -1.5F, 1, 64, 64, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, -61.0F, 0.0F, -61.0F, -61.0F);
      this.leftArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel = new ModelRendererTurbo[4];
      this.rightArmModel[0] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.rightArmModel[1] = new ModelRendererTurbo(this, 65, 65, this.textureX, this.textureY);
      this.rightArmModel[2] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.rightArmModel[3] = new ModelRendererTurbo(this, 119, 121, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.2F, -2.0F, 8, 15, 4, 0.0F, 0.2F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, -3.8F, 0.0F, 0.15F, 0.2F, 0.0F, 0.15F, 0.2F, -7.0F, 0.15F, -3.8F, -7.0F, 0.15F, -3.8F, -7.0F, 0.15F, 0.2F, -7.0F, 0.15F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[1].addShapeBox(-3.0F, 5.8F, -2.0F, 4, 1, 4, 0.0F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F, 0.3F, 0.0F, 0.3F);
      this.rightArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[2].addShapeBox(-2.2F, -2.6F, -0.5F, 2, 1, 1, 0.0F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.1F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F, 0.4F, 0.0F, 0.15F);
      this.rightArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[3].addShapeBox(-3.3F, 0.0F, -1.5F, 1, 64, 64, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, 0.0F, 0.0F, -61.0F, -61.0F, 0.0F, -61.0F, -61.0F);
      this.rightArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
