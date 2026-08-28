//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelMG;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelDP28 extends ModelMG {
   public ModelDP28() {
      this.bipodModel = new ModelRendererTurbo[2];
      this.bipodModel[0] = new ModelRendererTurbo(this, 0, 0);
      this.bipodModel[0].addBox(0.0F, 0.0F, 0.0F, 1, 8, 1);
      this.bipodModel[0].rotateAngleZ = (float) (Math.PI / 4);
      this.bipodModel[0].setRotationPoint(5.66F, 0.0F, 0.0F);
      this.bipodModel[1] = new ModelRendererTurbo(this, 0, 0);
      this.bipodModel[1].addBox(-1.0F, 0.0F, 0.0F, 1, 8, 1);
      this.bipodModel[1].rotateAngleZ = (float) (-Math.PI / 4);
      this.bipodModel[1].setRotationPoint(-5.66F, 0.0F, 0.0F);
      this.gunModel = new ModelRendererTurbo[3];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 0);
      this.gunModel[0].addBox(-1.0F, -1.0F, -2.0F, 2, 2, 16);
      this.gunModel[0].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.gunModel[1] = new ModelRendererTurbo(this, 4, 0);
      this.gunModel[1].addBox(-1.0F, -2.0F, 14.0F, 2, 3, 2);
      this.gunModel[1].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 18);
      this.gunModel[2].addBox(-0.5F, -0.5F, -8.0F, 1, 1, 6);
      this.gunModel[2].setRotationPoint(0.0F, 6.0F, 0.0F);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 20, 0);
      this.ammoModel[0].addBox(-3.0F, 1.0F, 1.0F, 6, 1, 6);
      this.ammoModel[0].setRotationPoint(0.0F, 6.0F, 0.0F);
   }
}
