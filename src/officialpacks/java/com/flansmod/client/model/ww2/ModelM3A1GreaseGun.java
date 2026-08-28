//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelM3A1GreaseGun extends ModelGun {
   int textureX = 512;
   int textureY = 512;

   public ModelM3A1GreaseGun() {
      this.gunModel = new ModelRendererTurbo[9];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 300, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 0, 290, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 280, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 0, 270, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 0, 260, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 15, 250, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 0, 240, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 0, 230, this.textureX, this.textureY);
      this.gunModel[0].addBox(0.0F, 0.0F, 0.0F, 2, 4, 2, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, -4.0F, -1.0F);
      this.gunModel[0].rotateAngleZ = -0.1919862F;
      this.gunModel[1].addBox(0.0F, 0.0F, 0.0F, 13, 2, 2, 0.0F);
      this.gunModel[1].setRotationPoint(-1.0F, -5.0F, -1.0F);
      this.gunModel[2].addBox(0.0F, 0.0F, 0.0F, 5, 1, 1, 0.0F);
      this.gunModel[2].setRotationPoint(12.0F, -4.5F, -0.5F);
      this.gunModel[3].addBox(0.0F, 0.0F, 0.0F, 7, 1, 1, 0.0F);
      this.gunModel[3].setRotationPoint(1.0F, -3.0F, -0.5F);
      this.gunModel[4].addBox(0.0F, 0.0F, 0.0F, 3, 1, 1, 0.0F);
      this.gunModel[4].setRotationPoint(4.0F, -2.5F, -0.5F);
      this.gunModel[5].addBox(0.0F, 0.0F, 0.0F, 5, 0, 1, 0.0F);
      this.gunModel[5].setRotationPoint(-6.0F, -3.5F, -0.5F);
      this.gunModel[6].addBox(0.0F, 0.0F, 0.0F, 0, 3, 1, 0.0F);
      this.gunModel[6].setRotationPoint(-5.9F, -4.0F, -0.5F);
      this.gunModel[6].rotateAngleZ = -0.1047198F;
      this.gunModel[7].addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.gunModel[7].setRotationPoint(10.5F, -5.7F, -0.5F);
      this.gunModel[7].rotateAngleZ = 0.122173F;
      this.gunModel[8].addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.gunModel[8].setRotationPoint(0.0F, -5.5F, -0.5F);
      this.ammoModel = new ModelRendererTurbo[2];
      this.ammoModel[0] = new ModelRendererTurbo(this, 10, 230, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 15, 230, this.textureX, this.textureY);
      this.ammoModel[0].addBox(8.0F, -3.0F, -0.5F, 1, 6, 1, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.ammoModel[1].addBox(8.5F, -3.0F, -0.5F, 1, 6, 1, 0.0F);
      this.ammoModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.gunSlideDistance = 0.0F;
      this.animationType = EnumAnimationType.BOTTOM_CLIP;
      this.flipAll();
      this.translateAll(0.0F, 1.0F, 0.0F);
   }
}
