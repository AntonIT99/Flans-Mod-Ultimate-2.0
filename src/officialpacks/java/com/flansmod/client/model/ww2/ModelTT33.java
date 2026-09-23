package com.flansmod.client.model.ww2;

import com.flansmod.client.model.EnumAnimationType;
import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.common.vector.Vector3f;

public class ModelTT33 extends ModelGun {
   public ModelTT33() {
      int textureX = 32;
      int textureY = 16;
      this.gunModel = new ModelRendererTurbo[3];
      this.gunModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.gunModel[0].addBox(-1.0F, -2.0F, -1.0F, 3, 4, 2);
      this.gunModel[1] = new ModelRendererTurbo(this, 0, 6, textureX, textureY);
      this.gunModel[1].addBox(-1.0F, 2.0F, -1.0F, 8, 1, 2);
      this.gunModel[2] = new ModelRendererTurbo(this, 0, 9, textureX, textureY);
      this.gunModel[2].addBox(-0.5F, 3.5F, -0.5F, 8, 1, 1);
      this.slideModel = new ModelRendererTurbo[3];
      this.slideModel[0] = new ModelRendererTurbo(this, 0, 12, textureX, textureY);
      this.slideModel[0].addBox(-1.0F, 3.0F, -1.0F, 8, 2, 2);
      this.slideModel[1] = new ModelRendererTurbo(this, 10, 2, textureX, textureY);
      this.slideModel[1].addBox(5.95F, 4.5F, -0.5F, 1, 1, 1);
      this.slideModel[2] = new ModelRendererTurbo(this, 8, 0, textureX, textureY);
      this.slideModel[2].addBox(-0.8F, 4.5F, -0.5F, 1, 1, 1);
      this.ammoModel = new ModelRendererTurbo[1];
      this.ammoModel[0] = new ModelRendererTurbo(this, 14, 0, textureX, textureY);
      this.ammoModel[0].addBox(-0.5F, -1.8F, -0.5F, 2, 4, 1);
      this.barrelAttachPoint = new Vector3f(0.46875F, 0.25F, 0.0F);
      this.scopeAttachPoint = new Vector3f(0.1875F, 0.3125F, 0.0F);
      this.scopeIsOnSlide = true;
      this.gunSlideDistance = 0.25F;
      this.animationType = EnumAnimationType.PISTOL_CLIP;
   }
}
