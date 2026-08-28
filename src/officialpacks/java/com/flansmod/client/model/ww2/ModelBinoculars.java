//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelBinoculars extends ModelGun {
   int textureX = 32;
   int textureY = 32;

   public ModelBinoculars() {
      this.gunModel = new ModelRendererTurbo[10];
      this.gunModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 17, 17, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 1, 25, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 9, 25, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 17, 25, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 17, 25, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      this.gunModel[0].setRotationPoint(-1.5F, -3.25F, -2.0F);
      this.gunModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F);
      this.gunModel[1].setRotationPoint(-1.5F, -3.0F, -2.0F);
      this.gunModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F);
      this.gunModel[2].setRotationPoint(-1.5F, -2.75F, -2.0F);
      this.gunModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      this.gunModel[3].setRotationPoint(-1.5F, -3.25F, 1.0F);
      this.gunModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F);
      this.gunModel[4].setRotationPoint(-1.5F, -3.0F, 1.0F);
      this.gunModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 4, 1, 1, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F);
      this.gunModel[5].setRotationPoint(-1.5F, -2.75F, 1.0F);
      this.gunModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F);
      this.gunModel[6].setRotationPoint(0.0F, -3.0F, -1.0F);
      this.gunModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F);
      this.gunModel[7].setRotationPoint(0.0F, -3.0F, 0.0F);
      this.gunModel[8].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F);
      this.gunModel[8].setRotationPoint(0.0F, -3.35F, -0.5F);
      this.gunModel[9].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.5F, 0.0F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F, -0.2F, -0.25F, -0.3F);
      this.gunModel[9].setRotationPoint(0.0F, -3.1F, -0.5F);
      this.translateAll(0.0F, -4.5F, 0.0F);
      this.flipAll();
   }
}
