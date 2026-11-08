package com.flansmod.client.model.manus_legacy;

import com.flansmod.client.model.ModelPlane;
import com.flansmod.client.tmt.*;

public class ModelF22 extends ModelPlane {
   public ModelF22() {
      int textureX = 512;
      int textureY = 512;
      boolean VarGear = false;
      this.bodyModel = new ModelRendererTurbo[40];
      this.bodyModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 27, 488, textureX, textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 94, 435, textureX, textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 94, 393, textureX, textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[20] = new ModelRendererTurbo(this, 0, 460, textureX, textureY);
      this.bodyModel[21] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.bodyModel[22] = new ModelRendererTurbo(this, 0, 376, textureX, textureY);
      this.bodyModel[23] = new ModelRendererTurbo(this, 0, 394, textureX, textureY);
      this.bodyModel[24] = new ModelRendererTurbo(this, 0, 360, textureX, textureY);
      this.bodyModel[25] = new ModelRendererTurbo(this, 0, 485, textureX, textureY);
      this.bodyModel[26] = new ModelRendererTurbo(this, 0, 446, textureX, textureY);
      this.bodyModel[27] = new ModelRendererTurbo(this, 0, 446, textureX, textureY);
      this.bodyModel[28] = new ModelRendererTurbo(this, 0, 430, textureX, textureY);
      this.bodyModel[29] = new ModelRendererTurbo(this, 0, 413, textureX, textureY);
      this.bodyModel[30] = new ModelRendererTurbo(this, 300, 500, textureX, textureY);
      this.bodyModel[31] = new ModelRendererTurbo(this, 330, 500, textureX, textureY);
      this.bodyModel[32] = new ModelRendererTurbo(this, 350, 500, textureX, textureY);
      this.bodyModel[33] = new ModelRendererTurbo(this, 360, 500, textureX, textureY);
      this.bodyModel[34] = new ModelRendererTurbo(this, 300, 470, textureX, textureY);
      this.bodyModel[35] = new ModelRendererTurbo(this, 340, 470, textureX, textureY);
      this.bodyModel[36] = new ModelRendererTurbo(this, 390, 470, textureX, textureY);
      this.bodyModel[37] = new ModelRendererTurbo(this, 300, 440, textureX, textureY);
      this.bodyModel[38] = new ModelRendererTurbo(this, 340, 440, textureX, textureY);
      this.bodyModel[39] = new ModelRendererTurbo(this, 390, 440, textureX, textureY);
      this.bodyModel[0].addTrapezoid(0.0F, 0.0F, 0.0F, 16, 16, 16, 0.0F, -2.0F, 3);
      this.bodyModel[0].setRotationPoint(-109.0F, -23.0F, -8.0F);
      this.bodyModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 20, 12, 12, 0.0F, -6.0F, 3);
      this.bodyModel[1].setRotationPoint(-129.0F, -21.0F, -6.0F);
      this.bodyModel[2].addBox(0.0F, 0.0F, 0.0F, 112, 1, 48, 0.0F);
      this.bodyModel[2].setRotationPoint(-61.0F, -17.0F, -24.0F);
      this.bodyModel[3].addBox(0.0F, 0.0F, 0.0F, 82, 1, 32, 0.0F);
      this.bodyModel[3].setRotationPoint(-51.0F, -5.0F, -16.0F);
      this.bodyModel[4].addTrapezoid(0.0F, 0.0F, 0.0F, 32, 22, 22, 0.0F, -2.0F, 2);
      this.bodyModel[4].setRotationPoint(-61.0F, -26.0F, -11.0F);
      this.bodyModel[5].addTrapezoid(0.0F, 0.0F, 0.0F, 32, 18, 18, 0.0F, -1.0F, 2);
      this.bodyModel[5].setRotationPoint(-29.0F, -24.0F, -9.0F);
      this.bodyModel[6].addTrapezoid(0.0F, 0.0F, 0.0F, 42, 16, 16, 0.0F, -1.0F, 2);
      this.bodyModel[6].setRotationPoint(3.0F, -23.0F, -8.0F);
      this.bodyModel[7].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(16.0, 0.0, 16, 0), new Coord2D(16.0, 6.0, 16, 6)}), 62.0F, 17, 6, 41, 112, 0, new float[]{17.0F, 6.0F, 18.0F});
      this.bodyModel[7].rotateAngleY = (float) (Math.PI / 2);
      this.bodyModel[7].setRotationPoint(-61.0F, -17.0F, 24.0F);
      this.bodyModel[8].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(16.0, 0.0, 16, 0), new Coord2D(16.0, 6.0, 16, 6)}), 62.0F, 17, 6, 41, 112, 0, new float[]{17.0F, 6.0F, 18.0F});
      this.bodyModel[8].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.bodyModel[8].setRotationPoint(1.0F, -17.0F, -24.0F);
      this.bodyModel[9].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(16.0, 0.0, 16, 0), new Coord2D(16.0, 5.0, 16, 5)}), 50.0F, 17, 5, 41, 112, 0, new float[]{17.0F, 6.0F, 18.0F});
      this.bodyModel[9].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.bodyModel[9].setRotationPoint(49.0F, -17.0F, -24.0F);
      this.bodyModel[10].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(16.0, 0.0, 16, 0), new Coord2D(16.0, 5.0, 16, 5)}), 50.0F, 17, 5, 41, 112, 0, new float[]{17.0F, 6.0F, 18.0F});
      this.bodyModel[10].rotateAngleY = (float) (Math.PI / 2);
      this.bodyModel[10].setRotationPoint(1.0F, -17.0F, 24.0F);
      this.bodyModel[11].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 0.0, 32, 0), new Coord2D(27.0, 10.0, 27, 10), new Coord2D(5.0, 10.0, 5, 10)}), 1.0F, 32, 10, 76, 1, 0, new float[]{32.0F, 11.0F, 22.0F, 11.0F});
      this.bodyModel[11].rotateAngleY = (float) (Math.PI / 2);
      this.bodyModel[11].rotateAngleX = (float) (-Math.PI / 2);
      this.bodyModel[11].setRotationPoint(-51.0F, -4.0F, 16.0F);
      this.bodyModel[12].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(92.0, 0.0, 92, 0), new Coord2D(92.0, 14.0, 92, 14), new Coord2D(10.0, 14.0, 10, 14)}), 1.0F, 92, 14, 205, 1, 0, new float[]{92.0F, 14.0F, 82.0F, 17.0F});
      this.bodyModel[12].rotateAngleY = (float) Math.PI;
      this.bodyModel[12].rotateAngleX = 3.752458F;
      this.bodyModel[12].setRotationPoint(-61.0F, -16.0F, 24.0F);
      this.bodyModel[13].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(92.0, 0.0, 92, 0), new Coord2D(82.0, 14.0, 82, 14), new Coord2D(0.0, 14.0, 0, 14)}), 1.0F, 92, 14, 205, 1, 0, new float[]{92.0F, 17.0F, 82.0F, 14.0F});
      this.bodyModel[13].rotateAngleX = 3.752458F;
      this.bodyModel[13].setRotationPoint(31.0F, -16.0F, -24.0F);
      this.bodyModel[14].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(125.0, 0.0, 125, 0), new Coord2D(125.0, 18.0, 125, 18), new Coord2D(43.0, 18.0, 43, 18), new Coord2D(13.0, 13.0, 13, 13)}), 1.0F, 125, 18, 274, 1, 0, new float[]{125.0F, 18.0F, 82.0F, 30.0F, 18.0F});
      this.bodyModel[14].rotateAngleY = (float) Math.PI;
      this.bodyModel[14].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.bodyModel[14].setRotationPoint(-74.0F, -16.0F, -11.0F);
      this.bodyModel[15].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(125.0, 0.0, 125, 0), new Coord2D(125.0, 18.0, 125, 18), new Coord2D(43.0, 18.0, 43, 18), new Coord2D(13.0, 13.0, 13, 13)}), 1.0F, 125, 18, 274, 1, 0, new float[]{125.0F, 18.0F, 82.0F, 30.0F, 18.0F});
      this.bodyModel[15].rotateAngleY = (float) Math.PI;
      this.bodyModel[15].rotateAngleX = (float) (Math.PI / 2);
      this.bodyModel[15].setRotationPoint(-74.0F, -17.0F, 11.0F);
      this.bodyModel[16].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(14.0, 0.0, 14, 0), new Coord2D(13.0, 13.0, 13, 13)}), 1.0F, 14, 13, 45, 1, 0, new float[]{14.0F, 13.0F, 18.0F});
      this.bodyModel[16].rotateAngleY = (float) Math.PI;
      this.bodyModel[16].rotateAngleX = 4.362389F;
      this.bodyModel[16].rotateAngleZ = 0.36F;
      this.bodyModel[16].setRotationPoint(-74.0F, -16.0F, -11.0F);
      this.bodyModel[17].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(14.0, 0.0, 14, 0), new Coord2D(13.0, 13.0, 13, 13)}), 1.0F, 14, 13, 45, 1, 0, new float[]{14.0F, 13.0F, 18.0F});
      this.bodyModel[17].rotateAngleY = (float) Math.PI;
      this.bodyModel[17].rotateAngleX = 1.9207964F;
      this.bodyModel[17].rotateAngleZ = 0.36F;
      this.bodyModel[17].setRotationPoint(-74.0F, -17.0F, 11.0F);
      this.bodyModel[18].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 4.0, 32, 4), new Coord2D(32.0, 18.0, 32, 18), new Coord2D(0.0, 22.0, 0, 22)}), 1.0F, 32, 22, 100, 1, 0, new float[]{32.0F, 14.0F, 32.0F, 22.0F});
      this.bodyModel[18].setRotationPoint(-61.0F, -4.0F, -10.0F);
      this.bodyModel[18].rotateAngleY = -0.09599311F;
      this.bodyModel[19].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 4.0, 32, 4), new Coord2D(32.0, 18.0, 32, 18), new Coord2D(0.0, 22.0, 0, 22)}), 1.0F, 32, 22, 100, 1, 0, new float[]{32.0F, 14.0F, 32.0F, 22.0F});
      this.bodyModel[19].setRotationPoint(-61.0F, -4.0F, 11.0F);
      this.bodyModel[19].rotateAngleY = 0.09599311F;
      this.bodyModel[20].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 3.0, 32, 3), new Coord2D(32.0, 19.0, 32, 19), new Coord2D(0.0, 22.0, 0, 22)}), 1.0F, 32, 22, 102, 1, 0, new float[]{32.0F, 16.0F, 32.0F, 22.0F});
      this.bodyModel[20].setRotationPoint(-61.0F, -26.0F, 11.0F);
      this.bodyModel[20].rotateAngleX = (float) (Math.PI / 2);
      this.bodyModel[20].rotateAngleZ = 0.09599311F;
      this.bodyModel[21].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 3.0, 32, 3), new Coord2D(32.0, 19.0, 32, 19), new Coord2D(0.0, 22.0, 0, 22)}), 1.0F, 32, 22, 102, 1, 0, new float[]{32.0F, 16.0F, 32.0F, 22.0F});
      this.bodyModel[21].setRotationPoint(-61.0F, -5.0F, 11.0F);
      this.bodyModel[21].rotateAngleX = (float) (Math.PI / 2);
      this.bodyModel[21].rotateAngleZ = -0.09599311F;
      this.bodyModel[22].addBox(0.0F, 0.0F, 0.0F, 32, 1, 14, 0.0F);
      this.bodyModel[22].setRotationPoint(-93.0F, -9.0F, -7.0F);
      this.bodyModel[23].addBox(0.0F, 0.0F, 0.0F, 23, 2, 14, 0.0F);
      this.bodyModel[23].setRotationPoint(-70.0F, -8.0F, -7.0F);
      this.bodyModel[23].rotateAngleZ = 1.239184F;
      this.bodyModel[24].addBox(0.0F, 0.0F, 0.0F, 15, 1, 14, 0.0F);
      this.bodyModel[24].setRotationPoint(-88.0F, -8.0F, -7.0F);
      this.bodyModel[24].rotateAngleZ = 1.780236F;
      this.bodyModel[25].addBox(0.0F, 0.0F, 0.0F, 1, 17, 10, 0.0F);
      this.bodyModel[25].setRotationPoint(-81.0F, -35.0F, -5.0F);
      this.bodyModel[25].rotateAngleZ = -0.7766715F;
      this.bodyModel[26].addBox(0.0F, 0.0F, 0.0F, 17, 1, 10, 0.0F);
      this.bodyModel[26].setRotationPoint(-64.0F, -35.0F, -5.0F);
      this.bodyModel[26].rotateAngleZ = (float) (-Math.PI * 2.0 / 9.0);
      this.bodyModel[27].addBox(0.0F, 0.0F, 0.0F, 17, 1, 10, 0.0F);
      this.bodyModel[27].setRotationPoint(-81.0F, -35.0F, -5.0F);
      this.bodyModel[28].addBox(0.0F, 0.0F, 0.0F, 17, 13, 1, 0.0F);
      this.bodyModel[28].setRotationPoint(-81.0F, -35.0F, -5.0F);
      this.bodyModel[28].rotateAngleX = (float) (-Math.PI / 10);
      this.bodyModel[29].addBox(0.0F, 0.0F, 0.0F, 17, 1, 13, 0.0F);
      this.bodyModel[29].setRotationPoint(-81.0F, -35.0F, 5.0F);
      this.bodyModel[29].rotateAngleX = (float) (-Math.PI * 2.0 / 5.0);
      if (VarGear) {
         this.bodyModel[30].addBox(0.0F, 0.0F, 0.0F, 8, 8, 2, 0.0F);
         this.bodyModel[30].setRotationPoint(-58.0F, 2.0F, -1.0F);
         this.bodyModel[31].addBox(0.0F, 0.0F, 0.0F, 2, 6, 3, 0.0F);
         this.bodyModel[31].setRotationPoint(-55.0F, 0.0F, -1.5F);
         this.bodyModel[32].addBox(0.0F, 0.0F, 0.0F, 2, 6, 2, 0.0F);
         this.bodyModel[32].setRotationPoint(-54.0F, -5.0F, -1.0F);
         this.bodyModel[32].rotateAngleZ = -0.1570796F;
         this.bodyModel[33].addBox(0.0F, 0.0F, 0.0F, 18, 6, 1, 0.0F);
         this.bodyModel[33].setRotationPoint(-68.0F, -4.0F, 3.0F);
         this.bodyModel[34].addBox(0.0F, 0.0F, 0.0F, 10, 10, 2, 0.0F);
         this.bodyModel[34].setRotationPoint(18.0F, 0.0F, 23.0F);
         this.bodyModel[35].addBox(0.0F, 0.0F, 0.0F, 22, 11, 1, 0.0F);
         this.bodyModel[35].setRotationPoint(9.0F, -16.0F, 24.0F);
         this.bodyModel[35].rotateAngleX = 0.2094395F;
         this.bodyModel[36].addBox(0.0F, 0.0F, 0.0F, 2, 1, 14, 0.0F);
         this.bodyModel[36].setRotationPoint(24.0F, 4.0F, 24.0F);
         this.bodyModel[36].rotateAngleX = 1.082104F;
         this.bodyModel[36].rotateAngleY = 3.141593F;
         this.bodyModel[36].rotateAngleZ = 0.1745329F;
         this.bodyModel[37].addBox(0.0F, 0.0F, 0.0F, 10, 10, 2, 0.0F);
         this.bodyModel[37].setRotationPoint(18.0F, 0.0F, -25.0F);
         this.bodyModel[38].addBox(0.0F, 0.0F, 0.0F, 22, 11, 1, 0.0F);
         this.bodyModel[38].setRotationPoint(9.0F, -16.0F, -25.0F);
         this.bodyModel[38].rotateAngleX = -0.2094395F;
         this.bodyModel[39].addBox(0.0F, 0.0F, 0.0F, 2, 1, 14, 0.0F);
         this.bodyModel[39].setRotationPoint(22.0F, 4.0F, -24.0F);
         this.bodyModel[39].rotateAngleX = 1.082104F;
         this.bodyModel[39].rotateAngleZ = 0.1745329F;
      }

      this.tailModel = new ModelRendererTurbo[19];
      this.tailModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[1] = new ModelRendererTurbo(this, 98, 488, textureX, textureY);
      this.tailModel[2] = new ModelRendererTurbo(this, 98, 488, textureX, textureY);
      this.tailModel[3] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[4] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[5] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[6] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[7] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[8] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.tailModel[9] = new ModelRendererTurbo(this, 432, 0, textureX, textureY);
      this.tailModel[10] = new ModelRendererTurbo(this, 432, 0, textureX, textureY);
      this.tailModel[11] = new ModelRendererTurbo(this, 432, 0, textureX, textureY);
      this.tailModel[12] = new ModelRendererTurbo(this, 432, 0, textureX, textureY);
      this.tailModel[13] = new ModelRendererTurbo(this, 227, 146, textureX, textureY);
      this.tailModel[14] = new ModelRendererTurbo(this, 227, 171, textureX, textureY);
      this.tailModel[15] = new ModelRendererTurbo(this, 227, 146, textureX, textureY);
      this.tailModel[16] = new ModelRendererTurbo(this, 227, 171, textureX, textureY);
      this.tailModel[17] = new ModelRendererTurbo(this, 0, 144, textureX, textureY);
      this.tailModel[18] = new ModelRendererTurbo(this, 0, 144, textureX, textureY);
      this.tailModel[0].addBox(0.0F, 0.0F, 0.0F, 19, 10, 14, 0.0F);
      this.tailModel[0].setRotationPoint(45.0F, -22.0F, -7.0F);
      this.tailModel[1].addTrapezoid(0.0F, 0.0F, 0.0F, 84, 12, 12, 0.0F, -3.0F, 3);
      this.tailModel[1].setRotationPoint(-19.0F, -23.0F, -14.0F);
      this.tailModel[2].addTrapezoid(0.0F, 0.0F, 0.0F, 84, 12, 12, 0.0F, -3.0F, 3);
      this.tailModel[2].setRotationPoint(-19.0F, -23.0F, 2.0F);
      this.tailModel[3].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(34.0, 0.0, 34, 0), new Coord2D(34.0, 5.0, 34, 5), new Coord2D(2.0, 14.0, 2, 14)}), 1.0F, 34, 14, 86, 1, 0, new float[]{34.0F, 5.0F, 33.0F, 14.0F});
      this.tailModel[3].rotateAngleY = 3.3684855F;
      this.tailModel[3].rotateAngleX = 2.5307274F;
      this.tailModel[3].setRotationPoint(31.0F, -16.0F, -23.0F);
      this.tailModel[4].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(34.0, 0.0, 34, 0), new Coord2D(34.0, 5.0, 34, 5), new Coord2D(2.0, 14.0, 2, 14)}), 1.0F, 34, 14, 86, 1, 0, new float[]{34.0F, 5.0F, 33.0F, 14.0F});
      this.tailModel[4].rotateAngleY = 2.9147F;
      this.tailModel[4].rotateAngleX = 3.752458F;
      this.tailModel[4].setRotationPoint(31.0F, -16.0F, 24.0F);
      this.tailModel[5].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(32.0, 0.0, 32, 0), new Coord2D(30.0, 34.0, 30, 34), new Coord2D(2.0, 34.0, 2, 34)}), 1.0F, 32, 40, 140, 1, 0, new float[]{32.0F, 40.0F, 28.0F, 40.0F});
      this.tailModel[5].setRotationPoint(31.0F, -4.0F, -16.0F);
      this.tailModel[5].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[5].rotateAngleX = -1.3177236F;
      this.tailModel[6].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(21.0, 5.0, 21, 5), new Coord2D(0.0, 10.0, 0, 10)}), 4.0F, 21, 10, 53, 4, 0, new float[]{22.0F, 22.0F, 10.0F});
      this.tailModel[6].setRotationPoint(64.0F, -22.0F, 2.0F);
      this.tailModel[6].rotateAngleZ = (float) Math.PI;
      this.tailModel[7].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 3.0, 30, 3), new Coord2D(1.0, 16.0, 1, 16)}), 1.0F, 30, 16, 78, 1, 0, new float[]{30.0F, 32.0F, 16.0F});
      this.tailModel[7].rotateAngleY = (float) Math.PI;
      this.tailModel[7].rotateAngleX = 4.362389F;
      this.tailModel[7].rotateAngleZ = -0.1F;
      this.tailModel[7].setRotationPoint(49.0F, -19.5F, -13.0F);
      this.tailModel[8].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 3.0, 30, 3), new Coord2D(1.0, 16.0, 1, 16)}), 1.0F, 30, 16, 78, 1, 0, new float[]{30.0F, 32.0F, 16.0F});
      this.tailModel[8].rotateAngleY = (float) Math.PI;
      this.tailModel[8].rotateAngleX = 1.9207964F;
      this.tailModel[8].rotateAngleZ = -0.1F;
      this.tailModel[8].setRotationPoint(49.0F, -20.5F, 13.0F);
      this.tailModel[9].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(12.0, 0.0, 12, 0), new Coord2D(6.0, 5.0, 6, 5)}), 1.0F, 12, 5, 28, 1, 0, new float[]{12.0F, 8.0F, 8.0F});
      this.tailModel[9].setRotationPoint(65.0F, -22.0F, -14.0F);
      this.tailModel[9].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[9].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[10].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(12.0, 0.0, 12, 0), new Coord2D(6.0, 5.0, 6, 5)}), 1.0F, 12, 5, 28, 1, 0, new float[]{12.0F, 8.0F, 8.0F});
      this.tailModel[10].setRotationPoint(65.0F, -22.0F, 2.0F);
      this.tailModel[10].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[10].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[11].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(12.0, 0.0, 12, 0), new Coord2D(6.0, 5.0, 6, 5)}), 1.0F, 12, 5, 28, 1, 0, new float[]{12.0F, 8.0F, 8.0F});
      this.tailModel[11].setRotationPoint(65.0F, -11.0F, -14.0F);
      this.tailModel[11].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[11].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[12].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(12.0, 0.0, 12, 0), new Coord2D(6.0, 5.0, 6, 5)}), 1.0F, 12, 5, 28, 1, 0, new float[]{12.0F, 8.0F, 8.0F});
      this.tailModel[12].setRotationPoint(65.0F, -11.0F, 2.0F);
      this.tailModel[12].rotateAngleY = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[12].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[13].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 0.0, 30, 0), new Coord2D(46.0, 20.0, 46, 20), new Coord2D(6.0, 20.0, 6, 20), new Coord2D(0.0, 14.0, 0, 14)}), 1.0F, 46, 20, 118, 1, 0, new float[]{30.0F, 26.0F, 40.0F, 8.0F, 14.0F});
      this.tailModel[13].rotateAngleY = (float) Math.PI;
      this.tailModel[13].rotateAngleX = (float) (Math.PI / 2);
      this.tailModel[13].setRotationPoint(50.0F, -17.0F, 14.0F);
      this.tailModel[14].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(26.0, 0.0, 26, 0), new Coord2D(26.0, 20.0, 26, 20), new Coord2D(18.0, 20.0, 18, 20)}), 1.0F, 26, 20, 81, 1, 0, new float[]{26.0F, 20.0F, 8.0F, 27.0F});
      this.tailModel[14].rotateAngleY = (float) Math.PI;
      this.tailModel[14].rotateAngleX = (float) (Math.PI / 2);
      this.tailModel[14].setRotationPoint(56.0F, -17.0F, 34.0F);
      this.tailModel[15].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(30.0, 0.0, 30, 0), new Coord2D(46.0, 20.0, 46, 20), new Coord2D(6.0, 20.0, 6, 20), new Coord2D(0.0, 14.0, 0, 14)}), 1.0F, 46, 20, 118, 1, 0, new float[]{30.0F, 26.0F, 40.0F, 8.0F, 14.0F});
      this.tailModel[15].rotateAngleY = (float) Math.PI;
      this.tailModel[15].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[15].setRotationPoint(50.0F, -16.0F, -14.0F);
      this.tailModel[16].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(26.0, 0.0, 26, 0), new Coord2D(26.0, 20.0, 26, 20), new Coord2D(18.0, 20.0, 18, 20)}), 1.0F, 26, 20, 81, 1, 0, new float[]{26.0F, 20.0F, 8.0F, 27.0F});
      this.tailModel[16].rotateAngleY = (float) Math.PI;
      this.tailModel[16].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.tailModel[16].setRotationPoint(56.0F, -16.0F, -34.0F);
      this.tailModel[17].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(49.0, 0.0, 49, 0), new Coord2D(31.0, 36.0, 31, 36), new Coord2D(16.0, 36.0, 16, 36)}), 1.0F, 49, 36, 144, 1, 0, new float[]{49.0F, 40.0F, 15.0F, 39.0F});
      this.tailModel[17].rotateAngleY = (float) Math.PI;
      this.tailModel[17].rotateAngleX = -0.4F;
      this.tailModel[17].setRotationPoint(25.0F, -17.0F, -16.0F);
      this.tailModel[18].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(49.0, 0.0, 49, 0), new Coord2D(31.0, 36.0, 31, 36), new Coord2D(16.0, 36.0, 16, 36)}), 1.0F, 49, 36, 144, 1, 0, new float[]{49.0F, 40.0F, 15.0F, 39.0F});
      this.tailModel[18].rotateAngleY = (float) Math.PI;
      this.tailModel[18].rotateAngleX = 0.4F;
      this.tailModel[18].setRotationPoint(25.0F, -17.0F, 15.0F);
      this.rightWingModel = new ModelRendererTurbo[4];
      this.rightWingModel[0] = new ModelRendererTurbo(this, 0, 185, textureX, textureY);
      this.rightWingModel[1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.rightWingModel[2] = new ModelRendererTurbo(this, 0, 246, textureX, textureY);
      this.rightWingModel[3] = new ModelRendererTurbo(this, 0, 265, textureX, textureY);
      this.rightWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(62.0, 0.0, 62, 0), new Coord2D(62.0, 56.0, 62, 56), new Coord2D(47.0, 56.0, 47, 56)}), 1.0F, 62, 56, 206, 1, 0, new float[]{62.0F, 56.0F, 15.0F, 73.0F});
      this.rightWingModel[0].rotateAngleY = (float) Math.PI;
      this.rightWingModel[0].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.rightWingModel[0].setRotationPoint(-31.0F, -16.0F, -29.0F);
      this.rightWingModel[1].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(10.0, 0.0, 10, 0), new Coord2D(0.0, 12.0, 0, 12)}), 1.0F, 10, 12, 38, 1, 0, new float[]{10.0F, 16.0F, 12.0F});
      this.rightWingModel[1].rotateAngleY = (float) Math.PI;
      this.rightWingModel[1].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.rightWingModel[1].setRotationPoint(31.0F, -16.0F, -73.0F);
      this.rightWingModel[2].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(25.0, 0.0, 25, 0), new Coord2D(20.0, 15.0, 20, 15), new Coord2D(0.0, 15.0, 0, 15)}), 1.0F, 25, 15, 76, 1, 0, new float[]{25.0F, 16.0F, 20.0F, 15.0F});
      this.rightWingModel[2].rotateAngleY = (float) Math.PI;
      this.rightWingModel[2].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.rightWingModel[2].setRotationPoint(31.0F, -16.0F, -29.0F);
      this.rightWingModel[3].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(7.0, 0.0, 7, 0), new Coord2D(7.0, 29.0, 7, 29), new Coord2D(0.0, 29.0, 0, 29)}), 1.0F, 7, 29, 72, 1, 0, new float[]{7.0F, 29.0F, 7.0F, 29.0F});
      this.rightWingModel[3].rotateAngleY = (float) Math.PI;
      this.rightWingModel[3].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.rightWingModel[3].setRotationPoint(31.0F, -16.0F, -44.0F);
      this.leftWingModel = new ModelRendererTurbo[4];
      this.leftWingModel[0] = new ModelRendererTurbo(this, 0, 185, textureX, textureY);
      this.leftWingModel[1] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
      this.leftWingModel[2] = new ModelRendererTurbo(this, 0, 246, textureX, textureY);
      this.leftWingModel[3] = new ModelRendererTurbo(this, 0, 265, textureX, textureY);
      this.leftWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(62.0, 0.0, 62, 0), new Coord2D(62.0, 56.0, 62, 56), new Coord2D(47.0, 56.0, 47, 56)}), 1.0F, 62, 56, 206, 1, 0, new float[]{62.0F, 56.0F, 15.0F, 73.0F});
      this.leftWingModel[0].rotateAngleY = (float) Math.PI;
      this.leftWingModel[0].rotateAngleX = (float) (Math.PI / 2);
      this.leftWingModel[0].setRotationPoint(-31.0F, -17.0F, 29.0F);
      this.leftWingModel[1].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(10.0, 0.0, 10, 0), new Coord2D(0.0, 12.0, 0, 12)}), 1.0F, 10, 12, 38, 1, 0, new float[]{10.0F, 16.0F, 12.0F});
      this.leftWingModel[1].rotateAngleY = (float) Math.PI;
      this.leftWingModel[1].rotateAngleX = (float) (Math.PI / 2);
      this.leftWingModel[1].setRotationPoint(31.0F, -17.0F, 73.0F);
      this.leftWingModel[2].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(25.0, 0.0, 25, 0), new Coord2D(20.0, 15.0, 20, 15), new Coord2D(0.0, 15.0, 0, 15)}), 1.0F, 25, 15, 76, 1, 0, new float[]{25.0F, 16.0F, 20.0F, 15.0F});
      this.leftWingModel[2].rotateAngleY = (float) Math.PI;
      this.leftWingModel[2].rotateAngleX = (float) (Math.PI / 2);
      this.leftWingModel[2].setRotationPoint(31.0F, -17.0F, 29.0F);
      this.leftWingModel[3].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(7.0, 0.0, 7, 0), new Coord2D(7.0, 29.0, 7, 29), new Coord2D(0.0, 29.0, 0, 29)}), 1.0F, 7, 29, 72, 1, 0, new float[]{7.0F, 29.0F, 7.0F, 29.0F});
      this.leftWingModel[3].rotateAngleY = (float) Math.PI;
      this.leftWingModel[3].rotateAngleX = (float) (Math.PI / 2);
      this.leftWingModel[3].setRotationPoint(31.0F, -17.0F, 44.0F);
      this.propellerModels = new ModelRendererTurbo[0][0];
      this.yawFlapModel = new ModelRendererTurbo[0];
      this.pitchFlapLeftModel = new ModelRendererTurbo[1];
      this.pitchFlapLeftModel[0] = new ModelRendererTurbo(this, 227, 197, textureX, textureY);
      this.pitchFlapLeftModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(14.0, 0.0, 14, 0), new Coord2D(7.0, 20.0, 7, 20), new Coord2D(0.0, 20.0, 0, 20)}), 1.0F, 14, 20, 62, 1, 0, new float[]{14.0F, 21.0F, 7.0F, 20.0F});
      this.pitchFlapLeftModel[0].rotateAngleY = (float) Math.PI;
      this.pitchFlapLeftModel[0].rotateAngleX = (float) (Math.PI / 2);
      this.pitchFlapLeftModel[0].setRotationPoint(82.0F, -17.0F, 34.0F);
      this.pitchFlapRightModel = new ModelRendererTurbo[1];
      this.pitchFlapRightModel[0] = new ModelRendererTurbo(this, 227, 197, textureX, textureY);
      this.pitchFlapRightModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(14.0, 0.0, 14, 0), new Coord2D(7.0, 20.0, 7, 20), new Coord2D(0.0, 20.0, 0, 20)}), 1.0F, 14, 20, 62, 1, 0, new float[]{14.0F, 21.0F, 7.0F, 20.0F});
      this.pitchFlapRightModel[0].rotateAngleY = (float) Math.PI;
      this.pitchFlapRightModel[0].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.pitchFlapRightModel[0].setRotationPoint(82.0F, -16.0F, -34.0F);
      this.topWingModel = new ModelRendererTurbo[0];
      this.pitchFlapLeftWingModel = new ModelRendererTurbo[1];
      this.pitchFlapLeftWingModel[0] = new ModelRendererTurbo(this, 0, 297, textureX, textureY);
      this.pitchFlapLeftWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(13.0, 0.0, 13, 0), new Coord2D(3.0, 29.0, 3, 29), new Coord2D(0.0, 29.0, 0, 29)}), 1.0F, 13, 29, 76, 1, 0, new float[]{13.0F, 31.0F, 3.0F, 29.0F});
      this.pitchFlapLeftWingModel[0].rotateAngleY = (float) Math.PI;
      this.pitchFlapLeftWingModel[0].rotateAngleX = (float) (Math.PI / 2);
      this.pitchFlapLeftWingModel[0].setRotationPoint(38.0F, -17.0F, 44.0F);
      this.pitchFlapRightWingModel = new ModelRendererTurbo[1];
      this.pitchFlapRightWingModel[0] = new ModelRendererTurbo(this, 0, 297, textureX, textureY);
      this.pitchFlapRightWingModel[0].addShape3D(0.0F, 0.0F, 0.0F, new Shape2D(new Coord2D[]{new Coord2D(0.0, 0.0, 0, 0), new Coord2D(13.0, 0.0, 13, 0), new Coord2D(3.0, 29.0, 3, 29), new Coord2D(0.0, 29.0, 0, 29)}), 1.0F, 13, 29, 76, 1, 0, new float[]{13.0F, 31.0F, 3.0F, 29.0F});
      this.pitchFlapRightWingModel[0].rotateAngleY = (float) Math.PI;
      this.pitchFlapRightWingModel[0].rotateAngleX = (float) (Math.PI * 3.0 / 2.0);
      this.pitchFlapRightWingModel[0].setRotationPoint(38.0F, -16.0F, -44.0F);
      this.flipAll();
      // Manus Legacy alignment: centres the model where the definition expects it.
      this.translateAll(26.72F, 0F, 0F);
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
