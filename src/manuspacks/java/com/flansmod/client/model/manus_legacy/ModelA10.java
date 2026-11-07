package com.flansmod.client.model.manus_legacy;

import com.flansmod.client.model.ModelPlane;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelA10 extends ModelPlane {
   public ModelA10() {
      int textureX = 512;
      int textureY = 512;
      boolean VarGear = false;
      this.bodyModel = new ModelRendererTurbo[23];
      this.bodyModel[0] = new ModelRendererTurbo(this, 0, 417, textureX, textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 0, 104, textureX, textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 0, 141, textureX, textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 60, 104, textureX, textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 132, 104, textureX, textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 206, 104, textureX, textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 170, 104, textureX, textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 246, 104, textureX, textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 0, 178, textureX, textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 0, 260, textureX, textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 0, 444, textureX, textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 0, 497, textureX, textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 0, 428, textureX, textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 0, 481, textureX, textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 0, 497, textureX, textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 220, 0, textureX, textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 210, 0, textureX, textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 200, 0, textureX, textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 150, 0, textureX, textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 150, 20, textureX, textureY);
      this.bodyModel[20] = new ModelRendererTurbo(this, 180, 20, textureX, textureY);
      this.bodyModel[21] = new ModelRendererTurbo(this, 150, 40, textureX, textureY);
      this.bodyModel[22] = new ModelRendererTurbo(this, 180, 40, textureX, textureY);
      this.bodyModel[0].addBox(0.0F, 0.0F, 0.0F, 32, 4, 4, 0.0F);
      this.bodyModel[0].setRotationPoint(-111.0F, -13.0F, -4.0F);
      this.bodyModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 10, 16, 18, 0.0F, -5.0F, 3);
      this.bodyModel[1].setRotationPoint(-102.0F, -26.0F, -9.0F);
      this.bodyModel[2].addBox(0.0F, 0.0F, 0.0F, 20, 16, 18, 0.0F);
      this.bodyModel[2].setRotationPoint(-92.0F, -26.0F, -9.0F);
      this.bodyModel[3].addBox(0.0F, 0.0F, 0.0F, 16, 3, 18, 0.0F);
      this.bodyModel[3].setRotationPoint(-72.0F, -13.0F, -9.0F);
      this.bodyModel[4].addBox(0.0F, 0.0F, 0.0F, 16, 13, 1, 0.0F);
      this.bodyModel[4].setRotationPoint(-72.0F, -26.0F, -9.0F);
      this.bodyModel[5].addBox(0.0F, 0.0F, 0.0F, 17, 13, 1, 0.0F);
      this.bodyModel[5].setRotationPoint(-72.0F, -26.0F, -9.0F);
      this.bodyModel[5].rotateAngleZ = 0.2443461F;
      this.bodyModel[6].addBox(0.0F, 0.0F, 0.0F, 16, 13, 1, 0.0F);
      this.bodyModel[6].setRotationPoint(-72.0F, -26.0F, 8.0F);
      this.bodyModel[7].addBox(0.0F, 0.0F, 0.0F, 17, 13, 1, 0.0F);
      this.bodyModel[7].setRotationPoint(-72.0F, -26.0F, 8.0F);
      this.bodyModel[7].rotateAngleZ = 0.2443461F;
      this.bodyModel[8].addBox(0.0F, 0.0F, 0.0F, 48, 20, 18, 0.0F);
      this.bodyModel[8].setRotationPoint(-56.0F, -30.0F, -9.0F);
      this.bodyModel[9].addBox(0.0F, 0.0F, 0.0F, 32, 4, 58, 0.0F);
      this.bodyModel[9].setRotationPoint(-16.0F, -15.0F, -29.0F);
      this.bodyModel[10].addBox(0.0F, 0.0F, 0.0F, 1, 20, 14, 0.0F);
      this.bodyModel[10].setRotationPoint(-74.0F, -37.0F, -7.0F);
      this.bodyModel[10].rotateAngleZ = -0.8203048F;
      this.bodyModel[11].addBox(0.0F, 0.0F, 0.0F, 20, 1, 14, 0.0F);
      this.bodyModel[11].setRotationPoint(-74.0F, -37.0F, -7.0F);
      this.bodyModel[12].addBox(0.0F, 0.0F, 0.0F, 20, 12, 1, 0.0F);
      this.bodyModel[12].setRotationPoint(-74.0F, -37.0F, -7.0F);
      this.bodyModel[12].rotateAngleX = -0.1570796F;
      this.bodyModel[13].addBox(0.0F, 0.0F, 0.0F, 20, 1, 12, 0.0F);
      this.bodyModel[13].setRotationPoint(-74.0F, -37.0F, 7.0F);
      this.bodyModel[13].rotateAngleX = -1.413717F;
      this.bodyModel[14].addBox(0.0F, 0.0F, 0.0F, 20, 1, 14, 0.0F);
      this.bodyModel[14].setRotationPoint(-54.0F, -37.0F, -7.0F);
      this.bodyModel[14].rotateAngleZ = (float) (-Math.PI / 4);
      if (VarGear) {
         this.bodyModel[15].addBox(0.0F, 0.0F, 0.0F, 8, 8, 2, 0.0F);
         this.bodyModel[15].setRotationPoint(-70.0F, 2.0F, 1.0F);
         this.bodyModel[16].addBox(0.0F, 0.0F, 0.0F, 2, 8, 1, 0.0F);
         this.bodyModel[16].setRotationPoint(-67.0F, -1.0F, 3.0F);
         this.bodyModel[17].addBox(0.0F, 0.0F, 0.0F, 2, 11, 1, 0.0F);
         this.bodyModel[17].setRotationPoint(-67.0F, -10.0F, 2.0F);
         this.bodyModel[18].addBox(0.0F, 0.0F, 0.0F, 20, 6, 1, 0.0F);
         this.bodyModel[18].setRotationPoint(-82.0F, -10.0F, 5.0F);
         this.bodyModel[19].addBox(0.0F, 0.0F, 0.0F, 12, 12, 2, 0.0F);
         this.bodyModel[19].setRotationPoint(4.0F, -2.0F, -30.0F);
         this.bodyModel[20].addBox(0.0F, 0.0F, 0.0F, 3, 14, 1, 0.0F);
         this.bodyModel[20].setRotationPoint(7.0F, -9.0F, -28.0F);
         this.bodyModel[20].rotateAngleZ = 0.122173F;
         this.bodyModel[21].addBox(0.0F, 0.0F, 0.0F, 12, 12, 2, 0.0F);
         this.bodyModel[21].setRotationPoint(4.0F, -2.0F, 28.0F);
         this.bodyModel[22].addBox(0.0F, 0.0F, 0.0F, 3, 14, 1, 0.0F);
         this.bodyModel[22].setRotationPoint(7.0F, -9.0F, 27.0F);
         this.bodyModel[22].rotateAngleZ = 0.122173F;
      }

      this.tailModel = new ModelRendererTurbo[14];
      this.tailModel[0] = new ModelRendererTurbo(this, 0, 219, textureX, textureY);
      this.tailModel[1] = new ModelRendererTurbo(this, 189, 434, textureX, textureY);
      this.tailModel[2] = new ModelRendererTurbo(this, 277, 434, textureX, textureY);
      this.tailModel[3] = new ModelRendererTurbo(this, 448, 305, textureX, textureY);
      this.tailModel[4] = new ModelRendererTurbo(this, 448, 340, textureX, textureY);
      this.tailModel[5] = new ModelRendererTurbo(this, 448, 375, textureX, textureY);
      this.tailModel[6] = new ModelRendererTurbo(this, 448, 410, textureX, textureY);
      this.tailModel[7] = new ModelRendererTurbo(this, 448, 445, textureX, textureY);
      this.tailModel[8] = new ModelRendererTurbo(this, 448, 480, textureX, textureY);
      this.tailModel[9] = new ModelRendererTurbo(this, 78, 430, textureX, textureY);
      this.tailModel[10] = new ModelRendererTurbo(this, 137, 157, textureX, textureY);
      this.tailModel[11] = new ModelRendererTurbo(this, 137, 126, textureX, textureY);
      this.tailModel[12] = new ModelRendererTurbo(this, 137, 157, textureX, textureY);
      this.tailModel[13] = new ModelRendererTurbo(this, 137, 126, textureX, textureY);
      this.tailModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 96, 20, 18, 0.0F, -3.0F, 2);
      this.tailModel[0].setRotationPoint(-8.0F, -30.0F, -9.0F);
      this.tailModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 9, 2, 34, 0.0F, -1.0F, 3);
      this.tailModel[1].setRotationPoint(28.0F, -25.0F, -17.0F);
      this.tailModel[2].addTrapezoid(0.0F, 0.0F, 0.0F, 9, 2, 34, 0.0F, -1.0F, 2);
      this.tailModel[2].setRotationPoint(37.0F, -25.0F, -17.0F);
      this.tailModel[3].addTrapezoid(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F, -2.0F, 3);
      this.tailModel[3].setRotationPoint(13.0F, -37.0F, -32.0F);
      this.tailModel[4].addBox(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F);
      this.tailModel[4].setRotationPoint(29.0F, -37.0F, -32.0F);
      this.tailModel[5].addTrapezoid(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F, -2.0F, 2);
      this.tailModel[5].setRotationPoint(45.0F, -37.0F, -32.0F);
      this.tailModel[6].addTrapezoid(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F, -2.0F, 3);
      this.tailModel[6].setRotationPoint(13.0F, -37.0F, 16.0F);
      this.tailModel[7].addBox(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F);
      this.tailModel[7].setRotationPoint(29.0F, -37.0F, 16.0F);
      this.tailModel[8].addTrapezoid(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F, -2.0F, 2);
      this.tailModel[8].setRotationPoint(45.0F, -37.0F, 16.0F);
      this.tailModel[9].addTrapezoid(0.0F, 0.0F, 0.0F, 12, 2, 80, 0.0F, -1.0F, 3);
      this.tailModel[9].setRotationPoint(70.0F, -18.0F, -40.0F);
      this.tailModel[10].addBox(0.0F, 0.0F, 0.0F, 18, 10, 2, 0.0F);
      this.tailModel[10].setRotationPoint(70.0F, -20.0F, -41.0F);
      this.tailModel[11].addBox(0.0F, 0.0F, 0.0F, 13, 22, 2, 0.0F);
      this.tailModel[11].setRotationPoint(70.0F, -42.0F, -41.0F);
      this.tailModel[12].addBox(0.0F, 0.0F, 0.0F, 18, 10, 2, 0.0F);
      this.tailModel[12].setRotationPoint(70.0F, -20.0F, 39.0F);
      this.tailModel[13].addBox(0.0F, 0.0F, 0.0F, 13, 22, 2, 0.0F);
      this.tailModel[13].setRotationPoint(70.0F, -42.0F, 39.0F);
      this.rightWingModel = new ModelRendererTurbo[5];
      this.rightWingModel[0] = new ModelRendererTurbo(this, 207, 325, textureX, textureY);
      this.rightWingModel[1] = new ModelRendererTurbo(this, 59, 139, textureX, textureY);
      this.rightWingModel[2] = new ModelRendererTurbo(this, 0, 79, textureX, textureY);
      this.rightWingModel[3] = new ModelRendererTurbo(this, 21, 79, textureX, textureY);
      this.rightWingModel[4] = new ModelRendererTurbo(this, 84, 79, textureX, textureY);
      this.rightWingModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 32, 4, 70, 0.0F, -2.0F, 1);
      this.rightWingModel[0].setRotationPoint(-16.0F, -11.0F, -29.0F);
      this.rightWingModel[0].rotateAngleX = 3.036873F;
      this.rightWingModel[1].addBox(0.0F, 0.0F, 0.0F, 28, 0, 6, 0.0F);
      this.rightWingModel[1].setRotationPoint(-14.0F, -20.5F, -98.0F);
      this.rightWingModel[1].rotateAngleX = -2.583087F;
      this.rightWingModel[2].addTrapezoid(0.0F, 0.0F, 0.0F, 5, 6, 4, 0.0F, -1.0F, 3);
      this.rightWingModel[2].setRotationPoint(-27.0F, -15.0F, -31.0F);
      this.rightWingModel[3].addBox(0.0F, 0.0F, 0.0F, 26, 6, 4, 0.0F);
      this.rightWingModel[3].setRotationPoint(-22.0F, -15.0F, -31.0F);
      this.rightWingModel[4].addTrapezoid(0.0F, 0.0F, 0.0F, 12, 6, 4, 0.0F, -2.0F, 2);
      this.rightWingModel[4].setRotationPoint(4.0F, -15.0F, -31.0F);
      this.leftWingModel = new ModelRendererTurbo[5];
      this.leftWingModel[0] = new ModelRendererTurbo(this, 0, 325, textureX, textureY);
      this.leftWingModel[1] = new ModelRendererTurbo(this, 59, 129, textureX, textureY);
      this.leftWingModel[2] = new ModelRendererTurbo(this, 0, 66, textureX, textureY);
      this.leftWingModel[3] = new ModelRendererTurbo(this, 21, 66, textureX, textureY);
      this.leftWingModel[4] = new ModelRendererTurbo(this, 84, 66, textureX, textureY);
      this.leftWingModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 32, 4, 70, 0.0F, -2.0F, 1);
      this.leftWingModel[0].setRotationPoint(-16.0F, -15.0F, 29.0F);
      this.leftWingModel[0].rotateAngleX = 0.1047198F;
      this.leftWingModel[1].addBox(0.0F, 0.0F, 0.0F, 28, 0, 6, 0.0F);
      this.leftWingModel[1].setRotationPoint(-14.0F, -20.3F, 98.5F);
      this.leftWingModel[1].rotateAngleX = -0.5585054F;
      this.leftWingModel[2].addTrapezoid(0.0F, 0.0F, 0.0F, 5, 6, 4, 0.0F, -1.0F, 3);
      this.leftWingModel[2].setRotationPoint(-27.0F, -15.0F, 27.0F);
      this.leftWingModel[3].addBox(0.0F, 0.0F, 0.0F, 26, 6, 4, 0.0F);
      this.leftWingModel[3].setRotationPoint(-22.0F, -15.0F, 27.0F);
      this.leftWingModel[4].addTrapezoid(0.0F, 0.0F, 0.0F, 12, 6, 4, 0.0F, -2.0F, 2);
      this.leftWingModel[4].setRotationPoint(4.0F, -15.0F, 27.0F);
      this.propellerModels = new ModelRendererTurbo[2][3];
      this.propellerModels[0][0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[0][1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[0][2] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[0][0].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[0][1].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[0][2].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[0][0].setRotationPoint(12.0F, -29.0F, -24.0F);
      this.propellerModels[0][1].setRotationPoint(12.0F, -29.0F, -24.0F);
      this.propellerModels[0][2].setRotationPoint(12.0F, -29.0F, -24.0F);
      this.propellerModels[1][0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[1][1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[1][2] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.propellerModels[1][0].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[1][1].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[1][2].addBox(-0.0F, -0.0F, -0.0F, 1, 4, 1, 0.0F);
      this.propellerModels[1][0].setRotationPoint(12.0F, -29.0F, 24.0F);
      this.propellerModels[1][1].setRotationPoint(12.0F, -29.0F, 24.0F);
      this.propellerModels[1][2].setRotationPoint(12.0F, -29.0F, 24.0F);
      this.yawFlapModel = new ModelRendererTurbo[2];
      this.yawFlapModel[0] = new ModelRendererTurbo(this, 176, 126, textureX, textureY);
      this.yawFlapModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 5, 22, 2, 0.0F, -1.0F, 2);
      this.yawFlapModel[0].setRotationPoint(83.0F, -42.0F, -41.0F);
      this.yawFlapModel[1] = new ModelRendererTurbo(this, 176, 126, textureX, textureY);
      this.yawFlapModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 5, 22, 2, 0.0F, -1.0F, 2);
      this.yawFlapModel[1].setRotationPoint(83.0F, -42.0F, 39.0F);
      this.pitchFlapRightModel = new ModelRendererTurbo[1];
      this.pitchFlapRightModel[0] = new ModelRendererTurbo(this, 346, 477, textureX, textureY);
      this.pitchFlapRightModel[0].addBox(0.0F, 0.0F, 0.0F, 5, 2, 33, 0.0F);
      this.pitchFlapRightModel[0].setRotationPoint(82.0F, -18.0F, -39.0F);
      this.pitchFlapLeftModel = new ModelRendererTurbo[1];
      this.pitchFlapLeftModel[0] = new ModelRendererTurbo(this, 267, 477, textureX, textureY);
      this.pitchFlapLeftModel[0].addBox(0.0F, 0.0F, 0.0F, 5, 2, 33, 0.0F);
      this.pitchFlapLeftModel[0].setRotationPoint(82.0F, -18.0F, 6.0F);
      this.topWingModel = new ModelRendererTurbo[0];
      this.pitchFlapLeftWingModel = new ModelRendererTurbo[0];
      this.pitchFlapRightWingModel = new ModelRendererTurbo[0];
      this.flipAll();
      // Manus Legacy alignment: centres the model where the definition expects it.
      this.translateAll(-8.09F, 0F, 0F);
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
