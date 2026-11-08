//This File was created with the Minecraft-SMP Modelling Toolbox 2.2.0.0
// Copyright (C) 2021 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelKhomeini extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 64;

	public ModelKhomeini() //Same as Filename
	{
		headModel = new ModelRendererTurbo[16];
		bodyModel = new ModelRendererTurbo[6];
		leftArmModel = new ModelRendererTurbo[1];
		rightArmModel = new ModelRendererTurbo[1];
		leftLegModel = new ModelRendererTurbo[3];
		rightLegModel = new ModelRendererTurbo[3];

		initheadModel_1();
		initbodyModel_1();
		initleftArmModel_1();
		initrightArmModel_1();
		initleftLegModel_1();
		initrightLegModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 18
		headModel[1] = new ModelRendererTurbo(this, 9, 1, textureX, textureY); // Box 19
		headModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 20
		headModel[3] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 21
		headModel[4] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 22
		headModel[5] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 29
		headModel[6] = new ModelRendererTurbo(this, 81, 1, textureX, textureY); // Box 0
		headModel[7] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 1
		headModel[8] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 2
		headModel[9] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 3
		headModel[10] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 4
		headModel[11] = new ModelRendererTurbo(this, 65, 9, textureX, textureY); // Box 5
		headModel[12] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 6
		headModel[13] = new ModelRendererTurbo(this, 25, 9, textureX, textureY); // Box 7
		headModel[14] = new ModelRendererTurbo(this, 57, 9, textureX, textureY); // Box 8
		headModel[15] = new ModelRendererTurbo(this, 89, 9, textureX, textureY); // Box 9

		headModel[0].addShapeBox(-3F, -5F, -4F, 2, 1, 1, 0F,-0.2F, -0.2F, 0.2F, 0.2F, -0.5F, 0.2F, 0.2F, -0.5F, 0.2F, 0.2F, -0.2F, 0.2F, 0.2F, -0.1F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, -0.1F, 0.2F); // Box 18
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(1F, -5F, -4F, 2, 1, 1, 0F,0.2F, -0.5F, 0.2F, -0.2F, -0.2F, 0.2F, -0.2F, -0.2F, 0.2F, 0.2F, -0.5F, 0.2F, 0.4F, 0F, 0.2F, 0.2F, -0.1F, 0.2F, 0F, -0.1F, 0.2F, 0F, 0F, 0.2F); // Box 19
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(-4F, -1F, -4F, 2, 1, 5, 0F,0.2F, 0.5F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 1F, 1.5F, 0.2F, 2.5F, 1.5F, 0.2F, 0.5F, 0.3F, 2.2F, 0.5F, 0.3F, 2.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F); // Box 20
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(-2F, -2.5F, -4F, 2, 1, 1, 0F,0F, -0.8F, 0.2F, 0F, -0.3F, 0.2F, 0F, -0.3F, 0.2F, 0F, -0.8F, 0.2F, 0.5F, 0.7F, 0.4F, 0F, -0.3F, 0.2F, 0F, -0.3F, 0.2F, 0.5F, 0.7F, 0.2F); // Box 21
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(0F, -2.5F, -4F, 2, 1, 1, 0F,0F, -0.3F, 0.2F, 0F, -0.8F, 0.2F, 0F, -0.8F, 0.2F, 0F, -0.3F, 0.2F, 0F, -0.3F, 0.2F, 0.5F, 0.7F, 0.4F, 0.5F, 0.7F, 0.2F, 0F, -0.3F, 0.2F); // Box 22
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-4F, 0.5F, -4F, 8, 1, 5, 0F,0.2F, 0F, 0.3F, 0.2F, 0F, 0.3F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, -2F, 0.8F, 1.2F, -2F, 0.8F, 1.2F, -2.8F, 0F, 0.2F, -2.8F, 0F, 0.2F); // Box 29
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(2F, -1F, -4F, 2, 1, 5, 0F,0.2F, 0F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 2.5F, 1.5F, 0.2F, 1F, 1.5F, 2.2F, 0.5F, 0.3F, 0.2F, 0.5F, 0.3F, 0.2F, 0.5F, 0.2F, 2.2F, 0.5F, 0.2F); // Box 0
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(-1F, -1F, -3.9F, 2, 1, 5, 0F,-0.2F, -0.2F, 0.2F, -0.2F, -0.2F, 0.2F, 0F, 1F, 1.5F, 0F, 1F, 1.5F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0.5F, 0.2F); // Box 1
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(0F, -8F, -4F, 4, 1, 8, 0F,-1.5F, 1.9F, 0.5F, 0F, 0.5F, 0.2F, 0F, 0F, 0.5F, -1F, 0.5F, 0.5F, -1.5F, 1F, 1.5F, 0F, 2.5F, 0.5F, 0F, 4.5F, 0.4F, 0F, 4.5F, 0F); // Box 2
		headModel[8].setRotationPoint(0F, 0F, 0F);

		headModel[9].addShapeBox(-4F, -8F, -4F, 4, 1, 8, 0F,0F, 0.5F, 0.2F, -1.5F, 1.9F, 0.5F, -1F, 0.5F, 0.5F, 0F, 0F, 0.5F, 0F, 2.5F, 0.5F, -1.5F, 1F, 1.5F, 0F, 4.5F, 0F, 0F, 4.5F, 0.4F); // Box 3
		headModel[9].setRotationPoint(0F, 0F, 0F);

		headModel[10].addShapeBox(-1.5F, -9.5F, -4F, 3, 3, 8, 0F,0F, 0.4F, 0.5F, 0F, 0.4F, 0.5F, -0.5F, -1F, 0.5F, -0.5F, -1F, 0.5F, 0F, 0.5F, 1.5F, 0F, 0.5F, 1.5F, 0F, 4F, 0F, 0F, 4F, 0F); // Box 4
		headModel[10].setRotationPoint(0F, 0F, 0F);

		headModel[11].addShapeBox(-6F, -8F, -4F, 2, 2, 8, 0F,-1.5F, -0.5F, 0F, 0F, 0.5F, 0.2F, 0F, 0F, 0.5F, -1F, -1.5F, 0F, -1F, 1.2F, 0.2F, 0F, 1.5F, 0.5F, 0F, 3.5F, 0.4F, -0.8F, 3F, -0.2F); // Box 5
		headModel[11].setRotationPoint(0F, 0F, 0F);

		headModel[12].addShapeBox(-4F, -8F, 5F, 4, 1, 1, 0F,0F, 0F, 0.5F, -1F, 0.5F, 0.5F, -1.5F, 0F, -0.5F, -1F, -0.2F, -1F, 0F, 4.5F, 0.6F, 0F, 4.5F, 1F, -0.5F, 4F, 0F, -1.5F, 4F, -0.5F); // Box 6
		headModel[12].setRotationPoint(0F, 0F, 0F);

		headModel[13].addShapeBox(0F, -8F, 5F, 4, 1, 1, 0F,-1F, 0.5F, 0.5F, 0F, 0F, 0.5F, -1F, -0.2F, -1F, -1.5F, 0F, -0.5F, 0F, 4.5F, 1F, 0F, 4.5F, 0.6F, -1.5F, 4F, -0.5F, -0.5F, 4F, 0F); // Box 7
		headModel[13].setRotationPoint(0F, 0F, 0F);

		headModel[14].addShapeBox(-1.5F, -9.5F, 5F, 3, 3, 1, 0F,-0.5F, -1F, 0.5F, -0.5F, -1F, 0.5F, 0F, -1.5F, -0.5F, 0F, -1.5F, -0.5F, 0F, 4F, 1F, 0F, 4F, 1F, -1F, 3.5F, 0F, -1F, 3.5F, 0F); // Box 8
		headModel[14].setRotationPoint(0F, 0F, 0F);

		headModel[15].addShapeBox(4F, -8F, -4F, 2, 2, 8, 0F,0F, 0.5F, 0.2F, -1.5F, -0.5F, 0F, -1F, -1.5F, 0F, 0F, 0F, 0.5F, 0F, 1.5F, 0.5F, -1F, 1.2F, 0.2F, -0.8F, 3F, -0.2F, 0F, 3.5F, 0.4F); // Box 9
		headModel[15].setRotationPoint(0F, 0F, 0F);
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 10
		bodyModel[1] = new ModelRendererTurbo(this, 113, 17, textureX, textureY); // Box 11
		bodyModel[2] = new ModelRendererTurbo(this, 33, 25, textureX, textureY); // Box 12
		bodyModel[3] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 13
		bodyModel[4] = new ModelRendererTurbo(this, 49, 41, textureX, textureY); // Box 19
		bodyModel[5] = new ModelRendererTurbo(this, 65, 41, textureX, textureY); // Box 20

		bodyModel[0].addShapeBox(-4F, 0F, -2F, 8, 12, 4, 0F,0F, 0F, 0.05F, 0F, 0F, 0.05F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.05F, 0F, 0F, 0.05F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 10
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(-4F, 0F, -2F, 2, 12, 4, 0F,0.15F, 0.2F, 0.15F, 0F, 0.2F, 0.15F, 3F, 0.2F, 0.15F, 0.15F, 0.2F, 0.15F, 0.15F, 0F, 0.15F, 5F, 0F, 0.15F, 3F, 0F, 0.15F, 0.15F, 0F, 0.15F); // Box 11
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(2F, 0F, -2F, 2, 7, 4, 0F,0F, 0.2F, 0.25F, 0.15F, 0.2F, 0.25F, 0.15F, 0.2F, 0.15F, 3F, 0.2F, 0.15F, 4.2F, 0F, 0.25F, 0.15F, 0F, 0.25F, 0.15F, 0F, 0.15F, 3F, 0F, 0.15F); // Box 12
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(2F, 7F, -2F, 2, 5, 4, 0F,4.2F, 0F, 0.25F, 0.15F, 0F, 0.25F, 0.15F, 0F, 0.15F, 3F, 0F, 0.15F, 4F, 0F, 0.3F, 0.15F, 0F, 0.25F, 0.15F, 0F, 0.15F, 3F, 0F, 0.15F); // Box 13
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-4F, 0F, -2F, 2, 12, 4, 0F,0.2F, 0.5F, 0.4F, 0.8F, 0.7F, 0.4F, 2F, 0.7F, 0.4F, 0.2F, 0.5F, 0.4F, 0.5F, 0F, 0.6F, 0.5F, 0F, 0.8F, 2F, 0F, 1F, 0.5F, 0F, 0.6F); // Box 19
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(2F, 0F, -2F, 2, 12, 4, 0F,0.8F, 0.7F, 0.4F, 0.2F, 0.5F, 0.4F, 0.2F, 0.5F, 0.4F, 2F, 0.7F, 0.4F, 0.5F, 0F, 0.8F, 0.5F, 0F, 0.6F, 0.5F, 0F, 0.6F, 2F, 0F, 1F); // Box 20
		bodyModel[5].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 97, 33, textureX, textureY); // Box 17

		leftArmModel[0].addShapeBox(-1F, -2F, -2F, 4, 11, 4, 0F,0F, 0.5F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0F, 0.5F, 0F, 0.2F, 0.5F, 0.5F, 1F, 0F, 0.5F, 1F, 0F, 0.5F, 0.2F, 0.5F, 0.5F); // Box 17
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 25, 41, textureX, textureY); // Box 18

		rightArmModel[0].addShapeBox(-3F, -2F, -2F, 4, 11, 4, 0F,0.1F, 0.1F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0.1F, 0.1F, 0F, 1F, 0F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 1F, 0F, 0.5F); // Box 18
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 65, 25, textureX, textureY); // Box 14
		leftLegModel[1] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 22
		leftLegModel[2] = new ModelRendererTurbo(this, 17, 57, textureX, textureY); // Box 40

		leftLegModel[0].addShapeBox(0F, 0F, -2F, 2, 10, 4, 0F,4F, 0F, 0.3F, 0.15F, 0F, 0.25F, 0.15F, 0F, 0.15F, 3F, 0F, 0.15F, 3.2F, 0.5F, 0.5F, 0.5F, 0.5F, 0.4F, 0.5F, 0.5F, 0.15F, 2F, 0.5F, 0.4F); // Box 14
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(0F, 0F, -2F, 2, 9, 4, 0F,0.5F, 0F, 0.8F, 0.5F, 0F, 0.6F, 0.5F, 0F, 0.6F, 2F, 0F, 1F, 0.4F, 1.7F, 1F, 1.2F, 1.5F, 0.4F, 1.2F, 1.5F, 1F, 2F, 1.7F, 1.5F); // Box 22
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-2F, 10F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.5F, 0.2F, -1.1F, 1.5F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 1.8F, 0.2F, 0F, 1.8F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 40
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 16
		rightLegModel[1] = new ModelRendererTurbo(this, 81, 41, textureX, textureY); // Box 21
		rightLegModel[2] = new ModelRendererTurbo(this, 97, 49, textureX, textureY); // Box 11

		rightLegModel[0].addShapeBox(-2F, 0F, -2F, 1, 10, 4, 0F,0.15F, 0F, 0.25F, 4F, 0F, 0.25F, 3F, 0F, 0.15F, 0.15F, 0F, 0.15F, 0.5F, 0.5F, 0.25F, 4.2F, 0.5F, 0.4F, 3F, 0.5F, 0.4F, 0.5F, 0.5F, 0.15F); // Box 16
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 0F, -2F, 2, 9, 4, 0F,0.5F, 0F, 0.6F, 0.5F, 0F, 0.8F, 2F, 0F, 1F, 0.5F, 0F, 0.6F, 1.2F, 1.5F, 0.4F, 0.4F, 1.7F, 1F, 2F, 1.7F, 1.5F, 1.2F, 1.5F, 1F); // Box 21
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-2F, 10F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.5F, 0.2F, -1.1F, 1.5F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 1.8F, 0.2F, 0F, 1.8F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 11
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);
	}
}