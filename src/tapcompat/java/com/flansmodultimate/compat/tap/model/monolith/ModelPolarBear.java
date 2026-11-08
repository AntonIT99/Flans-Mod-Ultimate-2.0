//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: PolarBear
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelPolarBear extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelPolarBear() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[11];
		leftArmModel = new ModelRendererTurbo[2];
		rightArmModel = new ModelRendererTurbo[2];
		leftLegModel = new ModelRendererTurbo[2];
		rightLegModel = new ModelRendererTurbo[2];

		initbodyModel_1();
		initleftArmModel_1();
		initrightArmModel_1();
		initleftLegModel_1();
		initrightLegModel_1();
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 73, 17, textureX, textureY); // Box 60
		bodyModel[1] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 61
		bodyModel[2] = new ModelRendererTurbo(this, 49, 41, textureX, textureY); // Box 62
		bodyModel[3] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 64
		bodyModel[4] = new ModelRendererTurbo(this, 81, 1, textureX, textureY); // Box 65
		bodyModel[5] = new ModelRendererTurbo(this, 49, 57, textureX, textureY); // Box 66
		bodyModel[6] = new ModelRendererTurbo(this, 97, 49, textureX, textureY); // Box 67
		bodyModel[7] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 68
		bodyModel[8] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 69
		bodyModel[9] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 70
		bodyModel[10] = new ModelRendererTurbo(this, 41, 97, textureX, textureY); // Box 77

		bodyModel[0].addShapeBox(-9F, 0F, -5F, 18, 11, 6, 0F,-2F, 0F, 1F, -2F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 60
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(-9F, 0F, 1F, 18, 11, 7, 0F,0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F); // Box 61
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(-9F, 11F, -5F, 18, 5, 6, 0F,-2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -8F, -1F, -3F, -8F, -1F, -3F, -6F, 0F, 0F, -6F, 0F, 0F); // Box 62
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(-9F, 11F, 1F, 18, 5, 6, 0F,0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 1F, -2F, 0F, 1F, -6F, 0F, 0F, -6F, 0F, 0F, -8F, -1F, -3F, -8F, -1F, -3F); // Box 64
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-7F, -9F, -4.5F, 14, 9, 5, 0F,-2F, 0F, 1F, -2F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 1F, -2F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 65
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(-7F, -9F, 0.5F, 14, 9, 7, 0F,0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, -1F, -2F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F); // Box 66
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(-3.5F, -16F, -5F, 7, 7, 8, 0F,0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, -1F, 0.5F, 0F, -1F, 1F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 0F, 1F, 1F, 0F); // Box 67
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-5F, -17F, -3F, 2, 2, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 68
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(3F, -17F, -3F, 2, 2, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 69
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-2.5F, -12F, -8F, 5, 3, 3, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 70
		bodyModel[9].setRotationPoint(0F, 0F, 0F);

		bodyModel[10].addShapeBox(-2F, 9F, 6F, 4, 4, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 77
		bodyModel[10].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 25, 81, textureX, textureY); // Box 73
		leftArmModel[1] = new ModelRendererTurbo(this, 57, 81, textureX, textureY); // Box 74

		leftArmModel[0].addShapeBox(0F, -8F, -4.5F, 6, 6, 6, 0F,1F, 0F, -1F, -2F, 0F, -1F, -2F, 0F, 0F, 1F, 0F, 0F, 0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0.5F, 0F, 1F); // Box 73
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(0F, -2F, -4.5F, 6, 10, 6, 0F,0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0.5F, 0F, 1F, -1F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, -1F, 0F, 0F); // Box 74
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 1, 65, textureX, textureY); // Box 71
		rightArmModel[1] = new ModelRendererTurbo(this, 97, 65, textureX, textureY); // Box 72

		rightArmModel[0].addShapeBox(-6F, -2F, -4.5F, 6, 10, 6, 0F,0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, -1F, -1F, 0F, -1F, -1F, 0F, 0F, 0F, 0F, 0F); // Box 71
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-6F, -8F, -4.5F, 6, 6, 6, 0F,-2F, 0F, -1F, 1F, 0F, -1F, 1F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 1F, 0F, 0F, 1F); // Box 72
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 58
		leftLegModel[1] = new ModelRendererTurbo(this, 1, 97, textureX, textureY); // Box 76

		leftLegModel[0].addShapeBox(-2F, -1F, -4F, 8, 13, 8, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, -2F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, -2F, 0F, -1F); // Box 58
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, -5F, -4F, 8, 4, 8, 0F,-1F, 0F, -1F, -2F, 0F, -1F, -2F, 0F, -1F, -1F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F); // Box 76
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 59
		rightLegModel[1] = new ModelRendererTurbo(this, 89, 81, textureX, textureY); // Box 75

		rightLegModel[0].addShapeBox(-6F, -1F, -4F, 8, 13, 8, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, -1F, -2F, 0F, -1F, -2F, 0F, -1F, 0F, 0F, -1F); // Box 59
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-6F, -5F, -4F, 8, 4, 8, 0F,-2F, 0F, -1F, -1F, 0F, -1F, -1F, 0F, -1F, -2F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F); // Box 75
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);
	}
}