package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWW1MaghrebTunic extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelWW1MaghrebTunic() {
      this.bodyModel = new ModelRendererTurbo[42];
      this.leftArmModel = new ModelRendererTurbo[1];
      this.rightArmModel = new ModelRendererTurbo[1];
      this.leftLegModel = new ModelRendererTurbo[2];
      this.rightLegModel = new ModelRendererTurbo[2];
      this.skirtFrontModel = new ModelRendererTurbo[11];
      this.skirtRearModel = new ModelRendererTurbo[5];
      this.initbodyModel_1();
      this.initleftArmModel_1();
      this.initrightArmModel_1();
      this.initleftLegModel_1();
      this.initrightLegModel_1();
      this.initskirtFrontModel_1();
      this.initskirtRearModel_1();
   }

   private void initbodyModel_1() {
      this.bodyModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 177, 1, this.textureX, this.textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 337, 1, this.textureX, this.textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 305, 1, this.textureX, this.textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 361, 1, this.textureX, this.textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 369, 1, this.textureX, this.textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 373, 360, this.textureX, this.textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 232, 399, this.textureX, this.textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 61, 187, this.textureX, this.textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 125, 224, this.textureX, this.textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 412, 246, this.textureX, this.textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 403, 220, this.textureX, this.textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 161, 17, this.textureX, this.textureY);
      this.bodyModel[20] = new ModelRendererTurbo(this, 424, 235, this.textureX, this.textureY);
      this.bodyModel[21] = new ModelRendererTurbo(this, 405, 258, this.textureX, this.textureY);
      this.bodyModel[22] = new ModelRendererTurbo(this, 349, 346, this.textureX, this.textureY);
      this.bodyModel[23] = new ModelRendererTurbo(this, 259, 428, this.textureX, this.textureY);
      this.bodyModel[24] = new ModelRendererTurbo(this, 195, 201, this.textureX, this.textureY);
      this.bodyModel[25] = new ModelRendererTurbo(this, 251, 257, this.textureX, this.textureY);
      this.bodyModel[26] = new ModelRendererTurbo(this, 257, 389, this.textureX, this.textureY);
      this.bodyModel[27] = new ModelRendererTurbo(this, 179, 400, this.textureX, this.textureY);
      this.bodyModel[28] = new ModelRendererTurbo(this, 198, 181, this.textureX, this.textureY);
      this.bodyModel[29] = new ModelRendererTurbo(this, 272, 196, this.textureX, this.textureY);
      this.bodyModel[30] = new ModelRendererTurbo(this, 178, 242, this.textureX, this.textureY);
      this.bodyModel[31] = new ModelRendererTurbo(this, 131, 202, this.textureX, this.textureY);
      this.bodyModel[32] = new ModelRendererTurbo(this, 391, 242, this.textureX, this.textureY);
      this.bodyModel[33] = new ModelRendererTurbo(this, 430, 209, this.textureX, this.textureY);
      this.bodyModel[34] = new ModelRendererTurbo(this, 373, 252, this.textureX, this.textureY);
      this.bodyModel[35] = new ModelRendererTurbo(this, 422, 195, this.textureX, this.textureY);
      this.bodyModel[36] = new ModelRendererTurbo(this, 232, 399, this.textureX, this.textureY);
      this.bodyModel[37] = new ModelRendererTurbo(this, 232, 399, this.textureX, this.textureY);
      this.bodyModel[38] = new ModelRendererTurbo(this, 61, 187, this.textureX, this.textureY);
      this.bodyModel[39] = new ModelRendererTurbo(this, 61, 187, this.textureX, this.textureY);
      this.bodyModel[40] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.bodyModel[41] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(0.0F, 0.0F, -2.0F, 3, 4, 1, 0.0F, -0.2F, -2.4F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, -0.2F, -2.4F, 0.0F, -0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(-3.0F, 0.0F, -2.0F, 3, 4, 1, 0.0F, 0.0F, 0.0F, 0.2F, -0.2F, -2.4F, 0.2F, -0.2F, -2.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addBox(-3.0F, 0.0F, -2.0F, 6, 10, 4, 0.0F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(0.0F, 4.0F, -3.0F, 3, 7, 1, 0.0F, -0.2F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.3F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, -0.2F, 0.3F, 0.0F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(-3.0F, 4.0F, -3.0F, 3, 7, 1, 0.0F, 0.0F, 0.0F, -0.8F, -0.2F, 0.0F, -0.8F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, -0.2F, 0.3F, -0.8F, -0.2F, 0.3F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(3.0F, 0.0F, -2.0F, 1, 11, 4, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(0.0F, 3.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(0.0F, 6.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[8].addShapeBox(0.0F, 8.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.bodyModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[9].addShapeBox(-4.0F, 0.0F, 2.0F, 8, 6, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, -1.7F, 0.0F, 0.0F, -1.7F, 0.0F, 0.0F, -1.7F, 0.0F, -0.5F, -1.7F, 0.0F, -0.5F);
      this.bodyModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[10].addBox(-2.0F, 5.0F, 2.0F, 4, 1, 1, 0.0F);
      this.bodyModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[11].addShapeBox(-4.0F, 0.0F, 2.0F, 1, 6, 1, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.7F, 0.0F, -0.5F, 1.8F, 0.0F, -0.5F, 1.8F, 0.0F, 0.0F, -1.7F, 0.0F, 0.0F);
      this.bodyModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[12].addShapeBox(3.0F, 0.0F, 2.0F, 1, 6, 1, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.8F, 0.0F, -0.5F, -1.7F, 0.0F, -0.5F, -1.7F, 0.0F, 0.0F, 1.8F, 0.0F, 0.0F);
      this.bodyModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[13].addShapeBox(-4.5F, 8.3F, -2.5F, 16, 8, 5, 0.0F, 0.1F, 0.0F, 0.05F, -6.9F, 0.0F, 0.05F, -6.9F, 0.0F, 0.05F, 0.1F, 0.0F, 0.05F, 0.1F, -6.7F, 0.05F, -6.9F, -6.7F, 0.05F, -6.9F, -6.7F, 0.05F, 0.1F, -6.7F, 0.05F);
      this.bodyModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[14].addShapeBox(-5.5F, 8.5F, -3.5F, 6, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[15].addShapeBox(-5.5F, 8.45F, -3.7F, 6, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[16].addShapeBox(-3.0F, 9.5F, -3.7F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[17].addShapeBox(-3.3F, 1.3F, -2.4F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[18].addShapeBox(-3.3F, 0.3F, -2.4F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[19].addShapeBox(2.3F, 0.3F, -2.4F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[19].setRotationPoint(0.0F, 0.0F, 3.7F);
      this.bodyModel[20].addShapeBox(2.3F, 1.3F, -2.4F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[20].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[21].addShapeBox(-3.3F, 0.0F, -2.2F, 8, 7, 6, 0.0F, 0.0F, 0.3F, 0.3F, -7.0F, 0.3F, 0.3F, -7.0F, 0.3F, -1.4F, 0.0F, 0.3F, -1.4F, 0.0F, -6.7F, 0.3F, -7.0F, -6.7F, 0.3F, -7.0F, -6.7F, -1.4F, 0.0F, -6.7F, -1.4F);
      this.bodyModel[21].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[22].addShapeBox(2.3F, 0.0F, -2.2F, 8, 7, 6, 0.0F, 0.0F, 0.3F, 0.3F, -7.0F, 0.3F, 0.3F, -7.0F, 0.3F, -1.4F, 0.0F, 0.3F, -1.4F, 0.0F, -6.7F, 0.3F, -7.0F, -6.7F, 0.3F, -7.0F, -6.7F, -1.4F, 0.0F, -6.7F, -1.4F);
      this.bodyModel[22].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[23].addShapeBox(-0.5F, 8.5F, -3.5F, 6, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[23].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[24].addShapeBox(-0.5F, 8.45F, -3.7F, 6, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[24].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[25].addShapeBox(2.0F, 9.5F, -3.7F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[25].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[26].addShapeBox(-5.5F, 8.5F, -3.5F, 6, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[26].setRotationPoint(0.0F, 0.0F, 6.0F);
      this.bodyModel[27].addShapeBox(-0.5F, 8.5F, -3.5F, 6, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[27].setRotationPoint(0.0F, 0.0F, 6.0F);
      this.bodyModel[28].addShapeBox(-0.5F, 8.45F, -3.7F, 6, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[28].setRotationPoint(0.0F, 0.0F, 6.3F);
      this.bodyModel[29].addShapeBox(-5.5F, 8.45F, -3.7F, 6, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[29].setRotationPoint(0.0F, 0.0F, 6.3F);
      this.bodyModel[30].addShapeBox(2.0F, 9.5F, -3.7F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[30].setRotationPoint(0.0F, 0.0F, 6.3F);
      this.bodyModel[31].addShapeBox(-3.0F, 9.5F, -3.7F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[31].setRotationPoint(0.0F, 0.0F, 6.3F);
      this.bodyModel[32].addShapeBox(-3.3F, 1.3F, -2.4F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[32].setRotationPoint(0.0F, 0.0F, 3.7F);
      this.bodyModel[33].addShapeBox(-3.3F, 0.3F, -2.4F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[33].setRotationPoint(0.0F, 0.0F, 3.7F);
      this.bodyModel[34].addShapeBox(2.3F, 1.3F, -2.4F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[34].setRotationPoint(0.0F, 0.0F, 3.7F);
      this.bodyModel[35].addShapeBox(2.3F, 0.3F, -2.4F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[35].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[36].addShapeBox(-5.5F, 8.5F, -3.5F, 4, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[36].setRotationPoint(0.7F, -3.4F, 0.5F);
      this.bodyModel[37].addShapeBox(-5.5F, 8.5F, -3.5F, 4, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[37].setRotationPoint(6.4F, -3.4F, 0.5F);
      this.bodyModel[38].addShapeBox(-5.5F, 8.45F, -3.7F, 4, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[38].setRotationPoint(0.7F, -3.4F, 0.6F);
      this.bodyModel[39].addShapeBox(-5.5F, 8.45F, -3.7F, 4, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[39].setRotationPoint(6.5F, -3.4F, 0.6F);
      this.bodyModel[40].addShapeBox(-4.0F, 0.0F, -2.0F, 1, 11, 4, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.bodyModel[40].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[41].addShapeBox(-3.0F, 3.0F, 2.0F, 6, 8, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F);
      this.bodyModel[41].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftArmModel_1() {
      this.leftArmModel[0] = new ModelRendererTurbo(this, 137, 1, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, -2.0F, -2.0F, 4, 11, 4, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightArmModel_1() {
      this.rightArmModel[0] = new ModelRendererTurbo(this, 161, 1, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.0F, -2.0F, 4, 11, 4, 0.0F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftLegModel_1() {
      this.leftLegModel[0] = new ModelRendererTurbo(this, 209, 1, this.textureX, this.textureY);
      this.leftLegModel[1] = new ModelRendererTurbo(this, 313, 1, this.textureX, this.textureY);
      this.leftLegModel[0].addBox(-2.0F, -1.0F, -2.0F, 4, 13, 4, 0.0F);
      this.leftLegModel[0].setRotationPoint(0.0F, -1.0F, 0.0F);
      this.leftLegModel[1].addBox(-2.0F, 11.0F, -2.0F, 4, 1, 4, 0.0F);
      this.leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightLegModel_1() {
      this.rightLegModel[0] = new ModelRendererTurbo(this, 289, 1, this.textureX, this.textureY);
      this.rightLegModel[1] = new ModelRendererTurbo(this, 209, 1, this.textureX, this.textureY);
      this.rightLegModel[0].addBox(-2.0F, 11.0F, -2.0F, 4, 1, 4, 0.0F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[1].addBox(-2.0F, -1.0F, -2.0F, 4, 13, 4, 0.0F);
      this.rightLegModel[1].setRotationPoint(0.0F, -1.0F, 0.0F);
   }

   private void initskirtFrontModel_1() {
      this.skirtFrontModel[0] = new ModelRendererTurbo(this, 201, 1, this.textureX, this.textureY);
      this.skirtFrontModel[1] = new ModelRendererTurbo(this, 241, 1, this.textureX, this.textureY);
      this.skirtFrontModel[2] = new ModelRendererTurbo(this, 225, 1, this.textureX, this.textureY);
      this.skirtFrontModel[3] = new ModelRendererTurbo(this, 249, 1, this.textureX, this.textureY);
      this.skirtFrontModel[4] = new ModelRendererTurbo(this, 257, 1, this.textureX, this.textureY);
      this.skirtFrontModel[5] = new ModelRendererTurbo(this, 265, 1, this.textureX, this.textureY);
      this.skirtFrontModel[6] = new ModelRendererTurbo(this, 273, 1, this.textureX, this.textureY);
      this.skirtFrontModel[7] = new ModelRendererTurbo(this, 107, 3, this.textureX, this.textureY);
      this.skirtFrontModel[8] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.skirtFrontModel[9] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.skirtFrontModel[10] = new ModelRendererTurbo(this, 107, 3, this.textureX, this.textureY);
      this.skirtFrontModel[0].addShapeBox(0.0F, -1.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.skirtFrontModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[1].addShapeBox(-3.9F, 7.0F, -2.0F, 1, 3, 2, 0.0F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.7F, 0.2F, 0.0F, -0.7F);
      this.skirtFrontModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[2].addShapeBox(0.0F, 1.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.skirtFrontModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[3].addShapeBox(0.0F, 4.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.skirtFrontModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[4].addShapeBox(0.0F, 6.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.skirtFrontModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[5].addShapeBox(0.0F, 9.0F, -3.0F, 1, 1, 1, 0.0F, 0.2F, -0.2F, -0.8F, -0.8F, -0.2F, -0.8F, -0.8F, -0.2F, 0.0F, 0.2F, -0.2F, 0.0F, 0.2F, 0.2F, -0.8F, -0.8F, 0.2F, -0.8F, -0.8F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F);
      this.skirtFrontModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[6].addShapeBox(3.0F, 7.0F, -2.0F, 1, 3, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, -0.7F, 0.0F, 0.0F, -0.7F);
      this.skirtFrontModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[7].addShapeBox(3.0F, -1.0F, -2.0F, 1, 8, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.skirtFrontModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[8].addShapeBox(0.0F, -1.0F, -3.0F, 3, 11, 1, 0.0F, -0.2F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.3F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, -0.2F, 0.3F, 0.0F);
      this.skirtFrontModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[9].addShapeBox(-3.0F, -1.0F, -3.0F, 3, 11, 1, 0.0F, 0.0F, 0.0F, -0.8F, -0.2F, 0.0F, -0.8F, -0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, -0.2F, 0.3F, -0.8F, -0.2F, 0.3F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtFrontModel[10].addShapeBox(-4.1F, -1.0F, -2.0F, 1, 8, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.skirtFrontModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initskirtRearModel_1() {
      this.skirtRearModel[0] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.skirtRearModel[1] = new ModelRendererTurbo(this, 233, 1, this.textureX, this.textureY);
      this.skirtRearModel[2] = new ModelRendererTurbo(this, 281, 1, this.textureX, this.textureY);
      this.skirtRearModel[3] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.skirtRearModel[4] = new ModelRendererTurbo(this, 106, 4, this.textureX, this.textureY);
      this.skirtRearModel[0].addShapeBox(-3.0F, -3.0F, 2.0F, 6, 13, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.8F, 0.0F, 0.0F, -0.8F);
      this.skirtRearModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtRearModel[1].addShapeBox(-3.9F, 7.0F, 0.0F, 1, 3, 2, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, -0.7F, 0.0F, 0.0F, -0.7F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F);
      this.skirtRearModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtRearModel[2].addShapeBox(3.0F, 7.0F, 0.0F, 1, 3, 2, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.7F, 0.2F, 0.0F, -0.7F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.skirtRearModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtRearModel[3].addShapeBox(3.0F, -1.0F, 0.0F, 1, 8, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.skirtRearModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.skirtRearModel[4].addShapeBox(-4.1F, -1.0F, 0.0F, 1, 8, 2, 0.0F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.skirtRearModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
