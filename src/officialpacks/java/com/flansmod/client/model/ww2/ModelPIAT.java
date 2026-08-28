//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelPIAT extends ModelGun {
   int textureX = 128;
   int textureY = 128;

   public ModelPIAT() {
      this.gunModel = new ModelRendererTurbo[16];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 11, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 0, 18, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 22, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 0, 28, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 0, 32, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 0, 37, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 0, 44, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 0, 56, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 0, 60, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 0, 64, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 0, 93, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 0, 93, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 0, 99, this.textureX, this.textureY);
      this.gunModel[13] = new ModelRendererTurbo(this, 0, 3, this.textureX, this.textureY);
      this.gunModel[14] = new ModelRendererTurbo(this, 0, 103, this.textureX, this.textureY);
      this.gunModel[15] = new ModelRendererTurbo(this, 0, 113, this.textureX, this.textureY);
      this.gunModel[0].addBox(0.0F, 0.0F, 0.0F, 1, 4, 2, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, 0.0F, -1.0F);
      this.gunModel[1].addBox(0.0F, 0.0F, 0.0F, 6, 1, 2, 0.0F);
      this.gunModel[1].setRotationPoint(0.0F, 4.0F, -1.0F);
      this.gunModel[2].addBox(0.0F, 0.0F, 0.0F, 1, 4, 2, 0.0F);
      this.gunModel[2].setRotationPoint(5.0F, 0.0F, -1.0F);
      this.gunModel[3].addBox(0.0F, 0.0F, 0.0F, 1, 3, 1, 0.0F);
      this.gunModel[3].setRotationPoint(3.0F, 0.0F, -0.5F);
      this.gunModel[4].addBox(0.0F, 0.0F, 0.0F, 7, 2, 2, 0.0F);
      this.gunModel[4].setRotationPoint(0.0F, -2.0F, -1.0F);
      this.gunModel[5].addBox(0.0F, 0.0F, 0.0F, 22, 3, 3, 0.0F);
      this.gunModel[5].setRotationPoint(-12.0F, -5.0F, -1.5F);
      this.gunModel[6].addBox(0.0F, 0.0F, 0.0F, 2, 7, 4, 0.0F);
      this.gunModel[6].setRotationPoint(-14.0F, -7.0F, -2.0F);
      this.gunModel[7].addBox(0.0F, 0.0F, 0.0F, 4, 2, 1, 0.0F);
      this.gunModel[7].setRotationPoint(-8.0F, -7.0F, 0.5F);
      this.gunModel[8].addBox(0.0F, 0.0F, 0.0F, 3, 3, 1, 0.0F);
      this.gunModel[8].setRotationPoint(4.0F, -8.0F, 0.5F);
      this.gunModel[9].addBox(0.0F, 0.0F, 0.0F, 1, 5, 5, 0.0F);
      this.gunModel[9].setRotationPoint(10.0F, -6.0F, -2.5F);
      this.gunModel[10].addBox(0.0F, 0.0F, 0.0F, 7, 4, 1, 0.0F);
      this.gunModel[10].setRotationPoint(11.0F, -5.0F, -2.5F);
      this.gunModel[11].addBox(0.0F, 0.0F, 0.0F, 7, 4, 1, 0.0F);
      this.gunModel[11].setRotationPoint(11.0F, -5.0F, 1.5F);
      this.gunModel[12].addBox(0.0F, 0.0F, 0.0F, 7, 1, 3, 0.0F);
      this.gunModel[12].setRotationPoint(11.0F, -2.0F, -1.5F);
      this.gunModel[13].addBox(0.0F, 0.0F, 0.0F, 2, 1, 5, 0.0F);
      this.gunModel[13].setRotationPoint(16.0F, -6.0F, -2.5F);
      this.gunModel[14].addBox(0.0F, 0.0F, 0.0F, 1, 8, 1, 0.0F);
      this.gunModel[14].setRotationPoint(9.0F, -2.0F, -0.5F);
      this.gunModel[15].addBox(0.0F, 0.0F, 0.0F, 3, 1, 5, 0.0F);
      this.gunModel[15].setRotationPoint(8.0F, 6.0F, -2.5F);
      this.ammoModel = new ModelRendererTurbo[10];
      this.ammoModel[0] = new ModelRendererTurbo(this, 0, 75, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 0, 75, this.textureX, this.textureY);
      this.ammoModel[2] = new ModelRendererTurbo(this, 0, 75, this.textureX, this.textureY);
      this.ammoModel[3] = new ModelRendererTurbo(this, 0, 80, this.textureX, this.textureY);
      this.ammoModel[4] = new ModelRendererTurbo(this, 0, 83, this.textureX, this.textureY);
      this.ammoModel[5] = new ModelRendererTurbo(this, 0, 83, this.textureX, this.textureY);
      this.ammoModel[6] = new ModelRendererTurbo(this, 0, 83, this.textureX, this.textureY);
      this.ammoModel[7] = new ModelRendererTurbo(this, 0, 88, this.textureX, this.textureY);
      this.ammoModel[8] = new ModelRendererTurbo(this, 0, 88, this.textureX, this.textureY);
      this.ammoModel[9] = new ModelRendererTurbo(this, 0, 88, this.textureX, this.textureY);
      this.ammoModel[0].addBox(0.0F, -0.5F, -1.5F, 1, 1, 3, 0.0F);
      this.ammoModel[0].setRotationPoint(11.0F, -3.5F, 0.0F);
      this.ammoModel[1].addShapeBox(0.0F, -1.5F, -1.5F, 1, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[1].setRotationPoint(11.0F, -3.5F, 0.0F);
      this.ammoModel[2].addShapeBox(0.0F, 0.5F, -1.5F, 1, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      this.ammoModel[2].setRotationPoint(11.0F, -3.5F, 0.0F);
      this.ammoModel[3].addBox(1.0F, -0.5F, -0.5F, 9, 1, 1, 0.0F);
      this.ammoModel[3].setRotationPoint(11.0F, -3.5F, 0.0F);
      this.ammoModel[4].addShapeBox(0.0F, -1.5F, -1.5F, 3, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[4].setRotationPoint(14.0F, -3.5F, 0.0F);
      this.ammoModel[5].addBox(0.0F, -0.5F, -1.5F, 3, 1, 3, 0.0F);
      this.ammoModel[5].setRotationPoint(14.0F, -3.5F, 0.0F);
      this.ammoModel[6].addShapeBox(0.0F, 0.5F, -1.5F, 3, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      this.ammoModel[6].setRotationPoint(14.0F, -3.5F, 0.0F);
      this.ammoModel[7].addShapeBox(0.0F, -1.5F, -1.5F, 2, 1, 3, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[7].setRotationPoint(17.0F, -3.5F, 0.0F);
      this.ammoModel[8].addShapeBox(0.0F, -0.5F, -1.5F, 2, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[8].setRotationPoint(17.0F, -3.5F, 0.0F);
      this.ammoModel[9].addShapeBox(0.0F, 0.5F, -1.5F, 2, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F);
      this.ammoModel[9].setRotationPoint(17.0F, -3.5F, 0.0F);
      this.gunSlideDistance = 0.0F;
      this.animationType = EnumAnimationType.END_LOADED;
      this.flipAll();
      this.translateAll(0.0F, 1.0F, 0.0F);
   }
}
