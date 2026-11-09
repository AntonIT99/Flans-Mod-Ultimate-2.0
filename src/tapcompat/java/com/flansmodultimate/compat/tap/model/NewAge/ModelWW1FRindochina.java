package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelWW1FRindochina extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 64;

   public ModelWW1FRindochina() {
      this.bodyModel = new ModelRendererTurbo[39];
      this.leftArmModel = new ModelRendererTurbo[2];
      this.rightArmModel = new ModelRendererTurbo[2];
      this.leftLegModel = new ModelRendererTurbo[8];
      this.rightLegModel = new ModelRendererTurbo[8];
      this.skirtFrontModel = new ModelRendererTurbo[1];
      this.initbodyModel_1();
      this.initleftArmModel_1();
      this.initrightArmModel_1();
      this.initleftLegModel_1();
      this.initrightLegModel_1();
      this.initskirtFrontModel_1();
   }

   private void initbodyModel_1() {
      this.bodyModel[0] = new ModelRendererTurbo(this, 185, 1, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 209, 1, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 129, 1, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 329, 1, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 385, 1, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 401, 1, this.textureX, this.textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 417, 1, this.textureX, this.textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 449, 1, this.textureX, this.textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 465, 1, this.textureX, this.textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 481, 1, this.textureX, this.textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 497, 1, this.textureX, this.textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 1, 9, this.textureX, this.textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 17, 9, this.textureX, this.textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 177, 1, this.textureX, this.textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 25, 9, this.textureX, this.textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 41, 9, this.textureX, this.textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 65, 9, this.textureX, this.textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 137, 9, this.textureX, this.textureY);
      this.bodyModel[20] = new ModelRendererTurbo(this, 161, 9, this.textureX, this.textureY);
      this.bodyModel[21] = new ModelRendererTurbo(this, 185, 9, this.textureX, this.textureY);
      this.bodyModel[22] = new ModelRendererTurbo(this, 209, 9, this.textureX, this.textureY);
      this.bodyModel[23] = new ModelRendererTurbo(this, 233, 9, this.textureX, this.textureY);
      this.bodyModel[24] = new ModelRendererTurbo(this, 281, 9, this.textureX, this.textureY);
      this.bodyModel[25] = new ModelRendererTurbo(this, 385, 9, this.textureX, this.textureY);
      this.bodyModel[26] = new ModelRendererTurbo(this, 105, 9, this.textureX, this.textureY);
      this.bodyModel[27] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.bodyModel[28] = new ModelRendererTurbo(this, 409, 9, this.textureX, this.textureY);
      this.bodyModel[29] = new ModelRendererTurbo(this, 425, 9, this.textureX, this.textureY);
      this.bodyModel[30] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.bodyModel[31] = new ModelRendererTurbo(this, 201, 1, this.textureX, this.textureY);
      this.bodyModel[32] = new ModelRendererTurbo(this, 225, 1, this.textureX, this.textureY);
      this.bodyModel[33] = new ModelRendererTurbo(this, 249, 1, this.textureX, this.textureY);
      this.bodyModel[34] = new ModelRendererTurbo(this, 273, 1, this.textureX, this.textureY);
      this.bodyModel[35] = new ModelRendererTurbo(this, 297, 1, this.textureX, this.textureY);
      this.bodyModel[36] = new ModelRendererTurbo(this, 321, 1, this.textureX, this.textureY);
      this.bodyModel[37] = new ModelRendererTurbo(this, 353, 1, this.textureX, this.textureY);
      this.bodyModel[38] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(-4.0F, 8.5F, -2.0F, 4, 2, 4, 0.0F, 0.4F, 0.0F, 0.7F, 0.0F, 0.0F, 1.3F, 0.0F, 0.0F, 1.2F, 0.4F, 0.0F, 0.9F, 0.4F, 0.0F, 0.7F, 0.0F, 0.0F, 1.3F, 0.0F, 0.0F, 1.2F, 0.4F, 0.0F, 0.9F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(0.0F, 8.5F, -2.0F, 4, 2, 4, 0.0F, 0.0F, 0.0F, 1.3F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.9F, 0.0F, 0.0F, 1.2F, 0.0F, 0.0F, 1.3F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.9F, 0.0F, 0.0F, 1.2F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-1.5F, 2.0F, -2.96F, 3, 1, 1, 0.0F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(-1.5F, 4.0F, -2.96F, 3, 1, 1, 0.0F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(-1.5F, 6.0F, -2.96F, 3, 1, 1, 0.0F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F, 0.0F, -0.4F, -0.2F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(-0.5F, 1.95F, -3.15F, 1, 7, 1, 0.0F, -0.45F, 0.95F, -0.4F, -0.45F, 0.95F, -0.4F, -0.45F, 0.95F, -0.4F, -0.45F, 0.95F, -0.4F, -0.45F, 0.0F, -0.4F, -0.45F, 0.0F, -0.4F, -0.45F, 0.0F, -0.4F, -0.45F, 0.0F, -0.4F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(-5.5F, 8.0F, -3.5F, 6, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(-5.5F, 7.95F, -3.7F, 6, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[8].addShapeBox(-3.0F, 9.0F, -3.7F, 4, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[9].addShapeBox(-4.0F, 0.0F, -2.0F, 2, 13, 4, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.4F, 0.0F, 0.7F, 0.0F, 0.0F, 0.7F, 0.0F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F);
      this.bodyModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[10].addShapeBox(-2.0F, 0.0F, -2.0F, 2, 13, 4, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.7F, 0.0F, -1.0F, 0.7F, 0.0F, -1.0F, 0.7F, 0.0F, 0.0F, 0.7F);
      this.bodyModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[11].addShapeBox(0.0F, 0.0F, -2.0F, 2, 13, 4, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -1.0F, 0.7F, 0.0F, 0.0F, 0.7F, 0.0F, 0.0F, 0.7F, 0.0F, -1.0F, 0.7F);
      this.bodyModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[12].addShapeBox(2.0F, 0.0F, -2.0F, 2, 13, 4, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F, 0.0F, 0.0F, 0.7F);
      this.bodyModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[13].addShapeBox(-0.5F, 8.0F, -3.5F, 4, 4, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[14].addShapeBox(2.0F, 9.0F, -3.7F, 2, 4, 1, 0.0F, 0.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, -3.0F, -2.0F, 0.0F, 0.0F, -2.0F, 0.0F);
      this.bodyModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[15].addShapeBox(-0.5F, 7.95F, -3.7F, 4, 2, 1, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F, -1.0F, -1.0F, 0.0F);
      this.bodyModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[16].addShapeBox(-2.5F, 0.0F, -2.5F, 5, 1, 0, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, 0.2F);
      this.bodyModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[17].addShapeBox(-3.3F, 1.3F, -2.5F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[18].addShapeBox(-3.3F, -0.7F, -2.5F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[19].addShapeBox(2.3F, -0.7F, -2.5F, 8, 8, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, -7.0F, -5.7F, 0.1F, 0.0F, -5.7F, 0.1F);
      this.bodyModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[20].addShapeBox(2.3F, 1.3F, -2.5F, 8, 14, 1, 0.0F, 0.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, -7.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, -7.0F, -6.7F, 0.1F, 0.0F, -6.7F, 0.1F);
      this.bodyModel[20].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[21].addShapeBox(-3.3F, -0.6F, 2.0F, 8, 10, 2, 0.0F, 0.0F, 0.1F, 0.1F, -7.0F, 0.1F, 0.1F, -7.0F, 0.1F, -0.6F, 0.0F, 0.1F, -0.6F, -3.0F, -6.0F, 0.1F, -5.0F, -7.7F, 0.1F, -5.0F, -7.7F, -0.6F, -3.0F, -6.0F, -0.6F);
      this.bodyModel[21].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[22].addShapeBox(-4.7F, -0.6F, 2.0F, 8, 10, 2, 0.0F, -7.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, -0.6F, -7.0F, 0.1F, -0.6F, -5.0F, -7.7F, 0.1F, -3.0F, -6.0F, 0.1F, -3.0F, -6.0F, -0.6F, -5.0F, -7.7F, -0.6F);
      this.bodyModel[22].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[23].addShapeBox(-3.0F, 1.0F, 2.15F, 6, 1, 3, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, -0.5F, 0.4F, 0.0F, -0.5F, 0.4F, 0.5F, 0.0F, 0.4F, 0.5F, 0.0F, 0.4F, 0.5F, 0.0F, 0.4F, 0.5F, 0.0F);
      this.bodyModel[23].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[24].addShapeBox(-3.0F, 2.5F, 2.0F, 6, 5, 3, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F, 0.35F, 0.0F, 0.0F);
      this.bodyModel[24].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[25].addShapeBox(-3.0F, 2.5F, 2.15F, 6, 5, 3, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.25F, 0.0F);
      this.bodyModel[25].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[26].addShapeBox(3.0F, 1.7F, 2.05F, 2, 5, 3, 0.0F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F);
      this.bodyModel[26].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[27].addShapeBox(3.0F, 1.2F, 2.1F, 2, 2, 3, 0.0F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F);
      this.bodyModel[27].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[28].addShapeBox(-5.0F, 1.7F, 2.05F, 2, 5, 3, 0.0F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F);
      this.bodyModel[28].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[29].addShapeBox(-5.0F, 1.2F, 2.1F, 2, 2, 3, 0.0F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, 0.0F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F, -0.2F, -0.2F, -0.23F);
      this.bodyModel[29].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[30].addShapeBox(-3.2F, 9.15F, 1.5F, 3, 2, 2, 0.0F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F, -0.25F, 0.35F, -0.2F);
      this.bodyModel[30].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[31].addShapeBox(-2.2F, 9.35F, 2.6F, 1, 1, 1, 0.0F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F, -0.32F, 0.2F, -0.2F);
      this.bodyModel[31].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[32].addShapeBox(-2.2F, 9.55F, 2.65F, 1, 1, 1, 0.0F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F, -0.28F, -0.32F, -0.2F);
      this.bodyModel[32].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[33].addShapeBox(-2.2F, 11.35F, 2.45F, 1, 1, 1, 0.0F, -0.2F, 0.35F, -0.2F, -0.2F, 0.35F, -0.2F, -0.2F, 0.35F, -0.2F, -0.2F, 0.35F, -0.2F, -0.1F, 0.35F, -0.2F, -0.1F, 0.35F, -0.2F, -0.1F, 0.35F, -0.2F, -0.1F, 0.35F, -0.2F);
      this.bodyModel[33].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[34].addShapeBox(-2.2F, 13.5F, 2.45F, 1, 1, 1, 0.0F, -0.18F, 0.8F, -0.2F, -0.18F, 0.8F, -0.2F, -0.18F, 0.8F, -0.2F, -0.18F, 0.8F, -0.2F, -0.22F, 0.8F, -0.2F, -0.22F, 0.8F, -0.2F, -0.22F, 0.8F, -0.2F, -0.22F, 0.8F, -0.2F);
      this.bodyModel[34].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[35].addShapeBox(-2.2F, 15.9F, 2.45F, 1, 1, 1, 0.0F, -0.15F, 0.2F, -0.2F, -0.15F, 0.2F, -0.2F, -0.15F, 0.2F, -0.2F, -0.15F, 0.2F, -0.2F, -0.2F, -0.8F, -0.2F, -0.2F, -0.8F, -0.2F, -0.2F, -0.8F, -0.2F, -0.2F, -0.8F, -0.2F);
      this.bodyModel[35].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[36].addShapeBox(-2.2F, 15.5F, 2.5F, 1, 1, 1, 0.0F, -0.2F, 0.2F, -0.2F, -0.2F, 0.2F, -0.2F, -0.2F, 0.2F, -0.2F, -0.2F, 0.2F, -0.2F, -0.15F, -0.8F, -0.2F, -0.15F, -0.8F, -0.2F, -0.15F, -0.8F, -0.2F, -0.15F, -0.8F, -0.2F);
      this.bodyModel[36].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[37].addShapeBox(-0.5F, 1.3F, 3.15F, 1, 1, 2, 0.0F, -0.2F, 0.35F, 0.0F, -0.2F, 0.35F, 0.0F, -0.2F, 0.35F, -0.25F, -0.2F, 0.35F, -0.25F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F);
      this.bodyModel[37].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[38].addShapeBox(-0.5F, 2.0F, 4.2F, 1, 5, 1, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, 0.0F, -0.2F, 0.0F, -0.05F, -0.2F, 0.0F, -0.05F, -0.2F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F, -0.2F, 0.5F, 0.0F);
      this.bodyModel[38].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftArmModel_1() {
      this.leftArmModel[0] = new ModelRendererTurbo(this, 361, 1, this.textureX, this.textureY);
      this.leftArmModel[1] = new ModelRendererTurbo(this, 81, 17, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, -2.0F, -2.0F, 4, 11, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[1].addShapeBox(-0.8F, 7.0F, -2.0F, 4, 2, 4, 0.0F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F);
      this.leftArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightArmModel_1() {
      this.rightArmModel[0] = new ModelRendererTurbo(this, 337, 1, this.textureX, this.textureY);
      this.rightArmModel[1] = new ModelRendererTurbo(this, 17, 17, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.0F, -2.0F, 4, 11, 4, 0.0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[1].addShapeBox(-3.2F, 7.0F, -2.0F, 4, 2, 4, 0.0F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F, 0.25F, 0.0F, 0.25F);
      this.rightArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftLegModel_1() {
      this.leftLegModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.leftLegModel[1] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.leftLegModel[2] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.leftLegModel[3] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.leftLegModel[4] = new ModelRendererTurbo(this, 89, 1, this.textureX, this.textureY);
      this.leftLegModel[5] = new ModelRendererTurbo(this, 161, 1, this.textureX, this.textureY);
      this.leftLegModel[6] = new ModelRendererTurbo(this, 257, 1, this.textureX, this.textureY);
      this.leftLegModel[7] = new ModelRendererTurbo(this, 281, 1, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, 11.0F, -3.75F, 4, 1, 2, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.1F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[1].addShapeBox(-0.5F, 10.2F, -4.0F, 1, 1, 4, 0.0F, -0.3F, -0.4F, -0.25F, -0.3F, -0.5F, -0.25F, -0.3F, -0.5F, -2.6F, -0.3F, -0.4F, -2.6F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -2.6F, -0.3F, 0.0F, -2.6F);
      this.leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[2].addShapeBox(-2.0F, 10.2F, -2.75F, 4, 1, 1, 0.0F, -0.25F, -0.5F, -0.3F, -2.1F, -0.5F, 0.3F, -2.1F, -0.4F, -0.9F, -0.25F, -0.4F, -0.3F, -0.25F, 0.0F, -0.3F, -2.1F, 0.0F, 0.3F, -2.1F, 0.0F, -0.9F, -0.25F, 0.0F, -0.3F);
      this.leftLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[3].addShapeBox(-2.0F, 10.2F, -2.75F, 4, 1, 1, 0.0F, -2.1F, -0.5F, 0.3F, -0.25F, -0.5F, -0.3F, -0.25F, -0.4F, -0.3F, -2.1F, -0.4F, -0.9F, -2.1F, 0.0F, 0.3F, -0.25F, 0.0F, -0.3F, -0.25F, 0.0F, -0.3F, -2.1F, 0.0F, -0.9F);
      this.leftLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[4].addShapeBox(-2.0F, 6.05F, -2.0F, 4, 6, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F);
      this.leftLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[5].addShapeBox(-2.0F, 11.05F, -2.0F, 4, 2, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F);
      this.leftLegModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[6].addShapeBox(-2.0F, -2.0F, -2.0F, 4, 7, 4, 0.0F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.45F, 0.5F, 0.0F, 0.55F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.15F);
      this.leftLegModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[7].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 2, 4, 0.0F, 0.0F, 0.0F, 0.45F, 0.5F, 0.0F, 0.55F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.15F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.leftLegModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightLegModel_1() {
      this.rightLegModel[0] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.rightLegModel[1] = new ModelRendererTurbo(this, 33, 1, this.textureX, this.textureY);
      this.rightLegModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.rightLegModel[3] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.rightLegModel[4] = new ModelRendererTurbo(this, 113, 1, this.textureX, this.textureY);
      this.rightLegModel[5] = new ModelRendererTurbo(this, 137, 1, this.textureX, this.textureY);
      this.rightLegModel[6] = new ModelRendererTurbo(this, 233, 1, this.textureX, this.textureY);
      this.rightLegModel[7] = new ModelRendererTurbo(this, 305, 1, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, 11.0F, -3.75F, 4, 1, 2, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.1F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[1].addShapeBox(-2.0F, 10.2F, -2.75F, 4, 1, 1, 0.0F, -0.25F, -0.5F, -0.3F, -2.1F, -0.5F, 0.3F, -2.1F, -0.4F, -0.9F, -0.25F, -0.4F, -0.3F, -0.25F, 0.0F, -0.3F, -2.1F, 0.0F, 0.3F, -2.1F, 0.0F, -0.9F, -0.25F, 0.0F, -0.3F);
      this.rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[2].addShapeBox(-2.0F, 10.2F, -2.75F, 4, 1, 1, 0.0F, -2.1F, -0.5F, 0.3F, -0.25F, -0.5F, -0.3F, -0.25F, -0.4F, -0.3F, -2.1F, -0.4F, -0.9F, -2.1F, 0.0F, 0.3F, -0.25F, 0.0F, -0.3F, -0.25F, 0.0F, -0.3F, -2.1F, 0.0F, -0.9F);
      this.rightLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[3].addShapeBox(-0.5F, 10.2F, -4.0F, 1, 1, 4, 0.0F, -0.3F, -0.4F, -0.25F, -0.3F, -0.5F, -0.25F, -0.3F, -0.5F, -2.6F, -0.3F, -0.4F, -2.6F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -0.25F, -0.3F, 0.0F, -2.6F, -0.3F, 0.0F, -2.6F);
      this.rightLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[4].addShapeBox(-2.0F, 6.05F, -2.0F, 4, 6, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F);
      this.rightLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[5].addShapeBox(-2.0F, 11.05F, -2.0F, 4, 2, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F, 0.02F, -1.0F, 0.02F);
      this.rightLegModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[6].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 2, 4, 0.0F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.45F, 0.0F, 0.0F, 0.15F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.rightLegModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[7].addShapeBox(-2.0F, -2.0F, -2.0F, 4, 7, 4, 0.0F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.45F, 0.0F, 0.0F, 0.15F, 0.5F, 0.0F, 0.55F);
      this.rightLegModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initskirtFrontModel_1() {
      this.skirtFrontModel[0] = new ModelRendererTurbo(this, 433, 1, this.textureX, this.textureY);
      this.skirtFrontModel[0].addShapeBox(-2.5F, -2.0F, -2.0F, 5, 6, 2, 0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F, 0.4F, 0.0F, 0.7F);
      this.skirtFrontModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
