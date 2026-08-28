//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.wolffsmod.api.client.model.ModelBase;
import net.minecraft.world.entity.Entity;

public class ModelWW2Parachute extends ModelBase {
   public ModelRendererTurbo backpackModel;
   public ModelRendererTurbo[] parachuteModel = new ModelRendererTurbo[6];

   public ModelWW2Parachute() {
      this.backpackModel = new ModelRendererTurbo(this, 0, 0, 128, 128);
      this.backpackModel.addBox(-4.0F, 26.0F, -6.0F, 8, 12, 2);
      this.parachuteModel[0] = new ModelRendererTurbo(this, 0, 16, 128, 128);
      this.parachuteModel[0].addTrapezoid(-12.0F, 38.0F, -12.0F, 24, 24, 24, 0.0F, -8.0F, 4);
      this.parachuteModel[1] = new ModelRendererTurbo(this, 72, 0, 128, 128);
      this.parachuteModel[1].addTrapezoid(-8.0F, 62.0F, -8.0F, 16, 8, 16, 0.0F, 4.0F, 4);
      this.parachuteModel[2] = new ModelRendererTurbo(this, 0, 64, 128, 128);
      this.parachuteModel[2].addTrapezoid(-8.0F, 70.0F, -8.0F, 16, 4, 16, 0.0F, -4.0F, 5);
      this.parachuteModel[3] = new ModelRendererTurbo(this, 0, 16, 128, 128);
      this.parachuteModel[3].flip = true;
      this.parachuteModel[3].addTrapezoid(-12.0F, 38.0F, -12.0F, 24, 24, 24, 0.0F, -8.0F, 4);
      this.parachuteModel[4] = new ModelRendererTurbo(this, 72, 0, 128, 128);
      this.parachuteModel[4].flip = true;
      this.parachuteModel[4].addTrapezoid(-8.0F, 62.0F, -8.0F, 16, 8, 16, 0.0F, 4.0F, 4);
      this.parachuteModel[5] = new ModelRendererTurbo(this, 0, 64, 128, 128);
      this.parachuteModel[5].flip = true;
      this.parachuteModel[5].addTrapezoid(-8.0F, 70.0F, -8.0F, 16, 4, 16, 0.0F, -4.0F, 5);
   }

   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      this.backpackModel.render(f5);

      for (ModelRendererTurbo part : this.parachuteModel) {
         part.render(f5);
      }
   }
}
