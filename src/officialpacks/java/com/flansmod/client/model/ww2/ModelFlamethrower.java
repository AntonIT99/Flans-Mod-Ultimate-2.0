//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelFlamethrower extends ModelGun {
   int textureX = 64;
   int textureY = 16;

   public ModelFlamethrower() {
      this.gunModel = new ModelRendererTurbo[11];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 14, 0, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 20, 0, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 0, 4, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 28, 0, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 0, 6, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 36, 0, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 22, 3, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 8, 6, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 30, 3, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 0, 10, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 5, 2, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[0].setRotationPoint(0.0F, -4.0F, -1.0F);
      this.gunModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, -0.25F, 0.0F, 0.5F, -0.25F, 0.0F, 0.5F, -0.25F, 0.0F, 0.5F, -0.25F);
      this.gunModel[1].setRotationPoint(3.25F, -2.0F, -1.0F);
      this.gunModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.5F, -0.25F, -0.25F, 0.5F, -0.25F, -0.25F, 0.5F, -0.25F, -0.25F, 0.5F, -0.25F);
      this.gunModel[2].setRotationPoint(0.0F, -2.0F, -1.0F);
      this.gunModel[3].addBox(0.0F, 0.0F, 0.0F, 10, 1, 1, 0.0F);
      this.gunModel[3].setRotationPoint(-10.0F, -3.5F, -0.5F);
      this.gunModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, -0.25F, -0.8F, -0.25F, -0.25F, -0.8F, -0.25F, -0.25F, -0.8F, -0.25F, -0.25F, -0.8F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F);
      this.gunModel[4].setRotationPoint(1.5F, -1.5F, -1.0F);
      this.gunModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 2, 2, 2, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, 0.0F, 0.5F, -0.25F, -0.5F, 0.5F, -0.25F, -0.5F, 0.5F, -0.25F, 0.0F, 0.5F, -0.25F);
      this.gunModel[5].setRotationPoint(0.0F, -0.5F, -1.0F);
      this.gunModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 2, 1, 2, 0.0F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, -0.5F, -0.25F, -0.25F, -0.5F, -0.25F, -0.25F, -0.5F, -0.25F, -0.25F, -0.5F, -0.25F);
      this.gunModel[6].setRotationPoint(1.5F, -2.0F, -1.0F);
      this.gunModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 1, 3, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[7].setRotationPoint(5.0F, -4.5F, -1.5F);
      this.gunModel[8].addShapeBox(0.0F, 0.0F, 0.0F, 2, 2, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, 0.0F);
      this.gunModel[8].setRotationPoint(8.0F, -4.0F, -1.0F);
      this.gunModel[9].addShapeBox(0.0F, 0.0F, 0.0F, 2, 3, 3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.25F, 0.0F, -0.25F, -0.25F, 0.0F, 0.0F, 0.0F);
      this.gunModel[9].setRotationPoint(6.0F, -4.5F, -1.5F);
      this.gunModel[10].addShapeBox(0.0F, 0.0F, 0.0F, 3, 2, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.gunModel[10].setRotationPoint(-13.0F, -4.0F, -1.0F);
      this.barrelAttachPoint = new Vector3f(0.0F, 0.0F, 0.0F);
      this.stockAttachPoint = new Vector3f(0.0F, 0.0F, 0.0F);
      this.scopeAttachPoint = new Vector3f(0.0F, 0.0F, 0.0F);
      this.gripAttachPoint = new Vector3f(0.0F, 0.0F, 0.0F);
      this.gunSlideDistance = 0.0F;
      this.animationType = EnumAnimationType.NONE;
      this.translateAll(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
