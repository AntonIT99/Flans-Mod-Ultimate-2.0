//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2019 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Eyes
// Model Creator: 
// Created on: 23.12.2019 - 19:08:18
// Last changed on: 23.12.2019 - 19:08:18

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelSithHood extends ModelCustomArmour //Same as Filename
{
	int textureX = 32;
	int textureY = 32;

	public ModelSithHood() //Same as Filename
	{
		headModel = new ModelRendererTurbo[5];
		headModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY); // Import Box0
		headModel[1] = new ModelRendererTurbo(this, 0, 3, textureX, textureY); // Import Box1
		headModel[2] = new ModelRendererTurbo(this, 5, 0, textureX, textureY); // Import Box2
		headModel[3] = new ModelRendererTurbo(this, 5, 3, textureX, textureY); // Import Box3
		headModel[4] = new ModelRendererTurbo(this, 0, 16, textureX, textureY); // Box 0

		headModel[0].addShapeBox(1F, -4F, -4F, 1, 1, 1, 0F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F); // Import Box0
		headModel[0].setRotationPoint(0F, 0F, 0F);
		headModel[0].glow = true;

		headModel[1].addShapeBox(-2F, -4F, -4F, 1, 1, 1, 0F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, 0.02F, 0F, 0F, 0.02F, 0F, 0F, -0.9F, 0F, 0F, -0.9F); // Import Box1
		headModel[1].setRotationPoint(0F, 0F, 0F);
		headModel[1].glow = true;

		headModel[2].addShapeBox(1F, -4F, -4F, 1, 1, 1, 0F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.9F); // Import Box2
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(-2F, -4F, -4F, 1, 1, 1, 0F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, 0.03F, -0.25F, -0.25F, -0.91F, -0.25F, -0.25F, -0.9F); // Import Box3
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-4F, -8F, -4F, 8, 8, 8, 0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F); // Box 0
		headModel[4].setRotationPoint(0F, 0F, 0F);


	}
}