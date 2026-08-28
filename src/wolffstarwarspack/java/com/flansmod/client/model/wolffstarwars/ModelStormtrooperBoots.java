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

public class ModelStormtrooperBoots extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 64;

	public ModelStormtrooperBoots() //Same as Filename
	{
		leftLegModel = new ModelRendererTurbo[5];
		leftLegModel[0] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Import Box50
		leftLegModel[1] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Import Box51
		leftLegModel[2] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Import Box52
		leftLegModel[3] = new ModelRendererTurbo(this, 25, 41, textureX, textureY); // Import Box53
		leftLegModel[4] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Box 2

		leftLegModel[0].addShapeBox(-0.5F, 5.91F, -2F, 1, 1, 4, 0F, 0.01F, 0.7F, 0.5F, 0.41F, 0.7F, 0.5F, 0.41F, -0.5F, 0.4F, 0.01F, -0.5F, 0.4F, 0.01F, 0.5F, 0.5F, 0.41F, 0.5F, 0.5F, 0.41F, 0.5F, 0.4F, 0.01F, 0.5F, 0.4F); // Import Box50
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 5.91F, -2F, 1, 1, 4, 0F, 0.02F, -0.2F, 0.5F, 0.5F, 0.7F, 0.5F, 0.5F, -0.5F, 0.4F, 0.02F, -0.5F, 0.4F, 0.02F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.4F, 0.02F, 0.5F, 0.4F); // Import Box51
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-2F, 7.1F, -2F, 4, 3, 4, 0F, 0.02F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.02F, 0F, 0.4F); // Import Box52
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(-2F, 10.91F, -2F, 4, 1, 4, 0F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0.1F, 1.8F, 0.41F, 0.1F, 1.8F, 0.41F, 0.1F, 0.4F, 0.02F, 0.1F, 0.4F); // Import Box53
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);

		leftLegModel[4].addShapeBox(1.4F, 5.91F, -2F, 1, 1, 4, 0F, 0.5F, 0.7F, 0.5F, 0.02F, -0.2F, 0.5F, 0.02F, -0.5F, 0.4F, 0.5F, -0.5F, 0.4F, 0.5F, 0.5F, 0.5F, 0.02F, 0.5F, 0.5F, 0.02F, 0.5F, 0.4F, 0.5F, 0.5F, 0.4F); // Box 2
		leftLegModel[4].setRotationPoint(0F, 0F, 0F);


		rightLegModel = new ModelRendererTurbo[2];
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Import Box43
		rightLegModel[1] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Import Box44

		rightLegModel[0].addShapeBox(-2F, 10.91F, -2F, 4, 1, 4, 0F, 0.41F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.02F, 0.8F, 0.4F, 0.41F, 0.8F, 0.4F, 0.41F, 0.1F, 1.8F, 0.02F, 0.1F, 1.8F, 0.02F, 0.1F, 0.4F, 0.41F, 0.1F, 0.4F); // Import Box43
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 7.1F, -2F, 4, 3, 4, 0F, 0.41F, 1F, 0.4F, 0.02F, 1F, 0.4F, 0.02F, 0.7F, 0.4F, 0.41F, 0.7F, 0.4F, 0.41F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.02F, 0F, 0.4F, 0.41F, 0F, 0.4F); // Import Box44
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);


	}
}