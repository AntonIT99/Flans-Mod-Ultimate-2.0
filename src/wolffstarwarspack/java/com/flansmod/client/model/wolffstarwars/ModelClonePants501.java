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

public class ModelClonePants501 extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 64;

	public ModelClonePants501() //Same as Filename
	{
		leftLegModel = new ModelRendererTurbo[10];
		leftLegModel[0] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Import Box39
		leftLegModel[1] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Import Box48
		leftLegModel[2] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Import Box49
		leftLegModel[3] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Import Box50
		leftLegModel[4] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Import Box51
		leftLegModel[5] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 60
		leftLegModel[6] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 61
		leftLegModel[7] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 62
		leftLegModel[8] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 67
		leftLegModel[9] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 70

		leftLegModel[0].addShapeBox(-2F, -0.0899999999999999F, -2F, 4, 12, 4, 0F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F); // Import Box39
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 1.91F, -2F, 4, 2, 4, 0F, 0.02F, 0.4F, 0.4F, 0.41F, 2.4F, 0.4F, 0.41F, 2.4F, 0.4F, 0.02F, 0.4F, 0.4F, 0.02F, 0.5F, 0.4F, 0.41F, 0.5F, 0.4F, 0.41F, 1F, 0.4F, 0.02F, 1F, 0.4F); // Import Box48
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(1F, 4.91F, -2F, 1, 1, 4, 0F, 0.09F, 0.7F, 1F, 0.41F, 0F, 1F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F, 0.09F, 0.7F, 1F, 0.41F, 0F, 1F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F); // Import Box49
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(-0.5F, 4.91F, -2F, 1, 1, 4, 0F, 0.01F, 0.7F, 1F, 0.41F, 0.7F, 1F, 0.41F, -0.3F, 0.4F, 0.01F, -0.3F, 0.4F, 0.01F, 0.7F, 1F, 0.41F, 0.7F, 1F, 0.41F, -0.3F, 0.4F, 0.01F, -0.3F, 0.4F); // Import Box50
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);

		leftLegModel[4].addShapeBox(-2F, 4.91F, -2F, 1, 1, 4, 0F, 0.02F, 0F, 1F, 0.5F, 0.7F, 1F, 0.5F, -0.3F, 0.4F, 0.02F, -0.3F, 0.4F, 0.02F, 0F, 1F, 0.5F, 0.7F, 1F, 0.5F, -0.3F, 0.4F, 0.02F, -0.3F, 0.4F); // Import Box51
		leftLegModel[4].setRotationPoint(0F, 0F, 0F);

		leftLegModel[5].addShapeBox(-1.6F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, -0.7F, 1.01F, 0.09F, -1F, 1.01F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0F, 1.01F, 0.09F, 0.7F, 1.01F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Box 60
		leftLegModel[5].setRotationPoint(0F, 0F, 0F);

		leftLegModel[6].addShapeBox(-0.1F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, -1F, 1.01F, 0.01F, -1F, 1.01F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0.7F, 1.01F, 0.01F, 0.7F, 1.01F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Box 61
		leftLegModel[6].setRotationPoint(0F, 0F, 0F);

		leftLegModel[7].addShapeBox(1F, 4.91F, -2F, 1, 1, 4, 0F, 0.09F, -1F, 1.01F, 0.41F, -0.7F, 1.01F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F, 0.09F, 0.7F, 1.01F, 0.41F, 0F, 1.01F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F); // Box 62
		leftLegModel[7].setRotationPoint(0F, 0F, 0F);

		leftLegModel[8].addShapeBox(1.85F, 0F, -1F, 1, 1, 1, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.6F, -0.2F, -0.4F, 3.6F, -0.2F); // Box 67
		leftLegModel[8].setRotationPoint(0F, 0F, 0F);

		leftLegModel[9].addShapeBox(1.85F, 0F, -2F, 1, 1, 1, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 3.4F, -0.2F, -0.4F, 3.4F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.5F, -0.2F); // Box 70
		leftLegModel[9].setRotationPoint(0F, 0F, 0F);


		rightLegModel = new ModelRendererTurbo[10];
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Box38
		rightLegModel[1] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Import Box42
		rightLegModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Import Box45
		rightLegModel[3] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Import Box46
		rightLegModel[4] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Import Box47
		rightLegModel[5] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 57
		rightLegModel[6] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 58
		rightLegModel[7] = new ModelRendererTurbo(this, 43, 41, textureX, textureY); // Box 59
		rightLegModel[8] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 63
		rightLegModel[9] = new ModelRendererTurbo(this, 55, 44, textureX, textureY); // Box 64

		rightLegModel[0].addShapeBox(-2F, -0.0899999999999999F, -2F, 4, 12, 4, 0F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0.8F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F, 0.01F, 0F, 0.11F); // Import Box38
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 1.91F, -2F, 4, 2, 4, 0F, 0.41F, 2.4F, 0.4F, 0.02F, 0.4F, 0.4F, 0.02F, 0.4F, 0.4F, 0.41F, 2.4F, 0.4F, 0.41F, 0.5F, 0.4F, 0.02F, 0.5F, 0.4F, 0.02F, 1F, 0.4F, 0.41F, 1F, 0.4F); // Import Box42
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-0.5F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, 0.7F, 1F, 0.01F, 0.7F, 1F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0.7F, 1F, 0.01F, 0.7F, 1F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Import Box45
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-2F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, 0F, 1F, 0.09F, 0.7F, 1F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0F, 1F, 0.09F, 0.7F, 1F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Import Box46
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);

		rightLegModel[4].addShapeBox(1F, 4.91F, -2F, 1, 1, 4, 0F, 0.5F, 0.7F, 1F, 0.02F, 0F, 1F, 0.02F, -0.3F, 0.4F, 0.5F, -0.3F, 0.4F, 0.5F, 0.7F, 1F, 0.02F, 0F, 1F, 0.02F, -0.3F, 0.4F, 0.5F, -0.3F, 0.4F); // Import Box47
		rightLegModel[4].setRotationPoint(0F, 0F, 0F);

		rightLegModel[5].addShapeBox(-2F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, -0.7F, 1.01F, 0.09F, -1F, 1.01F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0F, 1.01F, 0.09F, 0.7F, 1.01F, 0.09F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Box 57
		rightLegModel[5].setRotationPoint(0F, 0F, 0F);

		rightLegModel[6].addShapeBox(0.6F, 4.91F, -2F, 1, 1, 4, 0F, 0.09F, -1F, 1.01F, 0.41F, -0.7F, 1.01F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F, 0.09F, 0.7F, 1.01F, 0.41F, 0F, 1.01F, 0.41F, -0.3F, 0.4F, 0.09F, -0.3F, 0.4F); // Box 58
		rightLegModel[6].setRotationPoint(0F, 0F, 0F);

		rightLegModel[7].addShapeBox(-0.5F, 4.91F, -2F, 1, 1, 4, 0F, 0.41F, -1F, 1.01F, 0.01F, -1F, 1.01F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F, 0.41F, 0.7F, 1.01F, 0.01F, 0.7F, 1.01F, 0.01F, -0.3F, 0.4F, 0.41F, -0.3F, 0.4F); // Box 59
		rightLegModel[7].setRotationPoint(0F, 0F, 0F);

		rightLegModel[8].addShapeBox(-2.85F, 0F, -2F, 1, 1, 1, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 3.4F, -0.2F, -0.4F, 3.4F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.5F, -0.2F); // Box 63
		rightLegModel[8].setRotationPoint(0F, 0F, 0F);

		rightLegModel[9].addShapeBox(-2.85F, 0F, -1F, 1, 1, 1, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.5F, -0.2F, -0.4F, 3.6F, -0.2F, -0.4F, 3.6F, -0.2F); // Box 64
		rightLegModel[9].setRotationPoint(0F, 0F, 0F);


	}
}