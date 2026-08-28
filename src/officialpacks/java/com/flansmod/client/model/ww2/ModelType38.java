//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelType38 extends ModelGun {
   int textureX = 512;
   int textureY = 512;

   public ModelType38() {
      this.gunModel = new ModelRendererTurbo[18];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 220, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 0, 230, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 240, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 0, 260, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 80, 250, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 0, 230, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 0, 230, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 0, 100, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 0, 112, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 0, 112, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 0, 120, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 0, 120, this.textureX, this.textureY);
      this.gunModel[13] = new ModelRendererTurbo(this, 0, 100, this.textureX, this.textureY);
      this.gunModel[14] = new ModelRendererTurbo(this, 0, 130, this.textureX, this.textureY);
      this.gunModel[15] = new ModelRendererTurbo(this, 30, 130, this.textureX, this.textureY);
      this.gunModel[16] = new ModelRendererTurbo(this, 50, 130, this.textureX, this.textureY);
      this.gunModel[17] = new ModelRendererTurbo(this, 50, 130, this.textureX, this.textureY);
      this.gunModel[0].addBox(0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F);
      this.gunModel[0].setRotationPoint(-2.5F, -1.7F, -1.0F);
      this.gunModel[0].rotateAngleZ = 0.3490658F;
      this.gunModel[1].addBox(0.0F, 0.0F, 0.0F, 12, 1, 2, 0.0F);
      this.gunModel[1].setRotationPoint(1.0F, -3.0F, -1.0F);
      this.gunModel[2].addBox(0.0F, 0.0F, 0.0F, 4, 2, 2, 0.0F);
      this.gunModel[2].setRotationPoint(-3.5F, -2.0F, -1.0F);
      this.gunModel[2].rotateAngleZ = -0.1745329F;
      this.gunModel[3].addBox(0.0F, 0.0F, 0.0F, 8, 2, 2, 0.0F);
      this.gunModel[3].setRotationPoint(-11.0F, -2.0F, -1.0F);
      this.gunModel[4].addBox(0.0F, 0.0F, 0.0F, 8, 2, 2, 0.0F);
      this.gunModel[4].setRotationPoint(-11.0F, -0.2F, -1.0F);
      this.gunModel[4].rotateAngleZ = 0.2094395F;
      this.gunModel[5].addBox(0.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      this.gunModel[5].setRotationPoint(2.5F, -1.0F, -0.5F);
      this.gunModel[6].addBox(0.0F, 0.0F, 0.0F, 4, 1, 2, 0.0F);
      this.gunModel[6].setRotationPoint(1.0F, -2.0F, -1.0F);
      this.gunModel[7].addBox(0.0F, 0.0F, 0.0F, 8, 1, 2, 0.0F);
      this.gunModel[7].setRotationPoint(4.7F, -2.0F, -1.0F);
      this.gunModel[7].rotateAngleZ = 0.122173F;
      this.gunModel[8].addBox(0.0F, 0.0F, 0.0F, 1, 2, 2, 0.0F);
      this.gunModel[8].setRotationPoint(13.0F, -4.0F, -1.0F);
      this.gunModel[9].addBox(0.0F, 0.0F, 0.0F, 22, 1, 1, 0.0F);
      this.gunModel[9].setRotationPoint(0.5F, -3.8F, -0.3F);
      this.gunModel[10].addBox(0.0F, 0.0F, 0.0F, 22, 1, 1, 0.0F);
      this.gunModel[10].setRotationPoint(0.5F, -3.8F, -0.7F);
      this.gunModel[11].addBox(0.0F, 0.0F, 0.0F, 9, 1, 1, 0.0F);
      this.gunModel[11].setRotationPoint(13.5F, -3.1F, -0.2F);
      this.gunModel[12].addBox(0.0F, 0.0F, 0.0F, 9, 1, 1, 0.0F);
      this.gunModel[12].setRotationPoint(13.5F, -3.1F, -0.8F);
      this.gunModel[13].addBox(0.0F, 0.0F, 0.0F, 1, 2, 2, 0.0F);
      this.gunModel[13].setRotationPoint(22.0F, -4.0F, -1.0F);
      this.gunModel[14].addBox(0.0F, 0.0F, 0.0F, 7, 1, 1, 0.0F);
      this.gunModel[14].setRotationPoint(22.0F, -3.7F, -0.5F);
      this.gunModel[15].addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.gunModel[15].setRotationPoint(3.0F, -3.7F, -1.5F);
      this.gunModel[16].addBox(0.0F, 0.0F, 0.0F, 1, 1, 0, 0.0F);
      this.gunModel[16].setRotationPoint(26.0F, -4.5F, 0.5F);
      this.gunModel[17].addBox(0.0F, 0.0F, 0.0F, 1, 1, 0, 0.0F);
      this.gunModel[17].setRotationPoint(26.0F, -4.5F, -0.5F);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.ammoModel[0].addBox(3.0F, -3.5F, -0.5F, 3, 2, 1, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.gunSlideDistance = 0.0F;
      this.animationType = EnumAnimationType.RIFLE;
      this.flipAll();
      this.translateAll(0.0F, 2.0F, 0.0F);
   }
}
