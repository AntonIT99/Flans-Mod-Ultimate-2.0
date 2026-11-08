package com.flansmodultimate.compat.tap.model.NewAge;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class Modelkingsguardbody extends ModelCustomArmour {
   int textureX = 512;
   int textureY = 64;

   public Modelkingsguardbody() {
      this.bodyModel = new ModelRendererTurbo[20];
      this.leftArmModel = new ModelRendererTurbo[4];
      this.rightArmModel = new ModelRendererTurbo[4];
      this.leftLegModel = new ModelRendererTurbo[3];
      this.rightLegModel = new ModelRendererTurbo[3];
      this.initbodyModel_1();
      this.initleftArmModel_1();
      this.initrightArmModel_1();
      this.initleftLegModel_1();
      this.initrightLegModel_1();
   }

   private void initbodyModel_1() {
      this.bodyModel[0] = new ModelRendererTurbo(this, 17, 1, this.textureX, this.textureY);
      this.bodyModel[1] = new ModelRendererTurbo(this, 41, 1, this.textureX, this.textureY);
      this.bodyModel[2] = new ModelRendererTurbo(this, 49, 1, this.textureX, this.textureY);
      this.bodyModel[3] = new ModelRendererTurbo(this, 57, 1, this.textureX, this.textureY);
      this.bodyModel[4] = new ModelRendererTurbo(this, 65, 1, this.textureX, this.textureY);
      this.bodyModel[5] = new ModelRendererTurbo(this, 73, 1, this.textureX, this.textureY);
      this.bodyModel[6] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.bodyModel[7] = new ModelRendererTurbo(this, 81, 1, this.textureX, this.textureY);
      this.bodyModel[8] = new ModelRendererTurbo(this, 105, 1, this.textureX, this.textureY);
      this.bodyModel[9] = new ModelRendererTurbo(this, 217, 1, this.textureX, this.textureY);
      this.bodyModel[10] = new ModelRendererTurbo(this, 241, 1, this.textureX, this.textureY);
      this.bodyModel[11] = new ModelRendererTurbo(this, 97, 1, this.textureX, this.textureY);
      this.bodyModel[12] = new ModelRendererTurbo(this, 265, 1, this.textureX, this.textureY);
      this.bodyModel[13] = new ModelRendererTurbo(this, 273, 1, this.textureX, this.textureY);
      this.bodyModel[14] = new ModelRendererTurbo(this, 121, 1, this.textureX, this.textureY);
      this.bodyModel[15] = new ModelRendererTurbo(this, 145, 1, this.textureX, this.textureY);
      this.bodyModel[16] = new ModelRendererTurbo(this, 233, 1, this.textureX, this.textureY);
      this.bodyModel[17] = new ModelRendererTurbo(this, 257, 1, this.textureX, this.textureY);
      this.bodyModel[18] = new ModelRendererTurbo(this, 18, 9, this.textureX, this.textureY);
      this.bodyModel[19] = new ModelRendererTurbo(this, 17, 15, this.textureX, this.textureY);
      this.bodyModel[0].addShapeBox(-0.75F, 1.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[1].addShapeBox(-0.75F, 3.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[2].addShapeBox(-0.75F, 5.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[3].addShapeBox(-0.75F, 7.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[4].addShapeBox(-1.0F, 8.7F, -2.4F, 1, 1, 1, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F);
      this.bodyModel[4].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[5].addShapeBox(0.0F, 8.7F, -2.4F, 1, 1, 1, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F, -0.2F, 0.31F, 0.0F);
      this.bodyModel[5].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[6].addShapeBox(-0.5F, 8.7F, -2.85F, 1, 1, 1, 0.0F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F, -0.2F, -0.3F, -0.4F);
      this.bodyModel[6].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[7].addShapeBox(-4.2F, 8.7F, -2.5F, 4, 1, 5, 0.0F, 0.2F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F);
      this.bodyModel[7].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[8].addShapeBox(0.35F, 8.7F, -2.5F, 4, 1, 5, 0.0F, 0.2F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 1.2F, 0.3F, -0.2F, 0.2F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 0.0F, 0.3F, -0.2F, 1.2F, 0.3F, -0.2F);
      this.bodyModel[8].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[9].addShapeBox(-4.0F, -0.1F, -2.0F, 4, 1, 4, 0.0F, 0.0F, 0.0F, 0.1F, -1.5F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F);
      this.bodyModel[9].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[10].addShapeBox(0.0F, -0.1F, -2.0F, 4, 1, 4, 0.0F, -1.5F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F);
      this.bodyModel[10].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[11].addShapeBox(0.0F, 0.0F, -2.2F, 2, 1, 1, 0.0F, -0.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.5F, 0.0F, -1.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F);
      this.bodyModel[11].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[12].addShapeBox(-0.5F, 1.0F, -2.15F, 1, 11, 1, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, 0.0F, 0.0F, -0.4F, -0.1F, 0.0F, -0.4F, -0.1F, 0.0F, -0.4F, -0.1F, 0.0F, -0.4F, -0.1F, 0.0F);
      this.bodyModel[12].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[13].addShapeBox(-4.0F, 0.9F, -2.0F, 8, 11, 4, 0.0F, 0.1F, -0.1F, 0.1F, 0.1F, -0.1F, 0.1F, 0.1F, -0.1F, 0.1F, 0.1F, -0.1F, 0.1F, 0.1F, 0.6F, 0.1F, 0.1F, 0.6F, 0.1F, 0.1F, 0.6F, 0.1F, 0.1F, 0.6F, 0.1F);
      this.bodyModel[13].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[14].addShapeBox(-2.0F, 0.0F, -2.2F, 2, 1, 1, 0.0F, 0.0F, 0.0F, 0.0F, -0.75F, 0.0F, 0.0F, -0.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.5F, 0.0F);
      this.bodyModel[14].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[15].addShapeBox(-0.75F, 2.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[15].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[16].addShapeBox(-0.75F, 4.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[16].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[17].addShapeBox(-0.75F, 6.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[17].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[18].addShapeBox(-1.0F, 0.0F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[18].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[18].rotateAngleZ = (float) (Math.PI / 4);
      this.bodyModel[19].addShapeBox(-2.3F, -1.25F, -1.5F, 2, 1, 1, 0.0F, -1.0F, -0.75F, 0.75F, -0.5F, -0.75F, 0.75F, -0.5F, -0.75F, 0.5F, -1.0F, -0.75F, 0.5F, -1.0F, 0.25F, 0.75F, -0.5F, 0.25F, 0.75F, -0.5F, -0.5F, 0.5F, -1.0F, 0.0F, 0.5F);
      this.bodyModel[19].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.bodyModel[19].rotateAngleZ = (float) (Math.PI / 4);
   }

   private void initleftArmModel_1() {
      this.leftArmModel[0] = new ModelRendererTurbo(this, 25, 1, this.textureX, this.textureY);
      this.leftArmModel[1] = new ModelRendererTurbo(this, 169, 1, this.textureX, this.textureY);
      this.leftArmModel[2] = new ModelRendererTurbo(this, 329, 1, this.textureX, this.textureY);
      this.leftArmModel[3] = new ModelRendererTurbo(this, 297, 1, this.textureX, this.textureY);
      this.leftArmModel[0].addShapeBox(-1.0F, -2.0F, -2.0F, 4, 9, 4, 0.0F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F);
      this.leftArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[1].addShapeBox(-0.95F, -2.3F, -1.0F, 4, 1, 2, 0.0F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F);
      this.leftArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[2].addShapeBox(-1.0F, 6.0F, -3.0F, 4, 8, 5, 0.0F, 0.1F, -4.8572257E-16F, -0.8F, 0.3F, -4.1633363E-17F, -0.8F, 0.3F, 4.718448E-16F, 0.2F, 0.1F, -9.298118E-16F, 0.2F, 0.1F, -6.0F, -0.8F, 0.2F, -6.0F, -0.8F, 0.2F, -6.0F, 0.2F, 0.1F, -6.0F, 0.2F);
      this.leftArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftArmModel[3].addShapeBox(1.0F, -2.8F, -0.55F, 2, 2, 1, 0.0F, 0.1F, -0.4F, -0.3F, -0.6F, -0.4F, -0.3F, -0.6F, -0.4F, -0.1F, 0.1F, -0.4F, -0.1F, 0.1F, 0.0F, -0.3F, -0.6F, -1.0214052E-14F, -0.3F, -0.6F, 0.0F, -0.1F, 0.1F, 0.0F, -0.1F);
      this.leftArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightArmModel_1() {
      this.rightArmModel[0] = new ModelRendererTurbo(this, 1, 1, this.textureX, this.textureY);
      this.rightArmModel[1] = new ModelRendererTurbo(this, 193, 1, this.textureX, this.textureY);
      this.rightArmModel[2] = new ModelRendererTurbo(this, 305, 1, this.textureX, this.textureY);
      this.rightArmModel[3] = new ModelRendererTurbo(this, 321, 1, this.textureX, this.textureY);
      this.rightArmModel[0].addShapeBox(-3.0F, -2.0F, -2.0F, 4, 9, 4, 0.0F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.05F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F, 0.125F, 0.0F, 0.125F);
      this.rightArmModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[1].addShapeBox(-3.05F, -2.3F, -1.0F, 4, 1, 2, 0.0F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, -0.1F);
      this.rightArmModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[2].addShapeBox(-3.0F, 6.0F, -3.0F, 4, 2, 5, 0.0F, 0.3F, -3.6082248E-16F, -0.8F, 0.1F, -4.1633363E-17F, -0.8F, 0.1F, -4.1633363E-17F, 0.2F, 0.4F, 4.440892E-16F, 0.2F, 0.2F, 0.0F, -0.8F, 0.1F, 0.0F, -0.8F, 0.1F, 0.0F, 0.2F, 0.2F, 0.0F, 0.2F);
      this.rightArmModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightArmModel[3].addShapeBox(-3.0F, -2.8F, -0.55F, 2, 1, 1, 0.0F, -0.6F, -0.4F, -0.3F, 0.0F, -0.4F, -0.4F, 0.0F, -0.4F, -0.1F, -0.6F, -0.4F, -0.1F, -0.6F, 0.0F, -0.3F, 0.0F, -1.0214052E-14F, -0.4F, 0.0F, 0.0F, -0.1F, -0.6F, 0.0F, -0.1F);
      this.rightArmModel[3].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initleftLegModel_1() {
      this.leftLegModel[0] = new ModelRendererTurbo(this, 129, 1, this.textureX, this.textureY);
      this.leftLegModel[1] = new ModelRendererTurbo(this, 177, 1, this.textureX, this.textureY);
      this.leftLegModel[2] = new ModelRendererTurbo(this, 353, 1, this.textureX, this.textureY);
      this.leftLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 11, 4, 0.0F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.leftLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[1].addShapeBox(-2.0F, 10.1F, -3.3F, 4, 2, 6, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, -0.375F, 0.25F, 0.0F, -0.375F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, -0.375F, 0.0F, 0.0F, -0.25F);
      this.leftLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.leftLegModel[2].addShapeBox(1.1F, -0.1F, -0.5F, 1, 11, 1, 0.0F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F);
      this.leftLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
   }

   private void initrightLegModel_1() {
      this.rightLegModel[0] = new ModelRendererTurbo(this, 153, 1, this.textureX, this.textureY);
      this.rightLegModel[1] = new ModelRendererTurbo(this, 201, 1, this.textureX, this.textureY);
      this.rightLegModel[2] = new ModelRendererTurbo(this, 361, 1, this.textureX, this.textureY);
      this.rightLegModel[0].addShapeBox(-2.0F, -0.1F, -2.0F, 4, 11, 4, 0.0F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, -0.5F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F, 0.1F, 0.0F, 0.1F);
      this.rightLegModel[0].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[1].addShapeBox(-2.0F, 10.1F, -3.3F, 4, 2, 6, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, -0.375F, 0.25F, 0.0F, -0.375F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, 0.0F, 0.25F, 0.0F, -0.375F, 0.25F, 0.0F, -0.375F);
      this.rightLegModel[1].setRotationPoint(0.0F, 0.0F, 0.0F);
      this.rightLegModel[2].addShapeBox(-2.1F, -0.1F, -0.5F, 1, 11, 1, 0.0F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.5F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F, 0.1F, -0.75F, -0.4F);
      this.rightLegModel[2].setRotationPoint(0.0F, 0.0F, 0.0F);
   }
}
