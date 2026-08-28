//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelMG42Hand extends ModelGun {
   int textureX = 64;
   int textureY = 64;

   public ModelMG42Hand() {
      this.gunModel = new ModelRendererTurbo[16];
      this.gunModel[0] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 33, 9, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 41, 9, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 9, 17, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 17, 17, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 1, 25, this.textureX, this.textureY);
      this.gunModel[13] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.gunModel[14] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.gunModel[15] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(1.0F, -1.3F, -0.5F, 12, 1, 1, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F);
      this.gunModel[0].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[1].addShapeBox(-2.0F, -1.3F, -0.5F, 3, 1, 1, 0.0F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F);
      this.gunModel[1].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[2].addShapeBox(-7.0F, -1.3F, -0.5F, 5, 1, 1, 0.0F, 0.0F, 0.2F, -0.2F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.2F, -0.2F, 0.0F, 0.5F, -0.2F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, -0.2F);
      this.gunModel[2].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[3].addShapeBox(14.0F, -1.3F, -0.5F, 1, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F);
      this.gunModel[3].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[4].addShapeBox(13.0F, -1.3F, -0.5F, 1, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F);
      this.gunModel[4].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[5].addShapeBox(0.0F, -1.5F, -2.0F, 1, 2, 1, 0.0F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F);
      this.gunModel[5].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[6].addShapeBox(0.0F, -1.5F, -1.5F, 1, 2, 1, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F);
      this.gunModel[6].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[7].addShapeBox(12.0F, -2.3F, -0.5F, 1, 1, 1, 0.0F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.gunModel[7].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[8].addShapeBox(2.0F, -2.0F, -1.0F, 2, 1, 2, 0.0F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F);
      this.gunModel[8].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[9].addShapeBox(-4.9F, -1.8F, -0.5F, 1, 1, 1, 0.0F, 0.0F, 0.0F, -0.1F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[9].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[10].addShapeBox(-9.0F, -1.3F, -0.5F, 2, 1, 1, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.5F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.5F, -0.2F, 0.0F, 0.5F, -0.2F, 0.0F, 1.0F, 0.0F);
      this.gunModel[10].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[11].addShapeBox(-6.0F, 0.2F, -0.5F, 1, 2, 1, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 1.0F, 0.0F, -0.2F, -1.0F, 0.0F, -0.2F, -1.0F, 0.0F, -0.2F, 1.0F, 0.0F, -0.2F);
      this.gunModel[11].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[12].addShapeBox(1.0F, -1.3F, -0.5F, 12, 1, 1, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.4F);
      this.gunModel[12].setRotationPoint(4.0F, -2.0F, 0.0F);
      this.gunModel[13].addShapeBox(-1.0F, -1.5F, -2.0F, 2, 3, 4, 0.0F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F, -1.5F, -1.0F, -1.5F);
      this.gunModel[13].setRotationPoint(15.0F, -2.5F, 0.0F);
      this.gunModel[13].rotateAngleZ = 0.29670596F;
      this.gunModel[14].addShapeBox(-1.0F, 0.5F, -2.0F, 2, 3, 4, 0.0F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F);
      this.gunModel[14].setRotationPoint(15.0F, -2.25F, 0.0F);
      this.gunModel[14].rotateAngleY = (float) (-Math.PI / 9);
      this.gunModel[14].rotateAngleZ = (float) (-Math.PI * 5.0 / 12.0);
      this.gunModel[15].addShapeBox(-1.0F, 0.5F, -2.0F, 2, 3, 4, 0.0F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 0.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F, -0.8F, 2.0F, -1.8F);
      this.gunModel[15].setRotationPoint(15.0F, -2.25F, 0.0F);
      this.gunModel[15].rotateAngleY = (float) (Math.PI / 9);
      this.gunModel[15].rotateAngleZ = (float) (-Math.PI * 5.0 / 12.0);
      this.ammoModel = new ModelRendererTurbo[3];
      this.ammoModel[0] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.ammoModel[2] = new ModelRendererTurbo(this, 49, 17, this.textureX, this.textureY);
      this.ammoModel[0].addShapeBox(2.5F, -2.8F, 1.0F, 2, 3, 4, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.ammoModel[1].addShapeBox(2.5F, -3.5F, 0.4F, 2, 1, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.ammoModel[2].addShapeBox(2.5F, -2.9F, 1.0F, 2, 3, 4, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F);
      this.ammoModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.animationType = EnumAnimationType.SIDE_CLIP;
      this.translateAll(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
