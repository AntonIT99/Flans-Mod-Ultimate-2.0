//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class Model50Cal extends ModelMG {
   int textureX = 64;
   int textureY = 64;

   public Model50Cal() {
      this.bipodModel = new ModelRendererTurbo[4];
      this.bipodModel[0] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.bipodModel[1] = new ModelRendererTurbo(this, 9, 17, this.textureX, this.textureY);
      this.bipodModel[2] = new ModelRendererTurbo(this, 17, 17, this.textureX, this.textureY);
      this.bipodModel[3] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.bipodModel[0].addBox(0.0F, -6.0F, -2.0F, 1, 3, 1, 0.0F);
      this.bipodModel[0].setRotationPoint(-0.5F, 0.0F, 0.0F);
      this.bipodModel[1].addShapeBox(0.0F, -3.0F, -2.0F, 1, 3, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -6.0F, 0.0F, 6.0F, 6.0F, 0.0F, 6.0F, 6.0F, 0.0F, -6.0F, -6.0F, 0.0F, -6.0F);
      this.bipodModel[1].setRotationPoint(-0.5F, 0.0F, 0.0F);
      this.bipodModel[2].addShapeBox(0.0F, -3.0F, -2.0F, 1, 3, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 6.0F, 0.0F, 6.0F, -6.0F, 0.0F, 6.0F, -6.0F, 0.0F, -6.0F, 6.0F, 0.0F, -6.0F);
      this.bipodModel[2].setRotationPoint(-0.5F, 0.0F, 0.0F);
      this.bipodModel[3].addShapeBox(0.0F, -3.0F, -2.0F, 1, 3, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 4.0F);
      this.bipodModel[3].setRotationPoint(-0.5F, 0.0F, 0.0F);
      this.gunModel = new ModelRendererTurbo[10];
      this.gunModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(-0.5F, -0.5F, 2.0F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 3.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 3.0F, 0.0F, 0.0F, 3.0F);
      this.gunModel[0].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[1].addShapeBox(-0.5F, -1.2F, 4.0F, 1, 1, 7, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 3.0F, -0.2F, -0.2F, 3.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 3.0F, -0.2F, -0.2F, 3.0F);
      this.gunModel[1].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[2].addShapeBox(-0.5F, -1.2F, -1.0F, 1, 1, 5, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[2].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[3].addShapeBox(-1.0F, -1.7F, -10.0F, 2, 2, 9, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[3].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[4].addShapeBox(0.0F, -1.5F, -7.0F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[4].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[5].addShapeBox(-1.0F, -1.7F, -14.0F, 2, 2, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F);
      this.gunModel[5].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[6].addShapeBox(-2.0F, -2.2F, -16.0F, 1, 3, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[6].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[7].addShapeBox(1.0F, -2.2F, -16.0F, 1, 3, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[7].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[8].addShapeBox(1.0F, -1.7F, -15.0F, 0, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[8].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.gunModel[9].addShapeBox(-1.0F, -1.7F, -15.0F, 0, 2, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[9].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.ammoModel[0].addShapeBox(-5.0F, -1.2F, -7.0F, 4, 3, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, -6.0F, -1.5F);
      this.flipAll();
   }
}
