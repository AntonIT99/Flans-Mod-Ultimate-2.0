package com.flansmod.client.model.manus_legacy;

import com.flansmod.client.model.ModelPlane;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Shape2D;

public class ModelDropship2 extends ModelPlane {
   public ModelDropship2() {
      int textureX = 512;
      int textureY = 512;
      this.bodyModel = new ModelRendererTurbo[18];
      this.bodyModel[0] = new ModelRendererTurbo(this, 0, 504, textureX, textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 0, 457, textureX, textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 100, 470, textureX, textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 100, 457, textureX, textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 98, 494, textureX, textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 0, 366, textureX, textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 0, 429, textureX, textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 84, 429, textureX, textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 174, 429, textureX, textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 216, 429, textureX, textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 0, 324, textureX, textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 130, 370, textureX, textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 73, 370, textureX, textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 73, 400, textureX, textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 130, 400, textureX, textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 0, 238, textureX, textureY);
      this.bodyModel[0].addBox(0.0F, 0.0F, 0.0F, 18, 4, 4, 0.0F);
      this.bodyModel[0].setRotationPoint(-64.0F, -3.0F, -2.0F);
      this.bodyModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 14, 11, 34, 0.0F, -3.0F, 3);
      this.bodyModel[1].setRotationPoint(-60.0F, -12.0F, -17.0F);
      this.bodyModel[2].addBox(0.0F, 0.0F, 0.0F, 30, 10, 1, 0.0F);
      this.bodyModel[2].setRotationPoint(-46.0F, -12.0F, -17.0F);
      this.bodyModel[3].addBox(0.0F, 0.0F, 0.0F, 30, 10, 1, 0.0F);
      this.bodyModel[3].setRotationPoint(-46.0F, -12.0F, 16.0F);
      this.bodyModel[4].addBox(0.0F, 0.0F, 0.0F, 30, 6, 12, 0.0F);
      this.bodyModel[4].setRotationPoint(-46.0F, -2.0F, -6.0F);
      this.bodyModel[5].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 5.0, 30, 5), new Coord2D(30.0, 6.0, 30, 6), new Coord2D(0.0, 6.0, 0, 6)}), 11.0F, 30, 6, 67, 11, 0, new float[]{30.0F, 1.0F, 30.0F, 6.0F});
      this.bodyModel[5].setRotationPoint(-16.0F, 4.0F, -6.0F);
      this.bodyModel[6].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 5.0, 30, 5), new Coord2D(30.0, 6.0, 30, 6), new Coord2D(0.0, 6.0, 0, 6)}), 11.0F, 30, 6, 67, 11, 0, new float[]{30.0F, 1.0F, 30.0F, 6.0F});
      this.bodyModel[6].setRotationPoint(-16.0F, 4.0F, 17.0F);
      this.bodyModel[7].addBox(0.0F, 0.0F, 0.0F, 1, 25, 34, 0.0F);
      this.bodyModel[7].setRotationPoint(-16.0F, -25.0F, -17.0F);
      this.bodyModel[8].addBox(0.0F, 0.0F, 0.0F, 17, 1, 24, 0.0F);
      this.bodyModel[8].setRotationPoint(-47.0F, -12.0F, -12.0F);
      this.bodyModel[8].rotateAngleZ = (float) (Math.PI / 4);
      this.bodyModel[9].addBox(0.0F, 0.0F, 0.0F, 20, 1, 24, 0.0F);
      this.bodyModel[9].setRotationPoint(-35.0F, -24.0F, -12.0F);
      this.bodyModel[9].rotateAngleZ = 0.1047198F;
      this.bodyModel[10].addBox(0.0F, 0.0F, 0.0F, 19, 13, 1, 0.0F);
      this.bodyModel[10].setRotationPoint(-35.0F, -24.0F, -12.0F);
      this.bodyModel[10].rotateAngleX = -0.3839724F;
      this.bodyModel[11].addBox(0.0F, 0.0F, 0.0F, 19, 1, 13, 0.0F);
      this.bodyModel[11].setRotationPoint(-35.0F, -24.0F, 12.0F);
      this.bodyModel[11].rotateAngleX = -1.186824F;
      this.bodyModel[12].addTrapezoid(0.0F, 0.0F, 0.0F, 80, 4, 36, 0.0F, -4.0F, 4);
      this.bodyModel[12].setRotationPoint(-16.0F, -29.0F, -18.0F);
      this.bodyModel[13].addBox(0.0F, 0.0F, 0.0F, 32, 25, 1, 0.0F);
      this.bodyModel[13].setRotationPoint(32.0F, -25.0F, 17.0F);
      this.bodyModel[14].addBox(0.0F, 0.0F, 0.0F, 26, 25, 1, 0.0F);
      this.bodyModel[14].setRotationPoint(-16.0F, -25.0F, 17.0F);
      this.bodyModel[15].addBox(0.0F, 0.0F, 0.0F, 26, 25, 1, 0.0F);
      this.bodyModel[15].setRotationPoint(-16.0F, -25.0F, -18.0F);
      this.bodyModel[16].addBox(0.0F, 0.0F, 0.0F, 32, 25, 1, 0.0F);
      this.bodyModel[16].setRotationPoint(32.0F, -25.0F, -18.0F);
      this.bodyModel[17].addBox(0.0F, 0.0F, 0.0F, 80, 4, 36, 0.0F);
      this.bodyModel[17].setRotationPoint(-16.0F, 0.0F, -18.0F);
      this.tailModel = new ModelRendererTurbo[13];
      this.tailModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[1] = new ModelRendererTurbo(this, 0, 281, textureX, textureY);
      this.tailModel[2] = new ModelRendererTurbo(this, 221, 496, textureX, textureY);
      this.tailModel[3] = new ModelRendererTurbo(this, 200, 496, textureX, textureY);
      this.tailModel[4] = new ModelRendererTurbo(this, 111, 145, textureX, textureY);
      this.tailModel[5] = new ModelRendererTurbo(this, 0, 145, textureX, textureY);
      this.tailModel[6] = new ModelRendererTurbo(this, 0, 165, textureX, textureY);
      this.tailModel[7] = new ModelRendererTurbo(this, 0, 178, textureX, textureY);
      this.tailModel[8] = new ModelRendererTurbo(this, 0, 178, textureX, textureY);
      this.tailModel[9] = new ModelRendererTurbo(this, 412, 229, textureX, textureY);
      this.tailModel[10] = new ModelRendererTurbo(this, 412, 229, textureX, textureY);
      this.tailModel[11] = new ModelRendererTurbo(this, 320, 262, textureX, textureY);
      this.tailModel[12] = new ModelRendererTurbo(this, 320, 229, textureX, textureY);
      this.tailModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 16.0, 32, 16), new Coord2D(32.0, 29.0, 32, 29), new Coord2D(0.0, 29.0, 0, 29)}), 36.0F, 32, 29, 110, 36, 0, new float[]{36.0F, 13.0F, 32.0F, 29.0F});
      this.tailModel[0].rotateAngleX = (float) Math.PI;
      this.tailModel[0].rotateAngleZ = (float) Math.PI;
      this.tailModel[0].setRotationPoint(64.0F, 4.0F, -18.0F);
      this.tailModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 44, 4, 36, 0.0F, -4.0F, 4);
      this.tailModel[1].setRotationPoint(52.0F, -29.0F, -18.0F);
      this.tailModel[2].addBox(0.0F, 0.0F, 0.0F, 5, 12, 4, 0.0F);
      this.tailModel[2].setRotationPoint(41.0F, -25.0F, 18.0F);
      this.tailModel[3].addBox(0.0F, 0.0F, 0.0F, 5, 12, 4, 0.0F);
      this.tailModel[3].setRotationPoint(41.0F, -25.0F, -22.0F);
      this.tailModel[4].addBox(0.0F, 0.0F, 0.0F, 50, 4, 12, 0.0F);
      this.tailModel[4].setRotationPoint(46.0F, -25.0F, 22.0F);
      this.tailModel[4].rotateAngleX = -1.570796F;
      this.tailModel[4].rotateAngleY = -0.08203048F;
      this.tailModel[5].addBox(0.0F, 0.0F, 0.0F, 50, 12, 4, 0.0F);
      this.tailModel[5].setRotationPoint(46.0F, -25.0F, -22.0F);
      this.tailModel[5].rotateAngleY = 0.08028515F;
      this.tailModel[6].addBox(0.0F, 0.0F, 0.0F, 3, 6, 4, 0.0F);
      this.tailModel[6].setRotationPoint(94.0F, -22.0F, -2.0F);
      this.tailModel[7].addBox(0.0F, 0.0F, 0.0F, 3, 10, 10, 0.0F);
      this.tailModel[7].setRotationPoint(95.0F, -24.0F, -14.0F);
      this.tailModel[8].addBox(0.0F, 0.0F, 0.0F, 3, 10, 10, 0.0F);
      this.tailModel[8].setRotationPoint(95.0F, -24.0F, 4.0F);
      this.tailModel[9].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(22.0, 26.0, 22, 26), new Coord2D(7.0, 26.0, 7, 26)}), 1.0F, 22, 26, 88, 1, 0, new float[]{20.0F, 26.0F, 15.0F, 27.0F});
      this.tailModel[9].setRotationPoint(74.0F, -25.0F, -14.0F);
      this.tailModel[9].rotateAngleX = -0.1919862F;
      this.tailModel[9].rotateAngleY = (float) Math.PI;
      this.tailModel[10].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(22.0, 26.0, 22, 26), new Coord2D(7.0, 26.0, 7, 26)}), 1.0F, 22, 26, 88, 1, 0, new float[]{20.0F, 26.0F, 15.0F, 27.0F});
      this.tailModel[10].setRotationPoint(74.0F, -25.0F, 13.0F);
      this.tailModel[10].rotateAngleX = 0.1919862F;
      this.tailModel[10].rotateAngleY = (float) Math.PI;
      this.tailModel[11].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(22.0, 26.0, 22, 26), new Coord2D(7.0, 26.0, 7, 26)}), 1.0F, 22, 26, 88, 1, 0, new float[]{20.0F, 26.0F, 15.0F, 27.0F});
      this.tailModel[11].setRotationPoint(78.0F, -24.0F, -17.0F);
      this.tailModel[11].rotateAngleX = 4.520403F;
      this.tailModel[11].rotateAngleY = (float) Math.PI;
      this.tailModel[12].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(22.0, 26.0, 22, 26), new Coord2D(7.0, 26.0, 7, 26)}), 1.0F, 22, 26, 88, 1, 0, new float[]{20.0F, 26.0F, 15.0F, 27.0F});
      this.tailModel[12].setRotationPoint(78.0F, -25.0F, 17.0F);
      this.tailModel[12].rotateAngleX = 1.7627826F;
      this.tailModel[12].rotateAngleY = (float) Math.PI;
      this.leftWingModel = new ModelRendererTurbo[7];
      this.leftWingModel[0] = new ModelRendererTurbo(this, 300, 160, textureX, textureY);
      this.leftWingModel[1] = new ModelRendererTurbo(this, 300, 64, textureX, textureY);
      this.leftWingModel[2] = new ModelRendererTurbo(this, 245, 473, textureX, textureY);
      this.leftWingModel[3] = new ModelRendererTurbo(this, 245, 467, textureX, textureY);
      this.leftWingModel[4] = new ModelRendererTurbo(this, 245, 492, textureX, textureY);
      this.leftWingModel[5] = new ModelRendererTurbo(this, 245, 483, textureX, textureY);
      this.leftWingModel[6] = new ModelRendererTurbo(this, 102, 200, textureX, textureY);
      this.leftWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(68.0, 0.0, 68, 0), new Coord2D(38.0, 56.0, 38, 56), new Coord2D(18.0, 56.0, 18, 56)}), 2.0F, 68, 56, 210, 2, 0, new float[]{68.0F, 64.0F, 20.0F, 59.0F});
      this.leftWingModel[0].setRotationPoint(77.0F, -28.0F, -14.0F);
      this.leftWingModel[0].rotateAngleX = 1.7819808F;
      this.leftWingModel[1].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(26.0, 20.0, 26, 20), new Coord2D(16.0, 20.0, 16, 20)}), 2.0F, 26, 20, 76, 2, 0, new float[]{20.0F, 21.0F, 10.0F, 26.0F});
      this.leftWingModel[1].setRotationPoint(59.0F, -16.5F, -68.8F);
      this.leftWingModel[1].rotateAngleX = 2.0415926F;
      this.leftWingModel[4].addBox(0.0F, 0.0F, 0.0F, 28, 10, 10, 0.0F);
      this.leftWingModel[4].setRotationPoint(34.0F, -14.0F, -65.0F);
      this.leftWingModel[5].addBox(0.0F, 0.0F, 0.0F, 21, 4, 2, 0.0F);
      this.leftWingModel[5].setRotationPoint(39.0F, -17.5F, -61.0F);
      this.leftWingModel[6].addBox(0.0F, 0.0F, 0.0F, 20, 5, 20, 0.0F);
      this.leftWingModel[6].setRotationPoint(52.0F, -23.0F, -46.0F);
      this.leftWingModel[6].rotateAngleX = (float) (Math.PI / 15);
      this.rightWingModel = new ModelRendererTurbo[7];
      this.rightWingModel[0] = new ModelRendererTurbo(this, 300, 97, textureX, textureY);
      this.rightWingModel[1] = new ModelRendererTurbo(this, 300, 40, textureX, textureY);
      this.rightWingModel[2] = new ModelRendererTurbo(this, 245, 473, textureX, textureY);
      this.rightWingModel[3] = new ModelRendererTurbo(this, 245, 467, textureX, textureY);
      this.rightWingModel[4] = new ModelRendererTurbo(this, 245, 492, textureX, textureY);
      this.rightWingModel[5] = new ModelRendererTurbo(this, 245, 483, textureX, textureY);
      this.rightWingModel[6] = new ModelRendererTurbo(this, 102, 200, textureX, textureY);
      this.rightWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(68.0, 0.0, 68, 0), new Coord2D(38.0, 56.0, 38, 56), new Coord2D(18.0, 56.0, 18, 56)}), 2.0F, 68, 56, 210, 2, 0, new float[]{68.0F, 64.0F, 20.0F, 59.0F});
      this.rightWingModel[0].setRotationPoint(77.0F, -27.0F, 14.0F);
      this.rightWingModel[0].rotateAngleX = 4.5012045F;
      this.rightWingModel[1].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(20.0, 0.0, 20, 0), new Coord2D(26.0, 20.0, 26, 20), new Coord2D(16.0, 20.0, 16, 20)}), 2.0F, 26, 20, 76, 2, 0, new float[]{20.0F, 21.0F, 10.0F, 26.0F});
      this.rightWingModel[1].setRotationPoint(59.0F, -15.5F, 68.5F);
      this.rightWingModel[1].rotateAngleX = 4.241593F;
      this.rightWingModel[4].addBox(0.0F, 0.0F, 0.0F, 28, 10, 10, 0.0F);
      this.rightWingModel[4].setRotationPoint(34.0F, -14.0F, 55.0F);
      this.rightWingModel[5].addBox(0.0F, 0.0F, 0.0F, 21, 4, 2, 0.0F);
      this.rightWingModel[5].setRotationPoint(39.0F, -17.5F, 59.0F);
      this.rightWingModel[6].addBox(0.0F, 0.0F, 0.0F, 20, 5, 20, 0.0F);
      this.rightWingModel[6].setRotationPoint(72.0F, -23.0F, 46.0F);
      this.rightWingModel[6].rotateAngleX = (float) (Math.PI / 15);
      this.rightWingModel[6].rotateAngleY = (float) Math.PI;
      this.propellerModels = new ModelRendererTurbo[0][0];
      this.yawFlapModel = new ModelRendererTurbo[0];
      this.pitchFlapLeftModel = new ModelRendererTurbo[0];
      this.pitchFlapRightModel = new ModelRendererTurbo[0];
      this.topWingModel = new ModelRendererTurbo[0];
      this.pitchFlapLeftWingModel = new ModelRendererTurbo[0];
      this.pitchFlapRightWingModel = new ModelRendererTurbo[0];
      this.translateAll(-5);
      this.flipAll();
      // Manus Legacy alignment: centres the model where the definition expects it.
      this.translateAll(-5.17F, 0F, 0F);
   }

   private void translateAll(int y) {
      for (ModelRendererTurbo mod : this.bodyModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.leftWingModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.rightWingModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.yawFlapModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.pitchFlapRightWingModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.pitchFlapLeftWingModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.pitchFlapLeftModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.pitchFlapRightModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo mod : this.tailModel) {
         mod.rotationPointY += y;
      }

      for (ModelRendererTurbo[] mods : this.propellerModels) {
         for (ModelRendererTurbo mod : mods) {
            mod.rotationPointY += y;
         }
      }
   }
}
