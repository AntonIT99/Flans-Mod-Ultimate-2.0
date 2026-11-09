//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Jeff
// Model Creator: 
// Created on: 19.05.2020 - 08:09:04
// Last changed on: 19.05.2020 - 08:09:04

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelJeff extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelJeff() //Same as Filename
	{
		headModel = new ModelRendererTurbo[10];
		bodyModel = new ModelRendererTurbo[10];
		leftArmModel = new ModelRendererTurbo[3];
		rightArmModel = new ModelRendererTurbo[2];
		leftLegModel = new ModelRendererTurbo[4];
		rightLegModel = new ModelRendererTurbo[4];

		initheadModel_1();
		initbodyModel_1();
		initleftArmModel_1();
		initrightArmModel_1();
		initleftLegModel_1();
		initrightLegModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 17, 9, textureX, textureY); // Box 22
		headModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 28
		headModel[2] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 51
		headModel[3] = new ModelRendererTurbo(this, 81, 9, textureX, textureY); // Box 23
		headModel[4] = new ModelRendererTurbo(this, 73, 17, textureX, textureY); // Box 24
		headModel[5] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 25
		headModel[6] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 29
		headModel[7] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 30
		headModel[8] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Box 40
		headModel[9] = new ModelRendererTurbo(this, 73, 9, textureX, textureY); // Box 41

		headModel[0].addShapeBox(-4F, -9F, -4F, 8, 9, 8, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.3F, -0.1F, 0F, -0.3F, -0.1F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F); // Box 22
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(-0.5F, -5F, -4F, 1, 1, 1, 0F,-0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, 0F, 0.8F, 1.2F, 0F, 0.8F, 1.2F, 0F, 0.8F, 0F, 0F, 0.8F, 0F); // Box 28
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(-0.5F, -3F, -4F, 1, 1, 1, 0F,0F, 0.2F, 1.2F, 0F, 0.2F, 1.2F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, -0.1F, -0.7F, 1F, -0.1F, -0.7F, 1F, -0.1F, -0.9F, 0F, -0.1F, -0.9F, 0F); // Box 51
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(-4F, -9.2F, 0F, 8, 3, 4, 0F,0.3F, 0.2F, 1F, 0.3F, 0.2F, 1F, 0.3F, 0.2F, 0.2F, 0.3F, 0.2F, 0.2F, 0.5F, 0F, 1.2F, 0.5F, 0F, 1.2F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F); // Box 23
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-4F, -6.2F, 0F, 8, 4, 4, 0F,0.5F, 0F, 1.2F, 0.5F, 0F, 1.2F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F, 0.3F, 0.2F, -1.8F, 0.3F, 0.2F, -1.8F, 0.3F, 0.2F, 0.2F, 0.3F, 0.2F, 0.2F); // Box 24
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-4F, -9.2F, -4F, 8, 1, 4, 0F,-1F, 0.2F, 0.2F, -1F, 0.2F, 0.2F, 0.2F, 0.2F, -1F, 0.2F, 0.2F, -1F, -1.8F, -0.6F, 0.2F, -1.8F, -0.6F, 0.2F, 0.2F, 0F, -1F, 0.2F, 0F, -1F); // Box 25
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(1F, -5.8F, -4.2F, 2, 1, 1, 0F,0.1F, -0.2F, 0F, -0.8F, 0F, 0F, -0.8F, 0F, 0F, 0.1F, -0.2F, 0F, 0.2F, -0.4F, 0F, -0.8F, -0.5F, 0F, 0.2F, -0.5F, 0F, 0.2F, -0.4F, 0F); // Box 29
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(2.5F, -5.8F, -4.2F, 1, 1, 1, 0F,0.3F, 0F, 0F, -0.4F, -0.3F, 0F, -0.4F, -0.3F, 0F, 0.3F, 0F, 0F, 0.5F, -0.5F, 0F, -0.2F, -0.1F, 0F, -0.2F, -0.1F, 0F, 0.5F, -0.5F, 0F); // Box 30
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(-3F, -5.8F, -4.2F, 2, 1, 1, 0F,-0.8F, 0F, 0F, 0.1F, -0.2F, 0F, 0.1F, -0.2F, 0F, -0.8F, 0F, 0F, -0.8F, -0.5F, 0F, 0.2F, -0.4F, 0F, 0.2F, -0.4F, 0F, 0.2F, -0.5F, 0F); // Box 40
		headModel[8].setRotationPoint(0F, 0F, 0F);

		headModel[9].addShapeBox(-3.5F, -5.8F, -4.2F, 1, 1, 1, 0F,-0.4F, -0.3F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, -0.4F, -0.3F, 0F, -0.2F, -0.1F, 0F, 0.5F, -0.5F, 0F, 0.5F, -0.5F, 0F, -0.2F, -0.1F, 0F); // Box 41
		headModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 15
		bodyModel[1] = new ModelRendererTurbo(this, 105, 17, textureX, textureY); // Box 44
		bodyModel[2] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 29
		bodyModel[3] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 30
		bodyModel[4] = new ModelRendererTurbo(this, 97, 17, textureX, textureY); // Box 31
		bodyModel[5] = new ModelRendererTurbo(this, 121, 17, textureX, textureY); // Box 32
		bodyModel[6] = new ModelRendererTurbo(this, 97, 25, textureX, textureY); // Box 33
		bodyModel[7] = new ModelRendererTurbo(this, 89, 33, textureX, textureY); // Box 574
		bodyModel[8] = new ModelRendererTurbo(this, 41, 41, textureX, textureY); // Box 576
		bodyModel[9] = new ModelRendererTurbo(this, 65, 49, textureX, textureY); // Box 41

		bodyModel[0].addShapeBox(-4F, 3.5F, -2F, 2, 8, 4, 0F,1F, 0F, 0.8F, 2F, 0F, 0.8F, 2F, 0F, 0.8F, 1F, 0F, 0.8F, 0.5F, -0.5F, 0.6F, 2F, -0.2F, 0.6F, 2F, 0F, 0.6F, 0.5F, -0.5F, 0.6F); // Box 15
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(2F, 3.5F, -2F, 2, 8, 4, 0F,2F, 0F, 0.8F, 1F, 0F, 0.8F, 1F, 0F, 0.8F, 2F, 0F, 0.8F, 2F, -0.2F, 0.6F, 0.5F, -0.5F, 0.6F, 0.5F, -0.5F, 0.6F, 2F, 0F, 0.6F); // Box 44
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(2F, -0.5F, -2F, 2, 4, 4, 0F,-1F, 0.5F, 0.8F, 1F, 0.2F, 0.8F, 1F, 0.2F, 0.8F, 2F, 0.5F, 0.8F, 2F, 0F, 0.8F, 1F, 0F, 0.8F, 1F, 0F, 0.8F, 2F, 0F, 0.8F); // Box 29
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(-4F, -0.5F, -2F, 2, 4, 4, 0F,1F, 0.2F, 0.8F, -1F, 0.5F, 0.8F, 2F, 0.5F, 0.8F, 1F, 0.2F, 0.8F, 1F, 0F, 0.8F, 2F, 0F, 0.8F, 2F, 0F, 0.8F, 1F, 0F, 0.8F); // Box 30
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-0.5F, 3F, -3F, 1, 2, 1, 0F,-0.1F, 0F, 0F, -0.1F, 0F, 0F, -0.1F, 0F, 0F, -0.1F, 0F, 0F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, -0.5F, 0F, 0F, -0.5F, 0F); // Box 31
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(2F, 1.5F, -2.9F, 2, 1, 1, 0F,-0.9F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 32
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(2F, 2.5F, -2.9F, 2, 1, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F); // Box 33
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-4F, 9F, -2F, 4, 3, 4, 0F,0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F); // Box 574
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(0F, 9F, -2F, 4, 3, 4, 0F,0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F); // Box 576
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-4F, 0F, -2F, 8, 5, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F); // Box 41
		bodyModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 57, 9, textureX, textureY); // Box 35
		leftArmModel[1] = new ModelRendererTurbo(this, 9, 33, textureX, textureY); // Box 34
		leftArmModel[2] = new ModelRendererTurbo(this, 89, 57, textureX, textureY); // Box 45

		leftArmModel[0].addShapeBox(-1F, 5F, -2F, 4, 5, 4, 0F,0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F); // Box 35
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(2.5F, -6F, -12.5F, 1, 13, 25, 0F,0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F, 0F, -5.8F, -11F); // Box 34
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);

		leftArmModel[2].addShapeBox(-1F, -2F, -2F, 4, 8, 4, 0F,0.3F, 0.7F, 0.3F, 0.3F, 0.6F, 0.3F, 0.3F, 0.6F, 0.3F, 0.3F, 0.7F, 0.3F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F); // Box 45
		leftArmModel[2].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 61
		rightArmModel[1] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 34

		rightArmModel[0].addShapeBox(-3F, -2F, -2F, 4, 8, 4, 0F,0.3F, 0.6F, 0.3F, 0.3F, 0.7F, 0.3F, 0.3F, 0.7F, 0.3F, 0.3F, 0.6F, 0.3F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F, 0.5F, 0F, 0.5F); // Box 61
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-3F, 5F, -2F, 4, 5, 4, 0F,0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F); // Box 34
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 27
		leftLegModel[1] = new ModelRendererTurbo(this, 81, 1, textureX, textureY); // Box 28
		leftLegModel[2] = new ModelRendererTurbo(this, 81, 41, textureX, textureY); // Box 594
		leftLegModel[3] = new ModelRendererTurbo(this, 105, 41, textureX, textureY); // Box 595

		leftLegModel[0].addShapeBox(-2F, 10F, -4F, 4, 2, 2, 0F,0.1F, -0.9F, 0.1F, 0.1F, -0.9F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F); // Box 27
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 10F, -2F, 4, 2, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F); // Box 28
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 594
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(-2F, 3.4F, -2F, 4, 7, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 595
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 18
		rightLegModel[1] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 19
		rightLegModel[2] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Box 572
		rightLegModel[3] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 573

		rightLegModel[0].addShapeBox(-2F, 10F, -2F, 4, 2, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F); // Box 18
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 10F, -4F, 4, 2, 2, 0F,0.1F, -0.9F, 0.1F, 0.1F, -0.9F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F); // Box 19
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 572
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-2F, 3.4F, -2F, 4, 7, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 573
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);
	}
}