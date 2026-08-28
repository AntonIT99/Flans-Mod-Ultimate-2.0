//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2019 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: 
// Model Creator: 
// Created on: 23.12.2019 - 18:08:15
// Last changed on: 23.12.2019 - 18:08:15

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelEyes extends ModelCustomArmour //Same as Filename
{
	int textureX = 32;
	int textureY = 32;

	public ModelEyes() //Same as Filename
	{
		headModel = new ModelRendererTurbo[4];
		headModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY); // Box 0
		headModel[1] = new ModelRendererTurbo(this, 0, 3, textureX, textureY); // Box 1
		headModel[2] = new ModelRendererTurbo(this, 5, 0, textureX, textureY); // Box 2
		headModel[3] = new ModelRendererTurbo(this, 5, 3, textureX, textureY); // Box 3

		headModel[0].addShapeBox(1F, -4F, -4F, 1, 1, 1, 0F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F); // Box 0
		headModel[0].setRotationPoint(0F, 0F, 0F);
		headModel[0].glow = true;

		headModel[1].addShapeBox(-2F, -4F, -4F, 1, 1, 1, 0F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F); // Box 1
		headModel[1].setRotationPoint(0F, 0F, 0F);
		headModel[1].glow = true;

		headModel[2].addShapeBox(1F, -4F, -4F, 1, 1, 1, 0F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.9F); // Box 2
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(-2F, -4F, -4F, 1, 1, 1, 0F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.9F); // Box 3
		headModel[3].setRotationPoint(0F, 0F, 0F);


	}
}