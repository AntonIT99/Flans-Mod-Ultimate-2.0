//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.wolffsmod.api.client.model.ModelBase;
import net.minecraft.world.entity.Entity;

public class ModelMine extends ModelBase {
   public ModelRendererTurbo[] mineModel = new ModelRendererTurbo[3];
   public ModelRendererTurbo buttonModel;

   public ModelMine() {
      this.mineModel[0] = new ModelRendererTurbo(this, 0, 0, 32, 8);
      this.mineModel[0].addBox(-2.0F, 0.0F, -3.0F, 4, 2, 6);
      this.mineModel[1] = new ModelRendererTurbo(this, 14, 0, 32, 8);
      this.mineModel[1].addBox(-3.0F, 0.0F, -2.0F, 1, 2, 4);
      this.mineModel[2] = new ModelRendererTurbo(this, 14, 0, 32, 8);
      this.mineModel[2].addBox(-3.0F, 0.0F, -2.0F, 1, 2, 4);
      this.mineModel[2].rotateAngleY = (float) Math.PI;
      this.buttonModel = new ModelRendererTurbo(this, 0, 0, 32, 8);
      this.buttonModel.addBox(-0.5F, 1.5F, -0.5F, 1, 1, 1);
   }

   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      for (ModelRendererTurbo mineModelBit : this.mineModel) {
         mineModelBit.render(f5);
      }

      this.buttonModel.render(f5);
   }
}
