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

public class ModelStormtrooperPants extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 64;

	public ModelStormtrooperPants() //Same as Filename
	{
		leftLegModel = new ModelRendererTurbo[3];
		leftLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Import Box39
		leftLegModel[1] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Import Box48
		leftLegModel[2] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Box 1

		leftLegModel[0].addShapeBox(-2F, -0.0899999999999999F, -2F, 4, 12, 4, 0F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F); // Import Box39
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 2.51F, -2F, 4, 2, 4, 0F, 0.02F, 1F, 0.4F, 0.41F, 1F, 0.4F, 0.41F, 1F, 0.4F, 0.02F, 1F, 0.4F, 0.02F, 0.5F, 0.4F, 0.41F, 0.5F, 0.4F, 0.41F, 1.2F, 0.4F, 0.02F, 1.2F, 0.4F); // Import Box48
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-2F, -0.5F, -2F, 4, 2, 4, 0F, -2.98F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, -2.98F, -0.3F, 0.4F, -0.98F, 0F, 0.4F, 0.41F, 0F, 0.4F, 0.41F, 0F, 0.4F, -0.98F, 0F, 0.4F); // Box 1
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);


		rightLegModel = new ModelRendererTurbo[8];
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Box38
		rightLegModel[1] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Import Box42
		rightLegModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Import Box45
		rightLegModel[3] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Box 0
		rightLegModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 3
		rightLegModel[5] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 4
		rightLegModel[6] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 6
		rightLegModel[7] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 7

		rightLegModel[0].addShapeBox(-2F, -0.0899999999999999F, -2F, 4, 12, 4, 0F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F); // Import Box38
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 2.51F, -2F, 4, 2, 4, 0F, 0.41F, 1F, 0.4F, 0.02F, 1F, 0.4F, 0.02F, 1F, 0.4F, 0.41F, 1F, 0.4F, 0.41F, 1.2F, 0.4F, 0.02F, 1.2F, 0.4F, 0.02F, 1.2F, 0.4F, 0.41F, 1.2F, 0.4F); // Import Box42
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-0.1F, 4.5F, -1.8F, 1, 1, 4, 0F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F); // Import Box45
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-2F, -0.5F, -2F, 4, 2, 4, 0F, 0.41F, -0.3F, 0.4F, -2.98F, -0.3F, 0.4F, -2.98F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0F, 0.4F, -0.98F, 0F, 0.4F, -0.98F, 0F, 0.4F, 0.41F, 0F, 0.4F); // Box 0
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);

		rightLegModel[4].addShapeBox(1F, 4.5F, -1.8F, 1, 1, 4, 0F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F); // Box 3
		rightLegModel[4].setRotationPoint(0F, 0F, 0F);

		rightLegModel[5].addShapeBox(-1.2F, 4.5F, -1.8F, 1, 1, 4, 0F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F); // Box 4
		rightLegModel[5].setRotationPoint(0F, 0F, 0F);

		rightLegModel[6].addShapeBox(-2.3F, 4.5F, -1.8F, 1, 1, 4, 0F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -3.6F, -0.1F, 0.3F, -3.6F); // Box 6
		rightLegModel[6].setRotationPoint(0F, 0F, 0F);

		rightLegModel[7].addShapeBox(-2.9F, 4.5F, -1F, 1, 1, 4, 0F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -4.2F, -0.1F, 0.3F, -4.2F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, 1F, -0.1F, 0.3F, -4.2F, -0.1F, 0.3F, -4.2F); // Box 7
		rightLegModel[7].setRotationPoint(0F, 0F, 0F);


	}
}