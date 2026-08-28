//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelMG42 extends ModelMG {
   int textureX = 64;
   int textureY = 64;

   public ModelMG42() {
      this.bipodModel = new ModelRendererTurbo[3];
      this.bipodModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.bipodModel[1] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.bipodModel[2] = new ModelRendererTurbo(this, 17, 17, this.textureX, this.textureY);
      this.bipodModel[0].addBox(0.0F, 0.0F, 0.0F, 1, 1, 1, 0.0F);
      this.bipodModel[0].setRotationPoint(-0.5F, -5.5F, 0.0F);
      this.bipodModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 1, 0.0F, 2.0F, -0.5F, -0.2F, -2.5F, 0.0F, -0.2F, -2.5F, 0.0F, -0.2F, 2.0F, -0.5F, -0.2F, -0.3F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, -0.3F, 0.0F, -0.2F);
      this.bipodModel[1].setRotationPoint(2.0F, -5.0F, 0.0F);
      this.bipodModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 1, 0.0F, -2.5F, 0.0F, -0.2F, 2.0F, -0.5F, -0.2F, 2.0F, -0.5F, -0.2F, -2.5F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, -0.3F, 0.0F, -0.2F, -0.3F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F);
      this.bipodModel[2].setRotationPoint(-3.0F, -5.0F, 0.0F);
      this.gunModel = new ModelRendererTurbo[13];
      this.gunModel[0] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 9, 9, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 33, 9, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 41, 9, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(-0.5F, -1.0F, -10.0F, 1, 1, 12, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[1].addShapeBox(-0.5F, -1.0F, -13.0F, 1, 1, 3, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F);
      this.gunModel[1].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[2].addShapeBox(-0.5F, -1.0F, -18.0F, 1, 1, 5, 0.0F, -0.2F, 0.2F, 0.0F, -0.2F, 0.2F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F);
      this.gunModel[2].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[3].addShapeBox(-0.5F, -1.0F, 3.0F, 1, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F);
      this.gunModel[3].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[4].addShapeBox(-0.5F, -1.0F, 2.0F, 1, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F);
      this.gunModel[4].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[5].addShapeBox(1.0F, -1.2F, -11.0F, 1, 2, 1, 0.0F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F);
      this.gunModel[5].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[6].addShapeBox(0.5F, -1.2F, -11.0F, 1, 2, 1, 0.0F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F, 0.0F, -0.2F, -0.4F);
      this.gunModel[6].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[7].addShapeBox(-0.5F, -2.0F, 1.0F, 1, 1, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F);
      this.gunModel[7].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[8].addShapeBox(-1.0F, -1.7F, -9.0F, 2, 1, 2, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F);
      this.gunModel[8].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[9].addShapeBox(-0.5F, -1.5F, -15.9F, 1, 1, 1, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.2F, -0.2F, 0.0F, -0.2F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[9].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[10].addShapeBox(-0.5F, -1.0F, -20.0F, 1, 1, 2, 0.0F, 0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.0F, -0.2F, 0.2F, 0.0F, -0.2F, 0.2F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, -0.2F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F);
      this.gunModel[10].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[11].addShapeBox(-0.5F, 0.5F, -17.0F, 1, 2, 1, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 1.0F, -0.2F, 0.0F, 1.0F, -0.2F, 0.0F, -1.0F, -0.2F, 0.0F, -1.0F);
      this.gunModel[11].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.gunModel[12].addShapeBox(-0.5F, -1.0F, -10.0F, 1, 1, 12, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F);
      this.gunModel[12].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.ammoModel = new ModelRendererTurbo[3];
      this.ammoModel[0] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.ammoModel[1] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.ammoModel[2] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.ammoModel[0].addShapeBox(-5.0F, -1.0F, -12.5F, 4, 3, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.ammoModel[0].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.ammoModel[1].addShapeBox(-2.1F, -1.06F, -12.5F, 3, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.ammoModel[1].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.ammoModel[2].addShapeBox(-5.0F, -1.06F, -12.5F, 4, 3, 2, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F);
      this.ammoModel[2].setRotationPoint(0.0F, -5.5F, 0.5F);
      this.flipAll();
   }
}
