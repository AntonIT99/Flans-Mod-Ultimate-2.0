package com.flansmod.client.model.ww2;

import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelMosinSniper extends ModelMosin {
   public ModelMosinSniper() {
      int textureX = 64;
      int textureY = 16;
      this.defaultScopeModel = new ModelRendererTurbo[2];
      this.defaultScopeModel[0] = new ModelRendererTurbo(this, 24, 7, textureX, textureY);
      this.defaultScopeModel[0].addBox(1.5F, 5.0F, -0.5F, 1, 1, 1);
      this.defaultScopeModel[1] = new ModelRendererTurbo(this, 29, 7, textureX, textureY);
      this.defaultScopeModel[1].addBox(-1.0F, 6.0F, -0.5F, 4, 1, 1);
   }
}
