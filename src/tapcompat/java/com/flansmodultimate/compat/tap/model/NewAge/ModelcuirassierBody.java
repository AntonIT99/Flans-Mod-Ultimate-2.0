package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelcuirassierBody extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 512;

   public ModelcuirassierBody() {
      this.bodyModel = new ModelRendererTurbo[28];
      this.leftArmModel = new ModelRendererTurbo[4];
      this.rightArmModel = new ModelRendererTurbo[4];
      this.leftLegModel = new ModelRendererTurbo[6];
      this.rightLegModel = new ModelRendererTurbo[6];
      this.initbodyModel_1();
      this.initleftArmModel_1();
      this.initrightArmModel_1();
      this.initleftLegModel_1();
      this.initrightLegModel_1();
   }

   private void initbodyModel_1() {
      this.bodyModel[0] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 177, 1, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 201, 1, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 225, 1, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 249, 1, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 273, 1, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 297, 1, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 321, 1, this.textureX, this.textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 345, 1, this.textureX, this.textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 377, 1, this.textureX, this.textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 225, 9, this.textureX, this.textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 233, 9, this.textureX, this.textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 241, 9, this.textureX, this.textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 249, 9, this.textureX, this.textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 169, 1, this.textureX, this.textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 193, 1, this.textureX, this.textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 217, 1, this.textureX, this.textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 241, 1, this.textureX, this.textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 265, 1, this.textureX, this.textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 257, 9, this.textureX, this.textureY);
      this.bodyModel[20] = new ModelRendererTurbo(this, 289, 1, this.textureX, this.textureY);
      this.bodyModel[21] = new ModelRendererTurbo(this, 273, 9, this.textureX, this.textureY);
      this.bodyModel[22] = new ModelRendererTurbo(this, 281, 9, this.textureX, this.textureY);
      this.bodyModel[23] = new ModelRendererTurbo(this, 313, 1, this.textureX, this.textureY);
      this.bodyModel[24] = new ModelRendererTurbo(this, 337, 9, this.textureX, this.textureY);
      this.bodyModel[25] = new ModelRendererTurbo(this, 337, 1, this.textureX, this.textureY);
      this.bodyModel[26] = new ModelRendererTurbo(this, 289, 9, this.textureX, this.textureY);
      this.bodyModel[27] = new ModelRendererTurbo(this, 497, 9, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(-4.0F, 6.5F, -2.5F, 4, 3, 5, 0.0F, 0.6F, 0.0F, 0.4F, 0.0F, -0.5F, 1.5F, 0.0F, 0.0F, 1.0F, 0.6F, 0.0F, 0.5F, 0.15F, 0.0F, 0.2F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F, 0.4F, 0.15F, 0.0F, 0.2F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(-4.0F, 3.5F, -2.5F, 4, 3, 5, 0.0F, 0.5F, 0.0F, 0.4F, 0.0F, 0.0F, 1.2F, 0.0F, 0.0F, 1.0F, 0.5F, 0.0F, 0.5F, 0.6F, 0.0F, 0.4F, 0.0F, 0.49F, 1.49F, 0.0F, 0.0F, 1.0F, 0.6F, 0.0F, 0.4F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-4.0F, 2.5F, -2.5F, 4, 1, 5, 0.0F, -0.2F, 0.5F, 0.5F, 0.0F, 0.5F, 0.8F, 0.0F, 0.5F, 1.0F, -0.2F, 0.5F, 0.7F, 0.4F, 0.0F, 0.4F, 0.0F, 0.0F, 1.2F, 0.0F, 0.0F, 1.0F, 0.4F, 0.0F, 0.5F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(-4.0F, -0.5F, -2.5F, 4, 2, 5, 0.0F, -0.3F, -0.3F, 0.3F, -1.5F, -0.3F, 0.2F, 0.0F, -0.3F, 0.8F, -0.3F, -0.3F, 0.5F, -0.2F, 0.5F, 0.5F, 0.0F, 0.5F, 0.8F, 0.0F, 0.5F, 1.0F, -0.2F, 0.5F, 0.7F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(0.0F, -0.5F, -2.5F, 4, 2, 5, 0.0F, -1.5F, -0.3F, 0.2F, -0.3F, -0.3F, 0.3F, -0.3F, -0.3F, 0.5F, 0.0F, -0.3F, 0.8F, 0.0F, 0.5F, 0.8F, -0.2F, 0.5F, 0.5F, -0.2F, 0.5F, 0.7F, 0.0F, 0.5F, 1.0F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(0.0F, 2.5F, -2.5F, 4, 1, 5, 0.0F, 0.0F, 0.5F, 0.8F, -0.2F, 0.5F, 0.5F, -0.2F, 0.5F, 0.7F, 0.0F, 0.5F, 1.0F, 0.0F, 0.0F, 1.2F, 0.4F, 0.0F, 0.4F, 0.4F, 0.0F, 0.5F, 0.0F, 0.0F, 1.0F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(0.0F, 3.5F, -2.5F, 4, 3, 5, 0.0F, 0.0F, 0.0F, 1.2F, 0.5F, 0.0F, 0.4F, 0.5F, 0.0F, 0.5F, 0.0F, 0.0F, 1.0F, 0.0F, 0.51F, 1.51F, 0.6F, 0.0F, 0.4F, 0.6F, 0.0F, 0.4F, 0.0F, 0.0F, 1.0F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(0.0F, 6.5F, -2.5F, 4, 3, 5, 0.0F, 0.0F, -0.5F, 1.5F, 0.6F, 0.0F, 0.4F, 0.6F, 0.0F, 0.5F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.4F, 0.15F, 0.0F, 0.2F, 0.15F, 0.0F, 0.2F, 0.0F, 0.0F, 0.4F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[8].addShapeBox(-4.0F, 9.4F, -2.0F, 8, 1, 4, 0.0F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F);
      this.bodyModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[9].addShapeBox(-4.0F, 9.2F, -2.5F, 8, 1, 5, 0.0F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.25F, -0.2F, 0.25F, 0.6F, 0.0F, 0.6F, 0.6F, 0.0F, 0.6F, 0.6F, 0.0F, 0.6F, 0.6F, 0.0F, 0.6F);
      this.bodyModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[10].addShapeBox(-4.0F, 2.0F, -2.65F, 2, 10, 1, 0.0F, 0.0F, 0.0F, -0.45F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[11].addShapeBox(-2.0F, 2.0F, -2.65F, 2, 10, 1, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[12].addShapeBox(0.0F, 2.0F, -2.65F, 2, 10, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[13].addShapeBox(2.0F, 2.0F, -2.65F, 2, 10, 1, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.45F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[14].addShapeBox(-4.0F, 1.0F, -2.65F, 2, 1, 1, 0.0F, 0.0F, 1.0F, -0.45F, 0.0F, 0.9F, -0.15F, 0.0F, 0.9F, 0.0F, 0.0F, 1.0F, -0.3F, 0.0F, 0.0F, -0.45F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[15].addShapeBox(2.0F, 1.0F, -2.65F, 2, 1, 1, 0.0F, 0.0F, 0.9F, -0.15F, 0.0F, 1.0F, -0.45F, 0.0F, 1.0F, -0.3F, 0.0F, 0.9F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.45F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[16].addShapeBox(-2.0F, 1.0F, -2.65F, 2, 1, 1, 0.0F, 0.0F, 0.9F, -0.15F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, -0.3F, 0.0F, 0.9F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[17].addShapeBox(0.0F, 1.0F, -2.65F, 2, 1, 1, 0.0F, 0.0F, -0.1F, -0.05F, 0.0F, 0.9F, -0.15F, 0.0F, 0.9F, 0.0F, 0.0F, -0.1F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[18].addShapeBox(0.0F, 8.8F, -2.65F, 2, 0, 1, 0.0F, 0.0F, 0.0F, 0.2F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.2F, 0.05F, 0.0F, 0.05F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[19].addShapeBox(2.0F, 8.8F, -2.67F, 2, 0, 5, 0.0F, 0.05F, 0.0F, 0.0F, 0.2F, 0.0F, -0.15F, 0.2F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0F, 0.05F, 0.0F, 0.05F, 0.25F, 0.0F, -0.15F, 0.25F, 0.0F, -0.1F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[20].addShapeBox(-2.0F, 1.0F, -2.75F, 2, 1, 1, 0.0F, -1.0F, 1.0F, -0.15F, 0.0F, 0.3F, 0.0F, 0.0F, -0.1F, -0.3F, -0.4F, 1.0F, 0.0F, 0.2F, -2.0F, -0.15F, 0.0F, -0.6F, 0.0F, 0.0F, 0.0F, -0.3F, 0.2F, -2.0F, -0.15F);
      this.bodyModel[20].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[21].addShapeBox(-4.0F, 2.0F, 1.5F, 2, 10, 1, 0.0F, 0.0F, 2.0F, -0.3F, 0.0F, 2.0F, 0.0F, 0.0F, 2.0F, -0.15F, 0.0F, 2.0F, -0.45F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.25F);
      this.bodyModel[21].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[22].addShapeBox(2.0F, 2.0F, 1.5F, 2, 10, 1, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 2.0F, -0.3F, 0.0F, 2.0F, -0.45F, 0.0F, 2.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.25F, 0.0F, 0.0F, -0.15F);
      this.bodyModel[22].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[23].addShapeBox(-2.0F, 8.8F, -2.65F, 2, 0, 1, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.05F, 0.0F, 0.05F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[23].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[24].addShapeBox(-4.0F, 8.8F, -2.65F, 2, 0, 5, 0.0F, 0.2F, 0.0F, -0.15F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.0F, -0.1F, 0.25F, 0.0F, -0.15F, 0.05F, 0.0F, 0.05F, 0.0F, 0.0F, -0.3F, 0.25F, 0.0F, -0.1F);
      this.bodyModel[24].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[25].addShapeBox(0.0F, 1.0F, -2.75F, 2, 1, 1, 0.0F, 0.0F, 0.3F, 0.0F, -1.0F, 1.0F, -0.15F, -0.4F, 1.0F, 0.0F, 0.0F, -0.1F, -0.3F, 0.0F, -0.6F, 0.0F, 0.2F, -2.0F, -0.15F, 0.2F, -2.0F, -0.15F, 0.0F, 0.0F, -0.3F);
      this.bodyModel[25].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[26].addShapeBox(0.0F, 2.0F, 1.5F, 2, 10, 1, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, 0.0F);
      this.bodyModel[26].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[27].addShapeBox(-2.0F, 2.0F, 1.5F, 2, 10, 1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, -0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.15F);
      this.bodyModel[27].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftArmModel_1() {
      this.leftArmModel[0] = new ModelRendererTurbo(this, 377, 9, this.textureX, this.textureY);
      this.leftArmModel[1] = new ModelRendererTurbo(this, 377, 9, this.textureX, this.textureY);
      this.leftArmModel[2] = new ModelRendererTurbo(this, 145, 17, this.textureX, this.textureY);
      this.leftArmModel[3] = new ModelRendererTurbo(this, 441, 9, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, -2.0F, -2.0F, 4, 5, 4, 0.0F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[1].addShapeBox(-1.0F, 3.0F, -2.0F, 4, 5, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      this.leftArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[2].addShapeBox(-0.9F, 5.61F, -2.0F, 4, 2, 4, 0.0F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F);
      this.leftArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[3].addBox(-1.2F, -2.09F, -0.5F, 4, 1, 1, 0.0F);
      this.leftArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightArmModel_1() {
      this.rightArmModel[0] = new ModelRendererTurbo(this, 401, 9, this.textureX, this.textureY);
      this.rightArmModel[1] = new ModelRendererTurbo(this, 425, 9, this.textureX, this.textureY);
      this.rightArmModel[2] = new ModelRendererTurbo(this, 169, 17, this.textureX, this.textureY);
      this.rightArmModel[3] = new ModelRendererTurbo(this, 465, 9, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.0F, -2.0F, 4, 5, 4, 0.0F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F, 0.02F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[1].addShapeBox(-3.0F, 3.0F, -2.0F, 4, 5, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      this.rightArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[2].addShapeBox(-3.1F, 5.61F, -2.0F, 4, 2, 4, 0.0F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, 0.0F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F, 0.07F, -0.5F, 0.15F);
      this.rightArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[3].addBox(-2.8F, -2.09F, -0.5F, 4, 1, 1, 0.0F);
      this.rightArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftLegModel_1() {
      this.leftLegModel[0] = new ModelRendererTurbo(this, 369, 1, this.textureX, this.textureY);
      this.leftLegModel[1] = new ModelRendererTurbo(this, 449, 9, this.textureX, this.textureY);
      this.leftLegModel[2] = new ModelRendererTurbo(this, 369, 9, this.textureX, this.textureY);
      this.leftLegModel[3] = new ModelRendererTurbo(this, 473, 9, this.textureX, this.textureY);
      this.leftLegModel[4] = new ModelRendererTurbo(this, 97, 17, this.textureX, this.textureY);
      this.leftLegModel[5] = new ModelRendererTurbo(this, 121, 17, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, 9.0F, -2.75F, 4, 1, 1, 0.0F, -0.5F, -0.5F, -0.55F, -0.5F, -0.5F, -0.55F, -0.5F, -0.4F, 1.0F, -0.5F, -0.4F, 1.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[1].addShapeBox(-2.0F, 6.05F, -2.0F, 4, 6, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      this.leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[2].addShapeBox(-2.0F, 10.0F, -2.75F, 4, 2, 1, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.1F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.leftLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[3].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 1, 4, 0.0F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F);
      this.leftLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[4].addShapeBox(-2.0F, -1.0F, -2.0F, 4, 3, 4, 0.0F, 0.0F, 1.0F, 0.05F, 0.0F, 1.0F, 0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.45F, 0.5F, 0.0F, 0.55F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.15F);
      this.leftLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[5].addShapeBox(-2.0F, 2.0F, -2.0F, 4, 3, 4, 0.0F, 0.0F, 0.0F, 0.45F, 0.5F, 0.0F, 0.55F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.15F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.leftLegModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightLegModel_1() {
      this.rightLegModel[0] = new ModelRendererTurbo(this, 1, 17, this.textureX, this.textureY);
      this.rightLegModel[1] = new ModelRendererTurbo(this, 393, 9, this.textureX, this.textureY);
      this.rightLegModel[2] = new ModelRendererTurbo(this, 417, 9, this.textureX, this.textureY);
      this.rightLegModel[3] = new ModelRendererTurbo(this, 25, 17, this.textureX, this.textureY);
      this.rightLegModel[4] = new ModelRendererTurbo(this, 49, 17, this.textureX, this.textureY);
      this.rightLegModel[5] = new ModelRendererTurbo(this, 73, 17, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, 6.05F, -2.0F, 4, 6, 4, 0.0F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F, 0.02F, 0.0F, 0.02F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[1].addShapeBox(-2.0F, 10.0F, -2.75F, 4, 2, 1, 0.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.1F, 0.0F, -0.25F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[2].addShapeBox(-2.0F, 9.0F, -2.75F, 4, 1, 1, 0.0F, -0.5F, -0.5F, -0.55F, -0.5F, -0.5F, -0.55F, -0.5F, -0.4F, 1.0F, -0.5F, -0.4F, 1.0F, -0.5F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.05F, 0.0F, -0.25F);
      this.rightLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[3].addShapeBox(-2.0F, 5.0F, -2.0F, 4, 1, 4, 0.0F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 0.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F, 0.08F, 1.0F, 0.08F);
      this.rightLegModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[4].addShapeBox(-2.0F, -1.0F, -2.0F, 4, 3, 4, 0.0F, 0.0F, 1.0F, 0.05F, 0.0F, 1.0F, 0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.45F, 0.0F, 0.0F, 0.15F, 0.5F, 0.0F, 0.55F);
      this.rightLegModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[5].addShapeBox(-2.0F, 2.0F, -2.0F, 4, 3, 4, 0.0F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.45F, 0.0F, 0.0F, 0.15F, 0.5F, 0.0F, 0.55F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.rightLegModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
