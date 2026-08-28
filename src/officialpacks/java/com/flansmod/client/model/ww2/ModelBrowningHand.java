//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelBrowningHand extends ModelGun {
   int textureX = 32;
   int textureY = 32;

   public ModelBrowningHand() {
      this.gunModel = new ModelRendererTurbo[8];
      this.gunModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 17, 25, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 25, 25, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(-6.0F, -2.3F, -1.0F, 7, 2, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F);
      this.gunModel[0].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[1].addShapeBox(0.5F, -1.8F, -0.5F, 10, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[1].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[2].addShapeBox(1.0F, -1.8F, -0.5F, 10, 1, 1, 0.0F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F);
      this.gunModel[2].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[3].addShapeBox(-7.5F, -1.3F, -0.5F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[3].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[4].addShapeBox(-7.5F, -1.3F, -0.5F, 1, 3, 1, 0.0F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 1.0F, -0.2F, -0.2F, -1.0F, -0.2F, -0.2F, -1.0F, -0.2F, -0.2F, 1.0F, -0.2F, -0.2F);
      this.gunModel[4].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[5].addShapeBox(-0.1F, -3.0F, -0.5F, 1, 1, 1, 0.0F, -0.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.9F, 0.0F, 0.0F, -0.9F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.9F, 0.0F, 0.0F);
      this.gunModel[5].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[6].addShapeBox(-5.5F, -2.5F, -0.5F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[6].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.gunModel[7].addShapeBox(-5.5F, -3.25F, -0.5F, 1, 1, 1, 0.0F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F, -0.4F, -0.2F, -0.2F);
      this.gunModel[7].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 1, 25, this.textureX, this.textureY);
      this.ammoModel[0].addShapeBox(-4.5F, -1.7F, 1.0F, 4, 2, 3, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.5F, -0.6F, 0.0F, 0.5F, -0.6F, 0.5F, 0.0F, -0.6F, 0.5F, 0.0F, -0.6F, 0.5F, 0.5F, -0.6F, 0.5F, 0.5F);
      this.ammoModel[0].setRotationPoint(6.0F, -1.0F, 0.0F);
      this.animationType = EnumAnimationType.SIDE_CLIP;
      this.translateAll(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
