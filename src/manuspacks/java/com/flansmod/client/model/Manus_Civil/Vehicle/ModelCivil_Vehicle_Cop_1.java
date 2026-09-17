
package com.flansmod.client.model.Manus_Civil.Vehicle;

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelCivil_Vehicle_Cop_1 extends ModelVehicle {
   int textureX = 512;
   int textureY = 512;

   public ModelCivil_Vehicle_Cop_1() {
      bodyModel = new ModelRendererTurbo[20];
      bodyModel[0] = new ModelRendererTurbo(this, 0, 225, textureX, textureY);
      bodyModel[1] = new ModelRendererTurbo(this, 0, 466, textureX, textureY);
      bodyModel[2] = new ModelRendererTurbo(this, 31, 372, textureX, textureY);
      bodyModel[3] = new ModelRendererTurbo(this, 0, 372, textureX, textureY);
      bodyModel[4] = new ModelRendererTurbo(this, 82, 270, textureX, textureY);
      bodyModel[5] = new ModelRendererTurbo(this, 0, 269, textureX, textureY);
      bodyModel[6] = new ModelRendererTurbo(this, 0, 450, textureX, textureY);
      bodyModel[7] = new ModelRendererTurbo(this, 0, 434, textureX, textureY);
      bodyModel[8] = new ModelRendererTurbo(this, 86, 394, textureX, textureY);
      bodyModel[9] = new ModelRendererTurbo(this, 0, 386, textureX, textureY);
      bodyModel[10] = new ModelRendererTurbo(this, 0, 359, textureX, textureY);
      bodyModel[11] = new ModelRendererTurbo(this, 27, 359, textureX, textureY);
      bodyModel[12] = new ModelRendererTurbo(this, 0, 311, textureX, textureY);
      bodyModel[13] = new ModelRendererTurbo(this, 86, 347, textureX, textureY);
      bodyModel[14] = new ModelRendererTurbo(this, 86, 311, textureX, textureY);
      bodyModel[15] = new ModelRendererTurbo(this, 155, 388, textureX, textureY);
      bodyModel[16] = new ModelRendererTurbo(this, 155, 348, textureX, textureY);
      bodyModel[17] = new ModelRendererTurbo(this, 82, 225, textureX, textureY);
      bodyModel[18] = new ModelRendererTurbo(this, 90, 225, textureX, textureY);
      bodyModel[19] = new ModelRendererTurbo(this, 0, 188, textureX, textureY);
      bodyModel[0].addBox(8.0F, -8.0F, -14.0F, 12, 14, 28, 0.0F);
      bodyModel[1].addBox(-24.0F, 4.0F, -16.0F, 32, 2, 32, 0.0F);
      bodyModel[2].addBox(8.0F, -8.0F, -16.0F, 12, 9, 2, 0.0F);
      bodyModel[3].addBox(8.0F, -8.0F, 14.0F, 12, 9, 2, 0.0F);
      bodyModel[4].addBox(-6.0F, -7.0F, -14.0F, 1, 11, 28, 0.0F);
      bodyModel[5].addBox(-34.0F, -8.0F, -14.0F, 12, 12, 28, 0.0F);
      bodyModel[6].addBox(-24.0F, -8.0F, -16.0F, 32, 12, 2, 0.0F);
      bodyModel[7].addBox(-24.0F, -8.0F, 14.0F, 32, 12, 2, 0.0F);
      bodyModel[8].addBox(0.0F, 0.0F, 0.0F, 1, 13, 32, 0.0F);
      bodyModel[8].setRotationPoint(-20.0F, -19.0F, -16.0F);
      bodyModel[8].rotateAngleZ = (float) (-Math.PI / 6);
      bodyModel[9].addBox(20.0F, -8.0F, -16.0F, 10, 14, 32, 0.0F);
      bodyModel[10].addBox(-34.0F, -8.0F, 14.0F, 10, 9, 2, 0.0F);
      bodyModel[11].addBox(-34.0F, -8.0F, -16.0F, 10, 9, 2, 0.0F);
      bodyModel[12].addBox(-44.0F, -8.0F, -16.0F, 10, 14, 32, 0.0F);
      bodyModel[13].addBox(0.0F, 0.0F, 0.0F, 1, 12, 32, 0.0F);
      bodyModel[13].setRotationPoint(5.0F, -18.0F, -16.0F);
      bodyModel[13].rotateAngleZ = 0.4363323F;
      bodyModel[14].addBox(-21.0F, -19.0F, -16.0F, 27, 1, 32, 0.0F);
      bodyModel[15].addBox(-45.0F, 2.0F, -17.0F, 6, 4, 34, 0.0F);
      bodyModel[16].addBox(25.0F, 2.0F, -17.0F, 6, 4, 34, 0.0F);
      bodyModel[17].addBox(-7.0F, -18.0F, -16.0F, 2, 10, 1, 0.0F);
      bodyModel[18].addBox(-7.0F, -18.0F, 15.0F, 2, 10, 1, 0.0F);
      bodyModel[19].addBox(-8.0F, -22.0F, -14.0F, 3, 3, 28, 0.0F);
      leftBackWheelModel = new ModelRendererTurbo[1];
      leftBackWheelModel[0] = new ModelRendererTurbo(this, 0, 502, textureX, textureY);
      leftBackWheelModel[0].addBox(-4.0F, -4.0F, -1.0F, 8, 8, 2, 0.0F);
      leftBackWheelModel[0].setRotationPoint(-29.0F, 6.0F, -15.0F);
      rightBackWheelModel = new ModelRendererTurbo[1];
      rightBackWheelModel[0] = new ModelRendererTurbo(this, 0, 502, textureX, textureY);
      rightBackWheelModel[0].addBox(-4.0F, -4.0F, -1.0F, 8, 8, 2, 0.0F);
      rightBackWheelModel[0].setRotationPoint(-29.0F, 6.0F, 15.0F);
      leftFrontWheelModel = new ModelRendererTurbo[1];
      leftFrontWheelModel[0] = new ModelRendererTurbo(this, 0, 502, textureX, textureY);
      leftFrontWheelModel[0].addBox(-4.0F, -4.0F, -1.0F, 8, 8, 2, 0.0F);
      leftFrontWheelModel[0].setRotationPoint(14.0F, 6.0F, -15.0F);
      rightFrontWheelModel = new ModelRendererTurbo[1];
      rightFrontWheelModel[0] = new ModelRendererTurbo(this, 0, 502, textureX, textureY);
      rightFrontWheelModel[0].addBox(-4.0F, -4.0F, -1.0F, 8, 8, 2, 0.0F);
      rightFrontWheelModel[0].setRotationPoint(14.0F, 6.0F, 15.0F);
      steeringWheelModel = new ModelRendererTurbo[1];
      steeringWheelModel[0] = new ModelRendererTurbo(this, 0, 150, textureX, textureY);
      steeringWheelModel[0].addBox(0.0F, -4.0F, -4.0F, 1, 8, 8, 0.0F);
      steeringWheelModel[0].setRotationPoint(7.0F, -7.0F, 8.0F);
      steeringWheelModel[0].rotateAngleZ = -0.1919862F;
      flipAll();
      translateAll(8, 0, 0);
   }
}
