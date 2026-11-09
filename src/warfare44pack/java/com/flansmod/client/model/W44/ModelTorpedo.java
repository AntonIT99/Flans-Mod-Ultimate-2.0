package com.flansmod.client.model.W44;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelTorpedo extends ModelBase {
   int textureX = 128;
   int textureY = 128;
   public ModelRendererTurbo[] torpedoModel = new ModelRendererTurbo[18];

   public ModelTorpedo() {
      this.torpedoModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.torpedoModel[1] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.torpedoModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.torpedoModel[3] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.torpedoModel[4] = new ModelRendererTurbo(this, 97, 1, this.textureX, this.textureY);
      this.torpedoModel[5] = new ModelRendererTurbo(this, 73, 17, this.textureX, this.textureY);
      this.torpedoModel[6] = new ModelRendererTurbo(this, 97, 17, this.textureX, this.textureY);
      this.torpedoModel[7] = new ModelRendererTurbo(this, 73, 33, this.textureX, this.textureY);
      this.torpedoModel[8] = new ModelRendererTurbo(this, 1, 57, this.textureX, this.textureY);
      this.torpedoModel[9] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.torpedoModel[10] = new ModelRendererTurbo(this, 89, 9, this.textureX, this.textureY);
      this.torpedoModel[11] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.torpedoModel[12] = new ModelRendererTurbo(this, 113, 49, this.textureX, this.textureY);
      this.torpedoModel[13] = new ModelRendererTurbo(this, 25, 57, this.textureX, this.textureY);
      this.torpedoModel[14] = new ModelRendererTurbo(this, 41, 49, this.textureX, this.textureY);
      this.torpedoModel[15] = new ModelRendererTurbo(this, 57, 57, this.textureX, this.textureY);
      this.torpedoModel[16] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.torpedoModel[17] = new ModelRendererTurbo(this, 113, 9, this.textureX, this.textureY);
      this.torpedoModel[0].addBox(0.0F, 0.0F, 0.0F, 2, 45, 6, 0.0F);
      this.torpedoModel[0].setRotationPoint(-1.0F, -16.0F, -3.0F);
      this.torpedoModel[1].addShapeBox(0.0F, 0.0F, 0.0F, 2, 45, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[1].setRotationPoint(1.0F, -16.0F, -3.0F);
      this.torpedoModel[2].addShapeBox(0.0F, 0.0F, 0.0F, 2, 45, 6, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F);
      this.torpedoModel[2].setRotationPoint(-3.0F, -16.0F, -3.0F);
      this.torpedoModel[3].addShapeBox(0.0F, 0.0F, 0.0F, 2, 3, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F);
      this.torpedoModel[3].setRotationPoint(-1.0F, 29.0F, -3.0F);
      this.torpedoModel[4].addShapeBox(0.0F, 0.0F, 0.0F, 2, 3, 6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F);
      this.torpedoModel[4].setRotationPoint(1.0F, 29.0F, -3.0F);
      this.torpedoModel[5].addShapeBox(0.0F, 0.0F, 0.0F, 2, 3, 6, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F);
      this.torpedoModel[5].setRotationPoint(-3.0F, 29.0F, -3.0F);
      this.torpedoModel[6].addShapeBox(0.0F, 0.0F, 0.0F, 2, 26, 6, 0.0F, -2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F);
      this.torpedoModel[6].setRotationPoint(-3.0F, -42.0F, -3.0F);
      this.torpedoModel[7].addShapeBox(0.0F, 0.0F, 0.0F, 2, 26, 6, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[7].setRotationPoint(-1.0F, -42.0F, -3.0F);
      this.torpedoModel[8].addShapeBox(0.0F, 0.0F, 0.0F, 2, 26, 6, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, -2.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[8].setRotationPoint(1.0F, -42.0F, -3.0F);
      this.torpedoModel[9].addShapeBox(0.0F, 0.0F, 0.0F, 1, 5, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[9].setRotationPoint(-0.5F, -47.0F, -0.5F);
      this.torpedoModel[10].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[10].setRotationPoint(-0.5F, -46.0F, -2.5F);
      this.torpedoModel[11].addShapeBox(0.0F, 0.0F, 0.0F, 5, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[11].setRotationPoint(-2.5F, -44.0F, -0.5F);
      this.torpedoModel[12].addShapeBox(0.0F, 0.0F, 0.0F, 5, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F);
      this.torpedoModel[12].setRotationPoint(0.5F, -40.0F, -0.5F);
      this.torpedoModel[13].addShapeBox(0.0F, 0.0F, 0.0F, 5, 9, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[13].setRotationPoint(-5.5F, -40.0F, -0.5F);
      this.torpedoModel[14].addShapeBox(0.0F, 0.0F, 0.0F, 1, 9, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[14].setRotationPoint(-0.5F, -40.0F, 0.5F);
      this.torpedoModel[15].addShapeBox(0.0F, 0.0F, 0.0F, 1, 9, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 5.0F, 0.0F);
      this.torpedoModel[15].setRotationPoint(-0.5F, -40.0F, -5.5F);
      this.torpedoModel[16].addShapeBox(0.0F, 0.0F, 0.0F, 5, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[16].setRotationPoint(-2.5F, -46.0F, -0.5F);
      this.torpedoModel[17].addShapeBox(0.0F, 0.0F, 0.0F, 1, 1, 5, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.torpedoModel[17].setRotationPoint(-0.5F, -44.0F, -2.5F);
   }


   public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5) {
   }
}
