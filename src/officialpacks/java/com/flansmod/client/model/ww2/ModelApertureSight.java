//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelApertureSight extends ModelAttachment {
   int textureX = 64;
   int textureY = 64;

   public ModelApertureSight() {
      this.attachmentModel = new ModelRendererTurbo[14];
      this.attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.attachmentModel[1] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.attachmentModel[2] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.attachmentModel[3] = new ModelRendererTurbo(this, 13, 1, this.textureX, this.textureY);
      this.attachmentModel[4] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.attachmentModel[5] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.attachmentModel[6] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.attachmentModel[7] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.attachmentModel[8] = new ModelRendererTurbo(this, 49, 9, this.textureX, this.textureY);
      this.attachmentModel[9] = new ModelRendererTurbo(this, 33, 9, this.textureX, this.textureY);
      this.attachmentModel[10] = new ModelRendererTurbo(this, 41, 9, this.textureX, this.textureY);
      this.attachmentModel[11] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.attachmentModel[12] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.attachmentModel[13] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.attachmentModel[0].addShapeBox(-1.0F, -1.0F, -1.0F, 2, 1, 1, 0.0F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[1].addShapeBox(-0.5F, -1.25F, -1.0F, 1, 1, 2, 0.0F, 0.0F, -0.3F, -0.45F, 0.25F, -0.3F, -0.45F, 0.25F, -0.3F, -0.45F, 0.0F, -0.3F, -0.45F, 0.0F, -0.3F, -0.45F, 0.25F, -0.3F, -0.45F, 0.25F, -0.3F, -0.45F, 0.0F, -0.3F, -0.45F);
      this.attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[2].addShapeBox(-0.5F, -1.65F, -1.0F, 1, 1, 2, 0.0F, -0.2F, -0.3F, -0.8F, 0.0F, -0.3F, -0.8F, 0.0F, -0.3F, -0.8F, -0.2F, -0.3F, -0.8F, -0.2F, -0.3F, -0.5F, 0.2F, -0.3F, -0.5F, 0.2F, -0.3F, -0.5F, -0.2F, -0.3F, -0.5F);
      this.attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[3].addShapeBox(-0.5F, -2.5F, -0.5F, 1, 15, 15, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, -14.0F, -0.4F, 0.0F, -14.0F, -0.4F, -14.0F, 0.0F, -0.4F, -14.0F, 0.0F, -0.4F, -14.0F, -14.0F, -0.4F, -14.0F, -14.0F);
      this.attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[4].addShapeBox(-0.5F, -1.95F, -1.0F, 1, 1, 2, 0.0F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F);
      this.attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[5].addShapeBox(-0.5F, -3.0F, -1.1F, 1, 2, 1, 0.0F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.5F, -0.4F);
      this.attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[6].addShapeBox(-0.5F, -1.55F, -1.0F, 1, 1, 2, 0.0F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F, -0.4F, -0.4F, -0.3F);
      this.attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[7].addShapeBox(-0.5F, -1.55F, 0.25F, 1, 1, 1, 0.0F, -0.38F, -0.38F, -0.45F, -0.38F, -0.38F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.38F, -0.38F, -0.45F, -0.38F, -0.38F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F);
      this.attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[8].addShapeBox(-0.8F, -1.55F, -1.0F, 1, 1, 2, 0.0F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F);
      this.attachmentModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[9].addShapeBox(-1.0F, -1.0F, 0.0F, 2, 1, 1, 0.0F, 0.0F, -0.3F, -0.3F, 0.0F, -0.3F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, -0.5F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.attachmentModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[10].addShapeBox(-1.0F, -1.0F, -0.5F, 2, 1, 1, 0.0F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F, 0.0F, -0.3F, -0.2F);
      this.attachmentModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[11].addShapeBox(-0.5F, -1.55F, -1.25F, 1, 1, 1, 0.0F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.38F, -0.38F, -0.45F, -0.38F, -0.38F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.45F, -0.38F, -0.38F, -0.45F, -0.38F, -0.38F, -0.45F);
      this.attachmentModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[12].addShapeBox(-0.5F, -3.0F, 0.1F, 1, 2, 1, 0.0F, -0.3F, -0.5F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.5F, -0.4F, -0.3F, -0.8F, -0.4F, -0.3F, -0.8F, -0.4F);
      this.attachmentModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[13].addShapeBox(-0.5F, -3.05F, -1.0F, 1, 1, 2, 0.0F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F, -0.3F, -0.4F, -0.7F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F, -0.3F, -0.45F, -0.5F);
      this.attachmentModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
