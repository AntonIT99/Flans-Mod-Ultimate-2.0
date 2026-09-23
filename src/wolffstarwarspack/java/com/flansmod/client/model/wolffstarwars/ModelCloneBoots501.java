//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: GermanAssaultChest
// Model Creator: 
// Created on: 07.05.2020 - 21:01:46
// Last changed on: 07.05.2020 - 21:01:46

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelCloneBoots501 extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 64;

	public ModelCloneBoots501() //Same as Filename
	{
		leftLegModel = new ModelRendererTurbo[4];
		leftLegModel[0] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Import Box52
		leftLegModel[1] = new ModelRendererTurbo(this, 25, 41, textureX, textureY); // Import Box53
		leftLegModel[2] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 68
		leftLegModel[3] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 69

		leftLegModel[0].addShapeBox(-2F, 7.1F, -2F, 4, 3, 4, 0F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.02F, 0F, 0.4F); // Import Box52
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 10.91F, -2F, 4, 1, 4, 0F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0.1F, 1.8F, 0.41F, 0.1F, 1.8F, 0.41F, 0.1F, 0.4F, 0.02F, 0.1F, 0.4F); // Import Box53
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(1.85F, 7F, -1F, 1, 1, 1, 0F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F); // Box 68
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(1.85F, 7F, -2F, 1, 1, 1, 0F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F); // Box 69
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);


		rightLegModel = new ModelRendererTurbo[4];
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Import Box43
		rightLegModel[1] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Import Box44
		rightLegModel[2] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 65
		rightLegModel[3] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 66

		rightLegModel[0].addShapeBox(-2F, 10.91F, -2F, 4, 1, 4, 0F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0.1F, 1.8F, 0.02F, 0.1F, 1.8F, 0.02F, 0.1F, 0.4F, 0.41F, 0.1F, 0.4F); // Import Box43
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 7.1F, -2F, 4, 3, 4, 0F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.41F, 0F, 0.4F); // Import Box44
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-2.85F, 7F, -2F, 1, 1, 1, 0F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F); // Box 65
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-2.85F, 7F, -1F, 1, 1, 1, 0F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 0.6F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F, -0.4F, 2F, -0.2F); // Box 66
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);


	}
}