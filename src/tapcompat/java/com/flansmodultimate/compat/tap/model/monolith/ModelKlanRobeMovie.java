//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2025 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: KlanRobeMovie
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelKlanRobeMovie extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 128;

	public ModelKlanRobeMovie() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[10];
		leftArmModel = new ModelRendererTurbo[10];
		rightArmModel = new ModelRendererTurbo[10];
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
		bodyModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 44
		bodyModel[1] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 38
		bodyModel[2] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 113
		bodyModel[3] = new ModelRendererTurbo(this, 33, 33, textureX, textureY); // Box 118
		bodyModel[4] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 189
		bodyModel[5] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 52
		bodyModel[6] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 53
		bodyModel[7] = new ModelRendererTurbo(this, 33, 33, textureX, textureY); // Box 54
		bodyModel[8] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 55
		bodyModel[9] = new ModelRendererTurbo(this, 0, 105, textureX, textureY); // Box 33

		bodyModel[0].addShapeBox(-4F, 11F, -2.5F, 8, 1, 5, 0F,0.9F, -0.1F, 0.3F, 0.9F, -0.1F, 0.3F, 0.9F, -0.1F, 0.3F, 0.9F, -0.1F, 0.3F, 0.9F, -0.3F, 0.3F, 0.9F, -0.3F, 0.3F, 0.9F, -0.3F, 0.3F, 0.9F, -0.3F, 0.3F); // Box 44
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(0F, 5F, -2.5F, 4, 5, 5, 0F,0F, 0F, 0.4F, 0.5F, 0F, 0.4F, 0.5F, 0F, 0.4F, 0F, 0F, 0.4F, 0F, 1.1F, 0.2F, 0.8F, 1.1F, 0.2F, 0.8F, 1.1F, 0.2F, 0F, 1.1F, 0.2F); // Box 38
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(0F, 1.5F, -2.7F, 4, 3, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0F, 0F, 0.5F, 0F); // Box 113
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(0F, 1.5F, 1.7F, 4, 3, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0.7F, 0.5F, 0F, 0.7F, 0.5F, 0.2F, 0F, 0.5F, 0.2F); // Box 118
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(2F, 0F, -2.5F, 2, 1, 5, 0F,0.5F, 0.8F, 0F, -0.5F, 0.5F, 0F, -0.5F, 0.7F, 0F, 2F, 0.8F, 0F, 1.8F, 0.5F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0.5F, 0.2F, 2F, 0.5F, 0.2F); // Box 189
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(-4F, 5F, -2.5F, 4, 5, 5, 0F,0.5F, 0F, 0.4F, 0F, 0F, 0.4F, 0F, 0F, 0.4F, 0.5F, 0F, 0.4F, 0.8F, 1.1F, 0.2F, 0F, 1.1F, 0.2F, 0F, 1.1F, 0.2F, 0.8F, 1.1F, 0.2F); // Box 52
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(-4F, 1.5F, -2.7F, 4, 3, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0.5F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0.5F, 0F, 0.5F, 0.5F, 0F); // Box 53
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-4F, 1.5F, 1.7F, 4, 3, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.7F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0.2F, 0.7F, 0.5F, 0.2F); // Box 54
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(-4F, 0F, -2.5F, 2, 1, 5, 0F,-0.5F, 0.5F, 0F, 0.5F, 0.8F, 0F, 2F, 0.8F, 0F, -0.5F, 0.7F, 0F, 0F, 0.5F, 0.2F, 1.8F, 0.5F, 0.2F, 2F, 0.5F, 0.2F, 0F, 0.5F, 0.2F); // Box 55
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-10.5F, -4.5F, -3F, 21, 21, 1, 0F,-7F, -7.5F, 0F, -7F, -7.5F, 0F, -7F, -7.5F, 0F, -7F, -7.5F, 0F, -7F, -6.5F, 0F, -7F, -6.5F, 0F, -7F, -6.5F, 0F, -7F, -6.5F, 0F); // Box 33
		bodyModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 17, 49, textureX, textureY); // Box 141
		leftArmModel[1] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 142
		leftArmModel[2] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 113
		leftArmModel[3] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 114
		leftArmModel[4] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 116
		leftArmModel[5] = new ModelRendererTurbo(this, 9, 65, textureX, textureY); // Box 117
		leftArmModel[6] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 63
		leftArmModel[7] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 64
		leftArmModel[8] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 65
		leftArmModel[9] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 66

		leftArmModel[0].addShapeBox(-1F, 0F, -2F, 4, 4, 4, 0F,1.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F, 1F, 0F, 0.6F, 1F, 0F, 0.6F, 1F, 0F, 0.6F, 1F, 0F, 0.6F); // Box 141
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(2F, 4F, -2F, 1, 6, 2, 0F,-1.1F, 0F, 0.6F, 1F, 0F, 0.6F, 1F, 0F, 0F, -1.1F, 0F, 0F, -2.5F, -0.5F, 1.5F, 2F, -0.5F, 1.5F, 2.9F, 2.5F, 0F, -3.4F, 2.5F, 0F); // Box 142
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);

		leftArmModel[2].addShapeBox(1.5F, 4F, -2F, 1, 6, 2, 0F,0F, 0F, 0.6F, 0.6F, 0F, 0.6F, 0.6F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 2.5F, 2F, -0.5F, 1.5F, 2F, -0.5F, 0F, 0F, -1F, 0F); // Box 113
		leftArmModel[2].setRotationPoint(0F, 0F, 0F);

		leftArmModel[3].addShapeBox(1.5F, 9.5F, -2F, 1, 3, 2, 0F,0F, 0.5F, 2.5F, 2F, 0F, 1.5F, 2F, 0F, -3F, 0F, 0.5F, -4F, -1.5F, -1.5F, 1F, 2.8F, 0F, -1.9F, 2.8F, 0F, 0F, -1.5F, -1.5F, -2.5F); // Box 114
		leftArmModel[3].setRotationPoint(0F, 0F, 0F);

		leftArmModel[4].addShapeBox(-0.5F, 4F, -2F, 2, 6, 2, 0F,1.5F, 0F, 0.6F, 0F, 0F, 0.6F, 0F, 0F, 0F, 1.5F, 0F, 0F, 0.5F, -1.5F, 0.7F, 0F, -1F, 2.5F, 0F, -1F, 0F, 0.5F, -1.2F, 0F); // Box 116
		leftArmModel[4].setRotationPoint(0F, 0F, 0F);

		leftArmModel[5].addShapeBox(-1F, -2F, -2F, 4, 1, 4, 0F,1F, 0.2F, 0.2F, 0F, 0.1F, 0.2F, 0F, 0.1F, 0.2F, 1F, 0.2F, 0.2F, 1.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F); // Box 117
		leftArmModel[5].setRotationPoint(0F, 0F, 0F);

		leftArmModel[6].addShapeBox(2F, 4F, 0F, 1, 6, 2, 0F,-1.1F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0.6F, -1.1F, 0F, 0.6F, -3.4F, 2.5F, 0F, 2.9F, 2.5F, 0F, 2F, -0.5F, 1.5F, -2.5F, -0.5F, 1.5F); // Box 63
		leftArmModel[6].setRotationPoint(0F, 0F, 0F);

		leftArmModel[7].addShapeBox(1.5F, 9.5F, 0F, 1, 3, 2, 0F,0F, 0.5F, -4F, 2F, 0F, -3F, 2F, 0F, 1.5F, 0F, 0.5F, 2.5F, -1.5F, -1.5F, -2F, 2.8F, 0F, 0F, 2.8F, 0F, -1.9F, -1.5F, -1.5F, 1F); // Box 64
		leftArmModel[7].setRotationPoint(0F, 0F, 0F);

		leftArmModel[8].addShapeBox(1.5F, 4F, 0F, 1, 6, 2, 0F,0F, 0F, 0F, 0.6F, 0F, 0F, 0.6F, 0F, 0.6F, 0F, 0F, 0.6F, 0F, -1F, 0F, 2F, -0.5F, 0F, 2F, -0.5F, 1.5F, 0F, -1F, 2.5F); // Box 65
		leftArmModel[8].setRotationPoint(0F, 0F, 0F);

		leftArmModel[9].addShapeBox(-0.5F, 4F, 0F, 2, 6, 2, 0F,1.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.6F, 1.5F, 0F, 0.6F, 0.5F, -1.2F, 0F, 0F, -1F, 0F, 0F, -1F, 2.5F, 0.5F, -1.5F, 0.7F); // Box 66
		leftArmModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 17, 49, textureX, textureY); // Box 69
		rightArmModel[1] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 70
		rightArmModel[2] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 71
		rightArmModel[3] = new ModelRendererTurbo(this, 9, 65, textureX, textureY); // Box 72
		rightArmModel[4] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 73
		rightArmModel[5] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 74
		rightArmModel[6] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 75
		rightArmModel[7] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 76
		rightArmModel[8] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 77
		rightArmModel[9] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 78

		rightArmModel[0].addShapeBox(-3F, 0F, -2F, 4, 4, 4, 0F,0.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F, 1F, 0F, 0.6F, 1F, 0F, 0.6F, 1F, 0F, 0.6F, 1F, 0F, 0.6F); // Box 69
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-1.5F, 4F, -2F, 2, 6, 2, 0F,0F, 0F, 0.6F, 1.5F, 0F, 0.6F, 1.5F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 2F, 0.5F, -1.5F, 0.7F, 0.5F, -1.2F, 0F, 0F, -1F, 0F); // Box 70
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);

		rightArmModel[2].addShapeBox(-1.5F, 4F, 0F, 2, 6, 2, 0F,0F, 0F, 0F, 1.5F, 0F, 0F, 1.5F, 0F, 0.6F, 0F, 0F, 0.6F, 0F, -1F, 0F, 0.5F, -1.2F, 0F, 0.5F, -1.5F, 0.7F, 0F, -1F, 2.5F); // Box 71
		rightArmModel[2].setRotationPoint(0F, 0F, 0F);

		rightArmModel[3].addShapeBox(-3F, -2F, -2F, 4, 1, 4, 0F,0F, 0.1F, 0.2F, 1F, 0.2F, 0.2F, 1F, 0.2F, 0.2F, 0F, 0.1F, 0.2F, 0.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F, 1.4F, 0.5F, 0.4F, 0.4F, 0.5F, 0.4F); // Box 72
		rightArmModel[3].setRotationPoint(0F, 0F, 0F);

		rightArmModel[4].addShapeBox(-2.5F, 4F, -2F, 1, 6, 2, 0F,0.6F, 0F, 0.6F, 0F, 0F, 0.6F, 0F, 0F, 0F, 0.6F, 0F, 0F, 2F, -0.5F, 1.5F, 0F, -1F, 2F, 0F, -1F, 0F, 2F, -0.5F, 0F); // Box 73
		rightArmModel[4].setRotationPoint(0F, 0F, 0F);

		rightArmModel[5].addShapeBox(-2.5F, 9.5F, -2F, 1, 3, 2, 0F,2F, 0F, 1.5F, 0F, 0.5F, 2F, 0F, 0.5F, -3.5F, 2F, 0F, -3F, 2.8F, 0F, -1.9F, -1.5F, -1.5F, 1F, -1.5F, -1.5F, -1.5F, 2.8F, 0F, 0F); // Box 74
		rightArmModel[5].setRotationPoint(0F, 0F, 0F);

		rightArmModel[6].addShapeBox(-2.5F, 9.5F, 0F, 1, 3, 2, 0F,2F, 0F, -3F, 0F, 0.5F, -4F, 0F, 0.5F, 2.5F, 2F, 0F, 1.5F, 2.8F, 0F, 0F, -1.5F, -1.5F, -2.5F, -1.5F, -1.5F, 1F, 2.8F, 0F, -1.9F); // Box 75
		rightArmModel[6].setRotationPoint(0F, 0F, 0F);

		rightArmModel[7].addShapeBox(-2.5F, 4F, 0F, 1, 6, 2, 0F,0.6F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.6F, 0.6F, 0F, 0.6F, 2F, -0.5F, 0F, 0F, -1F, 0F, 0F, -1F, 2.5F, 2F, -0.5F, 1.5F); // Box 76
		rightArmModel[7].setRotationPoint(0F, 0F, 0F);

		rightArmModel[8].addShapeBox(-3F, 4F, 0F, 1, 6, 2, 0F,1F, 0F, 0F, -1.1F, 0F, 0F, -1.1F, 0F, 0.6F, 1F, 0F, 0.6F, 2.9F, 2.5F, 0F, -3.4F, 2.5F, 0F, -2.5F, -0.5F, 1.5F, 2F, -0.5F, 1.5F); // Box 77
		rightArmModel[8].setRotationPoint(0F, 0F, 0F);

		rightArmModel[9].addShapeBox(-3F, 4F, -2F, 1, 6, 2, 0F,1F, 0F, 0.6F, -1.1F, 0F, 0.6F, -1.1F, 0F, 0F, 1F, 0F, 0F, 2F, -0.5F, 1.5F, -2.5F, -0.5F, 1.5F, -3.4F, 2.5F, 0F, 2.9F, 2.5F, 0F); // Box 78
		rightArmModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 67
		leftLegModel[1] = new ModelRendererTurbo(this, 25, 89, textureX, textureY); // Box 68

		leftLegModel[0].addShapeBox(-2F, -1F, -2F, 4, 12, 4, 0F,-0.1F, -0.5F, 0.7F, 0.8F, -0.5F, 0.7F, 0.8F, -0.5F, 0.7F, -0.1F, -0.5F, 0.7F, -0.3F, -0.5F, 2.2F, 2.5F, -1.5F, 1.5F, 2.5F, -1.5F, 1.5F, -0.3F, -0.5F, 2.2F); // Box 67
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, -0.5F, -2F, 4, 12, 4, 0F,0F, 0F, 0.7F, 0.8F, -0.5F, 0.7F, 0.8F, -0.5F, 0.7F, 0F, 0F, 0.7F, 0F, -0.5F, 2.2F, 2.5F, -1.5F, 1.5F, 2.5F, -1.5F, 1.5F, 0F, -0.5F, 2.2F); // Box 68
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 69
		rightLegModel[1] = new ModelRendererTurbo(this, 25, 89, textureX, textureY); // Box 51

		rightLegModel[0].addShapeBox(-2F, -1F, -2F, 4, 12, 4, 0F,0.8F, -0.5F, 0.7F, -0.1F, -0.5F, 0.7F, -0.1F, -0.5F, 0.7F, 0.8F, -0.5F, 0.7F, 2.5F, -1.5F, 1.5F, -0.3F, -0.5F, 2.2F, -0.3F, -0.5F, 2.2F, 2.5F, -1.5F, 1.5F); // Box 69
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, -0.5F, -2F, 4, 12, 4, 0F,0.8F, -0.5F, 0.7F, 0.2F, 0F, 0.7F, 0F, 0F, 0.7F, 0.8F, -0.5F, 0.7F, 2.5F, -1.5F, 1.5F, 0F, -0.5F, 2.2F, 0F, -0.5F, 2.2F, 2.5F, -1.5F, 1.5F); // Box 51
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);
	}
}