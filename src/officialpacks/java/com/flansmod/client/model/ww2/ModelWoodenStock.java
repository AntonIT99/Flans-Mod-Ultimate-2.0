//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelAttachment;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWoodenStock extends ModelAttachment {
   int textureX = 32;
   int textureY = 32;

   public ModelWoodenStock() {
      this.attachmentModel = new ModelRendererTurbo[3];
      this.attachmentModel[0] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.attachmentModel[1] = new ModelRendererTurbo(this, 13, 1, this.textureX, this.textureY);
      this.attachmentModel[2] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.attachmentModel[0].addShapeBox(-8.0F, 0.0F, -0.5F, 3, 1, 1, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.attachmentModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[1].addShapeBox(-8.0F, 1.0F, -0.5F, 8, 1, 1, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 1.0F, 0.2F, 0.0F, -1.0F, 0.2F, 0.0F, -1.0F, 0.2F, 0.2F, 1.0F, 0.2F);
      this.attachmentModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.attachmentModel[2].addShapeBox(-5.0F, 0.0F, -0.5F, 5, 1, 1, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.attachmentModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.flipAll();
   }
}
