//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelThompson extends ModelGun {
   int textureX = 128;
   int textureY = 128;

   public ModelThompson() {
      this.gunModel = new ModelRendererTurbo[17];
      this.gunModel[0] = new ModelRendererTurbo(this, 89, 1, this.textureX, this.textureY);
      this.gunModel[1] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.gunModel[2] = new ModelRendererTurbo(this, 57, 17, this.textureX, this.textureY);
      this.gunModel[3] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.gunModel[4] = new ModelRendererTurbo(this, 1, 33, this.textureX, this.textureY);
      this.gunModel[5] = new ModelRendererTurbo(this, 1, 41, this.textureX, this.textureY);
      this.gunModel[6] = new ModelRendererTurbo(this, 105, 17, this.textureX, this.textureY);
      this.gunModel[7] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.gunModel[8] = new ModelRendererTurbo(this, 113, 9, this.textureX, this.textureY);
      this.gunModel[9] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.gunModel[10] = new ModelRendererTurbo(this, 41, 41, this.textureX, this.textureY);
      this.gunModel[11] = new ModelRendererTurbo(this, 49, 25, this.textureX, this.textureY);
      this.gunModel[12] = new ModelRendererTurbo(this, 105, 25, this.textureX, this.textureY);
      this.gunModel[13] = new ModelRendererTurbo(this, 41, 33, this.textureX, this.textureY);
      this.gunModel[14] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.gunModel[15] = new ModelRendererTurbo(this, 105, 25, this.textureX, this.textureY);
      this.gunModel[16] = new ModelRendererTurbo(this, 50, 15, this.textureX, this.textureY);
      this.gunModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 5, 10, 4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F);
      this.gunModel[0].setRotationPoint(1.0F, -7.0F, -2.0F);
      this.gunModel[1].addBox(0.0F, 0.0F, 0.0F, 20, 4, 5, 0.0F);
      this.gunModel[1].setRotationPoint(-8.0F, -13.0F, -2.5F);
      this.gunModel[2].addBox(0.0F, 0.0F, 0.0F, 5, 2, 4, 0.0F);
      this.gunModel[2].setRotationPoint(12.0F, -13.0F, -2.0F);
      this.gunModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 4, 4, 3, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
      this.gunModel[3].setRotationPoint(17.0F, -13.0F, -1.5F);
      this.gunModel[4].addBox(0.0F, 0.0F, 0.0F, 20, 2, 2, 0.0F);
      this.gunModel[4].setRotationPoint(21.0F, -13.0F, -1.0F);
      this.gunModel[5].addBox(0.0F, 0.0F, 0.0F, 16, 4, 3, 0.0F);
      this.gunModel[5].setRotationPoint(21.0F, -12.0F, -1.5F);
      this.gunModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 5, 3, 3, 0.0F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F);
      this.gunModel[6].setRotationPoint(41.0F, -13.5F, -1.5F);
      this.gunModel[7].addBox(0.0F, 0.0F, 0.0F, 4, 3, 1, 0.0F);
      this.gunModel[7].setRotationPoint(-5.0F, -14.7F, 1.0F);
      this.gunModel[7].rotateAngleZ = (float) (-Math.PI / 4);
      this.gunModel[8].addBox(0.0F, 0.0F, 0.0F, 4, 3, 1, 0.0F);
      this.gunModel[8].setRotationPoint(-5.0F, -14.7F, -2.0F);
      this.gunModel[8].rotateAngleZ = (float) (-Math.PI / 4);
      this.gunModel[9].addShapeBox(0.0F, 0.0F, 0.0F, 1, 2, 2, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F);
      this.gunModel[9].setRotationPoint(42.0F, -15.0F, -1.0F);
      this.gunModel[10].addBox(0.0F, 0.0F, 0.0F, 16, 1, 4, 0.0F);
      this.gunModel[10].setRotationPoint(-4.0F, -9.0F, -2.0F);
      this.gunModel[11].addBox(0.0F, 0.0F, 0.0F, 10, 1, 4, 0.0F);
      this.gunModel[11].setRotationPoint(-4.0F, -8.0F, -2.0F);
      this.gunModel[12].addShapeBox(0.0F, 0.0F, 0.0F, 6, 1, 5, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F);
      this.gunModel[12].setRotationPoint(4.5F, -4.0F, -2.5F);
      this.gunModel[13].addShapeBox(0.0F, 0.0F, 0.0F, 5, 1, 5, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F);
      this.gunModel[13].setRotationPoint(10.5F, -3.0F, -2.5F);
      this.gunModel[13].rotateAngleZ = (float) (Math.PI / 2);
      this.gunModel[14].addBox(0.0F, 0.0F, 0.0F, 1, 3, 1, 0.0F);
      this.gunModel[14].setRotationPoint(7.0F, -8.0F, -0.5F);
      this.gunModel[14].rotateAngleZ = 0.08726646F;
      this.gunModel[15].addShapeBox(0.0F, 0.0F, 0.0F, 6, 1, 5, 0.0F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F, 0.0F, 0.0F, -1.5F);
      this.gunModel[15].setRotationPoint(4.5F, -11.5F, -4.1F);
      this.gunModel[16].addShapeBox(0.0F, 0.0F, 0.0F, 1, 2, 3, 0.0F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F, -0.45F, 0.0F, -0.75F);
      this.gunModel[16].setRotationPoint(-5.5F, -14.4F, -1.5F);
      this.defaultStockModel = new ModelRendererTurbo[3];
      this.defaultStockModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.defaultStockModel[1] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.defaultStockModel[2] = new ModelRendererTurbo(this, 97, 33, this.textureX, this.textureY);
      this.defaultStockModel[0].addShapeBox(0.0F, 0.0F, 0.0F, 17, 8, 5, 0.0F, -2.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -12.0F, 0.0F, 0.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.defaultStockModel[0].setRotationPoint(-27.0F, -7.0F, -2.5F);
      this.defaultStockModel[0].rotateAngleZ = 0.06981317F;
      this.defaultStockModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 11, 3, 5, 0.0F, -6.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -6.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F);
      this.defaultStockModel[1].setRotationPoint(-18.0F, -4.0F, -2.5F);
      this.defaultStockModel[1].rotateAngleZ = 0.43633232F;
      this.defaultStockModel[2].addBox(0.0F, 0.0F, 0.0F, 3, 3, 5, 0.0F);
      this.defaultStockModel[2].setRotationPoint(-7.5F, -9.0F, -2.5F);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 81, 17, this.textureX, this.textureY);
      this.ammoModel[0].addBox(12.0F, -11.0F, -2.0F, 4, 16, 4, 0.0F);
      this.ammoModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.slideModel = new ModelRendererTurbo[1];
      this.slideModel[0] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.slideModel[0].addBox(0.0F, 0.0F, 0.0F, 1, 1, 2, 0.0F);
      this.slideModel[0].setRotationPoint(9.5F, -11.5F, -4.0F);
      this.gunSlideDistance = 0.6F;
      this.animationType = EnumAnimationType.BOTTOM_CLIP;
      this.translateAll(0.0F, -6.2F, 0.3F);
      this.thirdPersonOffset = new Vector3f(-0.2F, -0.1F, 0.02F);
      this.flipAll();
   }
}
