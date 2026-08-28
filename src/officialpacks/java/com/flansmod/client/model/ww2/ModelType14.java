//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelType14 extends ModelGun {
   int textureX = 512;
   int textureY = 512;

   public ModelType14() {
      this.gunModel = new ModelRendererTurbo[14];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 0, 250, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 0, 306, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 0, 265, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 0, 275, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 0, 285, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 0, 295, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 0, 265, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 0, 285, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 0, 305, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 0, 265, this.textureX, this.textureY);
      this.gunModel[13] = new ModelRendererTurbo(this, 0, 265, this.textureX, this.textureY);
      this.gunModel[0].addBox(0.0F, 0.0F, -0.3F, 1, 5, 1, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[0].rotateAngleZ = (float) (-Math.PI / 12);
      this.gunModel[1].addBox(0.0F, 0.0F, -0.7F, 1, 5, 1, 0.0F);
      this.gunModel[1].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[1].rotateAngleZ = (float) (-Math.PI / 12);
      this.gunModel[2].addBox(0.3F, 0.0F, -0.3F, 1, 5, 1, 0.0F);
      this.gunModel[2].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[2].rotateAngleZ = (float) (-Math.PI / 12);
      this.gunModel[3].addBox(0.3F, 0.0F, -0.7F, 1, 5, 1, 0.0F);
      this.gunModel[3].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[3].rotateAngleZ = (float) (-Math.PI / 12);
      this.gunModel[4].addBox(0.2F, 0.5F, -0.5F, 1, 4, 1, 0.0F);
      this.gunModel[4].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[4].rotateAngleZ = (float) (-Math.PI / 12);
      this.gunModel[5].addBox(0.0F, 0.0F, 0.0F, 3, 1, 1, 0.0F);
      this.gunModel[5].setRotationPoint(-1.2F, -3.0F, -0.6F);
      this.gunModel[6].addBox(0.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      this.gunModel[6].setRotationPoint(0.3F, -2.0F, -0.5F);
      this.gunModel[7].addBox(0.0F, 0.0F, 0.0F, 5, 1, 1, 0.0F);
      this.gunModel[7].setRotationPoint(0.5F, -3.3F, -0.5F);
      this.gunModel[8].addBox(0.0F, 0.0F, 0.0F, 2, 1, 1, 0.0F);
      this.gunModel[8].setRotationPoint(-1.5F, -3.3F, -0.5F);
      this.gunModel[9].addBox(0.0F, 0.0F, 0.0F, 3, 1, 1, 0.0F);
      this.gunModel[9].setRotationPoint(-1.2F, -3.5F, -0.4F);
      this.gunModel[10].addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.gunModel[10].setRotationPoint(-1.0F, -2.5F, -0.5F);
      this.gunModel[11].addBox(0.0F, 0.0F, 0.0F, 1, 1, 0, 0.0F);
      this.gunModel[11].setRotationPoint(4.2F, -3.6F, 0.0F);
      this.gunModel[12].addBox(0.0F, 0.0F, 0.0F, 3, 1, 1, 0.0F);
      this.gunModel[12].setRotationPoint(-1.2F, -3.5F, -0.6F);
      this.gunModel[13].addBox(0.0F, 0.0F, 0.0F, 3, 1, 1, 0.0F);
      this.gunModel[13].setRotationPoint(-1.2F, -3.0F, -0.4F);
      this.translateAll(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
