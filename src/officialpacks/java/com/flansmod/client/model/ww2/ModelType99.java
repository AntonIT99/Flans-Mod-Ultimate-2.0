//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelType99 extends ModelMG {
   int textureX = 64;
   int textureY = 32;

   public ModelType99() {
      this.bipodModel = new ModelRendererTurbo[2];
      this.bipodModel[0] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.bipodModel[1] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.gunModel = new ModelRendererTurbo[3];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 4, 0, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 18, this.textureX, this.textureY);
      this.ammoModel = new ModelRendererTurbo[2];
      this.ammoModel[0] = new ModelRendererTurbo(this, 4, 5, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 4, 5, this.textureX, this.textureY);
      this.bipodModel[0].addBox(0.0F, 0.0F, 0.0F, 1, 8, 1, 0.0F);
      this.bipodModel[0].rotateAngleZ = (float) (Math.PI / 4);
      this.bipodModel[0].setRotationPoint(5.66F, 0.0F, 0.0F);
      this.bipodModel[1].addBox(-1.0F, 0.0F, 0.0F, 1, 8, 1, 0.0F);
      this.bipodModel[1].rotateAngleZ = (float) (-Math.PI / 4);
      this.bipodModel[1].setRotationPoint(-5.66F, 0.0F, 0.0F);
      this.gunModel[0].addBox(-1.0F, -1.0F, -2.0F, 2, 2, 16, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.gunModel[1].addBox(-1.0F, -4.0F, 8.0F, 2, 3, 2, 0.0F);
      this.gunModel[1].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.gunModel[2].addBox(-0.5F, -0.5F, -8.0F, 1, 1, 6, 0.0F);
      this.gunModel[2].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.ammoModel[0].addBox(0.0F, 1.0F, 5.0F, 1, 3, 2, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.ammoModel[1].addBox(0.0F, 4.0F, 4.5F, 1, 2, 2, 0.0F);
      this.ammoModel[1].setRotationPoint(0.0F, 6.0F, 0.0F);
   }
}
