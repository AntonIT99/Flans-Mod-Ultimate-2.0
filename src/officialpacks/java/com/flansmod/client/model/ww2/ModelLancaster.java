//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\alpha\Documents\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3\1.7.10 stable mappings"!

package com.flansmod.client.model.ww2;

import com.flansmod.client.model.ModelPlane;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Shape2D;

public class ModelLancaster extends ModelPlane {
   private int textureX = 512;
   private int textureY = 512;

   public ModelLancaster() {
      this.bodyModel = new ModelRendererTurbo[25];
      this.bodyModel[0] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.bodyModel[0].addTrapezoid(-160.0F, -80.0F, -16.0F, 16, 32, 32, 0.0F, -4.0F, 3);
      this.bodyModel[18] = new ModelRendererTurbo(this, 0, 0, this.textureX, this.textureY);
      this.bodyModel[18].flip = true;
      this.bodyModel[18].addTrapezoid(-160.0F, -80.0F, -16.0F, 16, 32, 32, 0.0F, -4.0F, 3);
      this.bodyModel[1] = new ModelRendererTurbo(this, 64, 0, this.textureX, this.textureY);
      this.bodyModel[1].addBox(-144.0F, -80.0F, -15.0F, 16, 1, 30, 0.0F);
      this.bodyModel[2] = new ModelRendererTurbo(this, 127, 1, this.textureX, this.textureY);
      this.bodyModel[2].addBox(-144.0F, -49.0F, -15.0F, 112, 1, 30, 0.0F);
      this.bodyModel[3] = new ModelRendererTurbo(this, 64, 32, this.textureX, this.textureY);
      this.bodyModel[3].addTrapezoid(-128.0F, -96.0F, -16.0F, 64, 16, 32, 0.0F, -8.0F, 4);
      this.bodyModel[16] = new ModelRendererTurbo(this, 64, 32, this.textureX, this.textureY);
      this.bodyModel[16].flip = true;
      this.bodyModel[16].addTrapezoid(-128.0F, -96.0F, -16.0F, 64, 16, 32, 0.0F, -8.0F, 4);
      this.bodyModel[4] = new ModelRendererTurbo(this, 0, 80, this.textureX, this.textureY);
      this.bodyModel[4].addBox(-144.0F, -80.0F, -16.0F, 112, 32, 1, 0.0F);
      this.bodyModel[5] = new ModelRendererTurbo(this, 0, 80, this.textureX, this.textureY);
      this.bodyModel[5].addBox(-144.0F, -80.0F, -16.0F, 112, 32, 1, 0.0F);
      this.bodyModel[5].setRotationPoint(-176.0F, 0.0F, 0.0F);
      this.bodyModel[5].rotateAngleY = (float) Math.PI;
      this.bodyModel[19] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      this.bodyModel[19].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      this.bodyModel[19].setRotationPoint(-116.0F, -88.0F, 0.0F);
      this.bodyModel[19].rotateAngleY = (float) Math.PI;
      this.bodyModel[20] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      this.bodyModel[20].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      this.bodyModel[20].setRotationPoint(-116.0F, -88.0F, 0.0F);
      this.bodyModel[20].rotateAngleY = (float) Math.PI;
      this.bodyModel[21] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      this.bodyModel[21].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      this.bodyModel[21].setRotationPoint(-90.0F, -76.0F, 0.0F);
      this.bodyModel[21].rotateAngleY = (float) Math.PI;
      this.bodyModel[22] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      this.bodyModel[22].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      this.bodyModel[22].setRotationPoint(-90.0F, -76.0F, 0.0F);
      this.bodyModel[22].rotateAngleY = (float) Math.PI;
      this.bodyModel[23] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      this.bodyModel[23].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      this.bodyModel[23].setRotationPoint(-68.0F, -76.0F, 0.0F);
      this.bodyModel[23].rotateAngleY = (float) Math.PI;
      this.bodyModel[24] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      this.bodyModel[24].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      this.bodyModel[24].setRotationPoint(-68.0F, -76.0F, 0.0F);
      this.bodyModel[24].rotateAngleY = (float) Math.PI;
      this.bodyModel[6] = new ModelRendererTurbo(this, 226, 80, this.textureX, this.textureY);
      this.bodyModel[6].addBox(-71.0F, -96.0F, -15.0F, 103, 1, 30, 0.0F);
      this.bodyModel[7] = new ModelRendererTurbo(this, 432, 24, this.textureX, this.textureY);
      this.bodyModel[7].addBox(-72.0F, -96.0F, -16.0F, 1, 16, 32, 0.0F);
      this.bodyModel[8] = new ModelRendererTurbo(this, 0, 245, this.textureX, this.textureY);
      this.bodyModel[8].addBox(0.0F, 0.0F, -16.0F, 199, 16, 1, 0.0F);
      this.bodyModel[8].setRotationPoint(-71.0F, -96.0F, 0.0F);
      this.bodyModel[9] = new ModelRendererTurbo(this, 0, 245, this.textureX, this.textureY);
      this.bodyModel[9].addBox(0.0F, 0.0F, -16.0F, 199, 16, 1, 0.0F);
      this.bodyModel[9].setRotationPoint(-71.0F, -96.0F, 0.0F);
      this.bodyModel[9].doMirror(false, false, true);
      this.bodyModel[10] = new ModelRendererTurbo(this, 38, 262, this.textureX, this.textureY);
      this.bodyModel[10].addBox(0.0F, 0.0F, -16.0F, 160, 16, 1, 0.0F);
      this.bodyModel[10].setRotationPoint(-32.0F, -80.0F, 0.0F);
      this.bodyModel[11] = new ModelRendererTurbo(this, 38, 262, this.textureX, this.textureY);
      this.bodyModel[11].addBox(0.0F, 0.0F, -16.0F, 160, 16, 1, 0.0F);
      this.bodyModel[11].setRotationPoint(-32.0F, -80.0F, 0.0F);
      this.bodyModel[11].doMirror(false, false, true);
      this.bodyModel[12] = new ModelRendererTurbo(this, 144, 279, this.textureX, this.textureY);
      this.bodyModel[12].addTrapezoid(32.0F, -112.0F, -16.0F, 32, 16, 32, 0.0F, -8.0F, 4);
      this.bodyModel[17] = new ModelRendererTurbo(this, 144, 279, this.textureX, this.textureY);
      this.bodyModel[17].flip = true;
      this.bodyModel[17].addTrapezoid(32.0F, -112.0F, -16.0F, 32, 16, 32, 0.0F, -8.0F, 4);
      ModelRendererTurbo[][] dorsalModel = new ModelRendererTurbo[][]{new ModelRendererTurbo[4], null, null};
      dorsalModel[0][0] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      dorsalModel[0][0].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      dorsalModel[0][1] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      dorsalModel[0][1].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      dorsalModel[0][2] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      dorsalModel[0][2].addBox(8.0F, -6.0F, -7.0F, 2, 24, 2, 0.0F);
      dorsalModel[0][3] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      dorsalModel[0][3].addBox(8.0F, -6.0F, 5.0F, 2, 24, 2, 0.0F);
      dorsalModel[1] = new ModelRendererTurbo[3];
      dorsalModel[1][0] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      dorsalModel[1][0].addBox(8.0F, -1.0F, 3.0F, 24, 2, 2, 0.0F);
      dorsalModel[1][1] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      dorsalModel[1][1].addBox(8.0F, -1.0F, -5.0F, 24, 2, 2, 0.0F);
      dorsalModel[1][2] = new ModelRendererTurbo(this, 40, 415, this.textureX, this.textureY);
      dorsalModel[1][2].addBox(8.0F, -1.0F, -3.0F, 2, 2, 6, 0.0F);
      dorsalModel[2] = new ModelRendererTurbo[0];

      for (ModelRendererTurbo[] dorsalGunParts : dorsalModel) {
         for (ModelRendererTurbo dorsalGunPart : dorsalGunParts) {
            dorsalGunPart.setRotationPoint(48.0F, -104.0F, 0.0F);
         }
      }

      this.registerGunModel("Dorsal", dorsalModel);
      ModelRendererTurbo[][] noseGunModel = new ModelRendererTurbo[][]{new ModelRendererTurbo[4], null, null};
      noseGunModel[0][0] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      noseGunModel[0][0].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      noseGunModel[0][1] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      noseGunModel[0][1].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      noseGunModel[0][2] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      noseGunModel[0][2].addBox(8.0F, -6.0F, -7.0F, 2, 24, 2, 0.0F);
      noseGunModel[0][3] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      noseGunModel[0][3].addBox(8.0F, -6.0F, 5.0F, 2, 24, 2, 0.0F);
      noseGunModel[1] = new ModelRendererTurbo[3];
      noseGunModel[1][0] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      noseGunModel[1][0].addBox(8.0F, -1.0F, 3.0F, 24, 2, 2, 0.0F);
      noseGunModel[1][1] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      noseGunModel[1][1].addBox(8.0F, -1.0F, -5.0F, 24, 2, 2, 0.0F);
      noseGunModel[1][2] = new ModelRendererTurbo(this, 40, 415, this.textureX, this.textureY);
      noseGunModel[1][2].addBox(8.0F, -1.0F, -3.0F, 2, 2, 6, 0.0F);
      noseGunModel[2] = new ModelRendererTurbo[0];

      for (ModelRendererTurbo[] noseGunParts : noseGunModel) {
         for (ModelRendererTurbo noseGunPart : noseGunParts) {
            noseGunPart.setRotationPoint(-148.0F, -70.0F, 0.0F);
         }
      }

      this.registerGunModel("Nose", noseGunModel);
      ModelRendererTurbo[][] tailGunModel = new ModelRendererTurbo[][]{new ModelRendererTurbo[4], null, null};
      tailGunModel[0][0] = new ModelRendererTurbo(this, 0, 445, this.textureX, this.textureY);
      tailGunModel[0][0].addBox(-8.0F, 16.0F, -8.0F, 16, 2, 16, 0.0F);
      tailGunModel[0][1] = new ModelRendererTurbo(this, 0, 463, this.textureX, this.textureY);
      tailGunModel[0][1].addBox(-8.0F, 0.0F, -8.0F, 2, 16, 16, 0.0F);
      tailGunModel[0][2] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      tailGunModel[0][2].addBox(8.0F, -6.0F, -7.0F, 2, 24, 2, 0.0F);
      tailGunModel[0][3] = new ModelRendererTurbo(this, 40, 381, this.textureX, this.textureY);
      tailGunModel[0][3].addBox(8.0F, -6.0F, 5.0F, 2, 24, 2, 0.0F);
      tailGunModel[1] = new ModelRendererTurbo[3];
      tailGunModel[1][0] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      tailGunModel[1][0].addBox(8.0F, -1.0F, 3.0F, 24, 2, 2, 0.0F);
      tailGunModel[1][1] = new ModelRendererTurbo(this, 40, 411, this.textureX, this.textureY);
      tailGunModel[1][1].addBox(8.0F, -1.0F, -5.0F, 24, 2, 2, 0.0F);
      tailGunModel[1][2] = new ModelRendererTurbo(this, 40, 415, this.textureX, this.textureY);
      tailGunModel[1][2].addBox(8.0F, -1.0F, -3.0F, 2, 2, 6, 0.0F);
      tailGunModel[2] = new ModelRendererTurbo[0];

      for (ModelRendererTurbo[] tailGunParts : tailGunModel) {
         for (ModelRendererTurbo tailGunPart : tailGunParts) {
            tailGunPart.setRotationPoint(128.0F, -88.0F, 0.0F);
         }
      }

      this.registerGunModel("Tail", tailGunModel);
      this.bodyModel[13] = new ModelRendererTurbo(this, 52, 362, this.textureX, this.textureY);
      this.bodyModel[13].addShape3D(128.0F, -65.0F, -15.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(160.0, 16.0, 160, 0), new Coord2D(160.0, 17.0, 160, 1), new Coord2D(0.0, 1.0, 0, 1)}), 30.0F, 160, 1, 322, 30, 0, new float[]{1.0F, 160.0F, 1.0F, 160.0F});
      this.bodyModel[13].rotateAngleX = (float) Math.PI;
      this.bodyModel[14] = new ModelRendererTurbo(this, 52, 345, this.textureX, this.textureY);
      this.bodyModel[14].addShape3D(128.0F, -64.0F, -16.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(160.0, 0.0, 160, 0), new Coord2D(160.0, 16.0, 160, 16)}), 1.0F, 160, 16, 336, 1, 0, new float[]{160.0F, 16.0F, 160.0F});
      this.bodyModel[14].rotateAngleX = (float) Math.PI;
      this.bodyModel[15] = new ModelRendererTurbo(this, 52, 394, this.textureX, this.textureY);
      this.bodyModel[15].addShape3D(128.0F, -64.0F, 15.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(160.0, 0.0, 160, 0), new Coord2D(160.0, 16.0, 160, 16)}), 1.0F, 160, 16, 336, 1, 0, new float[]{160.0F, 16.0F, 160.0F});
      this.bodyModel[15].rotateAngleX = (float) Math.PI;
      this.tailModel = new ModelRendererTurbo[7];
      this.tailModel[0] = new ModelRendererTurbo(this, 0, 279, this.textureX, this.textureY);
      this.tailModel[0].addBox(96.0F, 16.0F, -82.0F, 16, 64, 2, 0.0F);
      this.tailModel[0].rotateAngleX = (float) (-Math.PI / 2);
      this.tailModel[1] = new ModelRendererTurbo(this, 0, 279, this.textureX, this.textureY);
      this.tailModel[1].addBox(96.0F, 16.0F, -82.0F, 16, 64, 2, 0.0F);
      this.tailModel[1].doMirror(false, true, false);
      this.tailModel[1].rotateAngleX = (float) (-Math.PI / 2);
      this.tailModel[2] = new ModelRendererTurbo(this, 72, 279, this.textureX, this.textureY);
      this.tailModel[2].addBox(96.0F, -128.0F, 80.0F, 16, 64, 2, 0.0F);
      this.tailModel[3] = new ModelRendererTurbo(this, 72, 279, this.textureX, this.textureY);
      this.tailModel[3].addBox(96.0F, -128.0F, -82.0F, 16, 64, 2, 0.0F);
      this.tailModel[4] = new ModelRendererTurbo(this, 240, 279, this.textureX, this.textureY);
      this.tailModel[4].addBox(64.0F, -96.0F, -15.0F, 64, 1, 30, 0.0F);
      this.tailModel[5] = new ModelRendererTurbo(this, 304, 111, this.textureX, this.textureY);
      this.tailModel[5].addTrapezoid(128.0F, -96.0F, -16.0F, 16, 32, 32, 0.0F, -6.0F, 2);
      this.tailModel[6] = new ModelRendererTurbo(this, 304, 111, this.textureX, this.textureY);
      this.tailModel[6].flip = true;
      this.tailModel[6].addTrapezoid(128.0F, -96.0F, -16.0F, 16, 32, 32, 0.0F, -6.0F, 2);
      this.tailWheelModel = new ModelRendererTurbo[2];
      this.tailWheelModel[0] = new ModelRendererTurbo(this, 36, 463, this.textureX, this.textureY);
      this.tailWheelModel[0].addBox(98.0F, -63.0F, -3.0F, 4, 16, 6);
      this.tailWheelModel[1] = new ModelRendererTurbo(this, 84, 447, this.textureX, this.textureY);
      this.tailWheelModel[1].addBox(94.0F, -53.0F, -2.0F, 12, 12, 4);
      this.leftWingModel = new ModelRendererTurbo[6];
      this.leftWingModel[0] = new ModelRendererTurbo(this, 0, 113, this.textureX, this.textureY);
      this.leftWingModel[0].addBox(-64.0F, 16.0F, -82.0F, 64, 96, 4, 0.0F);
      this.leftWingModel[0].rotateAngleX = (float) (-Math.PI / 2);
      this.leftWingModel[1] = new ModelRendererTurbo(this, 136, 113, this.textureX, this.textureY);
      this.leftWingModel[1].addTrapezoid(-64.0F, 112.0F, -82.0F, 80, 128, 4, 0.0F, -2.0F, 5);
      this.leftWingModel[1].rotateAngleX = (float) (-Math.PI / 2);
      this.leftWingModel[2] = new ModelRendererTurbo(this, 256, 32, this.textureX, this.textureY);
      this.leftWingModel[2].addTrapezoid(-112.0F, -80.0F, -64.0F, 64, 24, 24, 0.0F, -2.0F, 5);
      this.leftWingModel[3] = new ModelRendererTurbo(this, 256, 32, this.textureX, this.textureY);
      this.leftWingModel[3].addTrapezoid(-96.0F, -80.0F, -128.0F, 64, 24, 24, 0.0F, -2.0F, 5);
      this.leftWingModel[4] = new ModelRendererTurbo(this, 408, 24, this.textureX, this.textureY);
      this.leftWingModel[4].addBox(-115.0F, -76.0F, -60.0F, 6, 16, 16, 0.0F);
      this.leftWingModel[5] = new ModelRendererTurbo(this, 408, 24, this.textureX, this.textureY);
      this.leftWingModel[5].addBox(-99.0F, -76.0F, -124.0F, 6, 16, 16, 0.0F);
      this.leftWingWheelModel = new ModelRendererTurbo[2];
      this.leftWingWheelModel[0] = new ModelRendererTurbo(this, 48, 415, this.textureX, this.textureY);
      this.leftWingWheelModel[0].addBox(-82.0F, -56.0F, -58.0F, 4, 24, 12);
      this.leftWingWheelModel[1] = new ModelRendererTurbo(this, 80, 415, this.textureX, this.textureY);
      this.leftWingWheelModel[1].addBox(-92.0F, -44.0F, -56.0F, 24, 24, 8);
      this.rightWingModel = new ModelRendererTurbo[6];
      this.rightWingModel[0] = new ModelRendererTurbo(this, 0, 113, this.textureX, this.textureY);
      this.rightWingModel[0].addBox(-64.0F, 16.0F, -82.0F, 64, 96, 4, 0.0F);
      this.rightWingModel[0].doMirror(false, true, false);
      this.rightWingModel[0].rotateAngleX = (float) (-Math.PI / 2);
      this.rightWingModel[1] = new ModelRendererTurbo(this, 136, 113, this.textureX, this.textureY);
      this.rightWingModel[1].addTrapezoid(-64.0F, 112.0F, -82.0F, 80, 128, 4, 0.0F, -2.0F, 5);
      this.rightWingModel[1].doMirror(false, true, false);
      this.rightWingModel[1].rotateAngleX = (float) (-Math.PI / 2);
      this.rightWingModel[2] = new ModelRendererTurbo(this, 256, 32, this.textureX, this.textureY);
      this.rightWingModel[2].addTrapezoid(-112.0F, -80.0F, 40.0F, 64, 24, 24, 0.0F, -2.0F, 5);
      this.rightWingModel[3] = new ModelRendererTurbo(this, 256, 32, this.textureX, this.textureY);
      this.rightWingModel[3].addTrapezoid(-96.0F, -80.0F, 104.0F, 64, 24, 24, 0.0F, -2.0F, 5);
      this.rightWingModel[4] = new ModelRendererTurbo(this, 408, 24, this.textureX, this.textureY);
      this.rightWingModel[4].addBox(-115.0F, -76.0F, 44.0F, 6, 16, 16, 0.0F);
      this.rightWingModel[5] = new ModelRendererTurbo(this, 408, 24, this.textureX, this.textureY);
      this.rightWingModel[5].addBox(-99.0F, -76.0F, 108.0F, 6, 16, 16, 0.0F);
      this.rightWingWheelModel = new ModelRendererTurbo[2];
      this.rightWingWheelModel[0] = new ModelRendererTurbo(this, 48, 415, this.textureX, this.textureY);
      this.rightWingWheelModel[0].addBox(-82.0F, -56.0F, 46.0F, 4, 24, 12);
      this.rightWingWheelModel[1] = new ModelRendererTurbo(this, 80, 415, this.textureX, this.textureY);
      this.rightWingWheelModel[1].addBox(-92.0F, -44.0F, 48.0F, 24, 24, 8);
      this.propellerModels = new ModelRendererTurbo[4][3];
      this.propellerModels[1] = this.makeProp(-114, -68, 52);
      this.propellerModels[0] = this.makeProp(-114, -68, -52);
      this.propellerModels[3] = this.makeProp(-98, -68, 116);
      this.propellerModels[2] = this.makeProp(-98, -68, -116);
      this.yawFlapModel = new ModelRendererTurbo[2];
      this.yawFlapModel[0] = new ModelRendererTurbo(this, 108, 279, this.textureX, this.textureY);
      this.yawFlapModel[0].addBox(0.0F, -48.0F, -1.0F, 16, 64, 2, 0.0F);
      this.yawFlapModel[0].setPosition(112.0F, -80.0F, 81.0F);
      this.yawFlapModel[1] = new ModelRendererTurbo(this, 108, 279, this.textureX, this.textureY);
      this.yawFlapModel[1].addBox(0.0F, -48.0F, -1.0F, 16, 64, 2, 0.0F);
      this.yawFlapModel[1].setPosition(112.0F, -80.0F, -81.0F);
      this.pitchFlapLeftWingModel = new ModelRendererTurbo[1];
      this.pitchFlapLeftWingModel[0] = new ModelRendererTurbo(this, 0, 345, this.textureX, this.textureY);
      this.pitchFlapLeftWingModel[0].addBox(0.0F, -48.0F, -2.0F, 16, 96, 4, 0.0F);
      this.pitchFlapLeftWingModel[0].rotateAngleX = 1.570796F;
      this.pitchFlapLeftWingModel[0].setPosition(0.0F, -80.0F, -64.0F);
      this.pitchFlapLeftModel = new ModelRendererTurbo[1];
      this.pitchFlapLeftModel[0] = new ModelRendererTurbo(this, 36, 279, this.textureX, this.textureY);
      this.pitchFlapLeftModel[0].addBox(0.0F, -32.0F, -1.0F, 16, 64, 2, 0.0F);
      this.pitchFlapLeftModel[0].rotateAngleX = 1.570796F;
      this.pitchFlapLeftModel[0].setPosition(112.0F, -81.0F, 48.0F);
      this.pitchFlapRightWingModel = new ModelRendererTurbo[1];
      this.pitchFlapRightWingModel[0] = new ModelRendererTurbo(this, 0, 345, this.textureX, this.textureY);
      this.pitchFlapRightWingModel[0].addBox(0.0F, -48.0F, -2.0F, 16, 96, 4, 0.0F);
      this.pitchFlapRightWingModel[0].doMirror(false, true, false);
      this.pitchFlapRightWingModel[0].rotateAngleX = 1.570796F;
      this.pitchFlapRightWingModel[0].setPosition(0.0F, -80.0F, 64.0F);
      this.pitchFlapRightModel = new ModelRendererTurbo[1];
      this.pitchFlapRightModel[0] = new ModelRendererTurbo(this, 36, 279, this.textureX, this.textureY);
      this.pitchFlapRightModel[0].addBox(0.0F, -32.0F, -1.0F, 16, 64, 2, 0.0F);
      this.pitchFlapRightModel[0].rotateAngleX = 1.570796F;
      this.pitchFlapRightModel[0].setPosition(112.0F, -81.0F, -48.0F);
      this.translateAll(0, 52, 0);
      this.flipAll();
   }

   private ModelRendererTurbo[] makeProp(int i, int j, int k) {
      ModelRendererTurbo[] prop = new ModelRendererTurbo[]{new ModelRendererTurbo(this, 40, 345, this.textureX, this.textureY), new ModelRendererTurbo(this, 40, 345, this.textureX, this.textureY), new ModelRendererTurbo(this, 40, 345, this.textureX, this.textureY)};
      prop[0].addBox(-0.0F, -32.0F, -2.0F, 2, 32, 4, 0.0F);
      prop[0].setRotationPoint(i, j, k);
      prop[1].addBox(-0.0F, -32.0F, -2.0F, 2, 32, 4, 0.0F);
      prop[1].setRotationPoint(i, j, k);
      prop[2].addBox(-0.0F, -32.0F, -2.0F, 2, 32, 4, 0.0F);
      prop[2].setRotationPoint(i, j, k);
      return prop;
   }
}
