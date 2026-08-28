//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWoodenGrip extends ModelAttachment {
   int textureX = 32;
   int textureY = 32;

   public ModelWoodenGrip() {
      this.attachmentModel = new ModelRendererTurbo[8];
      this.attachmentModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.attachmentModel[1] = new ModelRendererTurbo(this, 9, 1, this.textureX, this.textureY);
      this.attachmentModel[2] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.attachmentModel[3] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.attachmentModel[4] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.attachmentModel[5] = new ModelRendererTurbo(this, 9, 9, this.textureX, this.textureY);
      this.attachmentModel[6] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.attachmentModel[7] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.attachmentModel[0].addShapeBox(-0.5F, 0.0F, -0.5F, 1, 1, 1, 0.0F, -0.1F, -0.3F, -0.1F, 0.25F, -0.3F, -0.1F, 0.25F, -0.3F, -0.1F, -0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F);
      this.attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[1].addShapeBox(-0.9F, 1.8F, -0.5F, 1, 1, 1, 0.0F, 0.05F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.05F, 0.0F, -0.1F, 0.2F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.2F, 0.0F, -0.1F);
      this.attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[2].addShapeBox(-0.5F, 0.4F, -0.5F, 1, 1, 1, 0.0F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.1F, -0.3F, -0.1F, 0.2F, -0.5F, -0.1F, 0.0F, -0.5F, -0.1F, 0.0F, -0.5F, -0.1F, 0.2F, -0.5F, -0.1F);
      this.attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[3].addShapeBox(-0.5F, 0.3F, -0.5F, 1, 1, 1, 0.0F, 0.2F, -0.6F, -0.1F, 0.0F, -0.6F, -0.1F, 0.0F, -0.6F, -0.1F, 0.2F, -0.6F, -0.1F, 0.3F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.0F, -0.1F, -0.1F, 0.3F, -0.1F, -0.1F);
      this.attachmentModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[4].addShapeBox(-0.5F, 0.6F, -0.5F, 1, 1, 1, 0.0F, 0.3F, -0.6F, -0.1F, 0.0F, -0.6F, -0.1F, 0.0F, -0.6F, -0.1F, 0.3F, -0.6F, -0.1F, 0.4F, -0.1F, -0.1F, -0.1F, -0.1F, -0.1F, -0.1F, -0.1F, -0.1F, 0.4F, -0.1F, -0.1F);
      this.attachmentModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[5].addShapeBox(-0.5F, 0.9F, -0.5F, 1, 1, 1, 0.0F, 0.4F, -0.6F, -0.1F, -0.1F, -0.6F, -0.1F, -0.1F, -0.6F, -0.1F, 0.4F, -0.6F, -0.1F, 0.45F, -0.1F, -0.1F, -0.3F, -0.1F, -0.1F, -0.3F, -0.1F, -0.1F, 0.45F, -0.1F, -0.1F);
      this.attachmentModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[6].addShapeBox(-1.0F, 2.8F, -0.5F, 1, 1, 1, 0.0F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F, -0.1F, -0.5F, -0.2F, -0.1F, -0.5F, -0.2F, -0.1F, -0.5F, -0.2F, -0.1F, -0.5F, -0.2F);
      this.attachmentModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[7].addShapeBox(-0.5F, -0.6F, -0.5F, 1, 1, 1, 0.0F, 0.2F, -0.5F, 0.0F, 0.4F, -0.5F, 0.0F, 0.4F, -0.5F, 0.0F, 0.2F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.attachmentModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
