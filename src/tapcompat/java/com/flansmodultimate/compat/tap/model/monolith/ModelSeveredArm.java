//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2024 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: SeveredArm
// Model Creator:
// Created on:18.06.2024 - 20:11:13
// Last changed on: 18.06.2024 - 20:11:13

package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelSeveredArm extends ModelBase
{
	int textureX = 64;
	int textureY = 32;

	public ModelSeveredArm()
	{
		severedarmModel = new ModelRendererTurbo[8];
		severedarmModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Box3
		severedarmModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 0
		severedarmModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 1
		severedarmModel[3] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 2
		severedarmModel[4] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Box 3
		severedarmModel[5] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 4
		severedarmModel[6] = new ModelRendererTurbo(this, 25, 17, textureX, textureY); // Box 5
		severedarmModel[7] = new ModelRendererTurbo(this, 33, 17, textureX, textureY); // Box 6

		severedarmModel[0].addShapeBox(-12F, -2F, -2F, 10, 4, 4, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Import Box3
		severedarmModel[0].setRotationPoint(0F, 0F, 0F);

		severedarmModel[1].addShapeBox(-2F, -2F, -2F, 4, 2, 2, 0F, 0F, 0F, 0F, 0F, 0.5F, 0.2F, -1.5F, 0F, -1F, 0F, 0F, 1F, 0F, 1F, 0F, -1F, 0F, 0F, -2F, -1F, -1F, 0F, 0F, 0F); // Box 0
		severedarmModel[1].setRotationPoint(0F, 0F, 0F);

		severedarmModel[2].addShapeBox(-2F, 0F, -2F, 4, 2, 2, 0F, 0F, 1F, 0F, -1F, 0F, 0F, -2F, -1F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 1.5F, 1.5F, -1.5F, 0F, -1F, 0F, 0F, 1F); // Box 1
		severedarmModel[2].setRotationPoint(0F, 0F, 0F);

		severedarmModel[3].addShapeBox(-2F, 0F, 0F, 4, 2, 2, 0F, 0F, 0F, 0F, -1F, -1F, -1F, -1F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 1F, -1.5F, 0F, -0.2F, 0F, 0.5F, -0.5F, 0F, 0F, 0F); // Box 2
		severedarmModel[3].setRotationPoint(0F, 0F, 0F);

		severedarmModel[4].addShapeBox(-2F, -2F, 0F, 4, 2, 2, 0F, 0F, 0F, 1F, -1.5F, 0F, -1F, 1F, 0.8F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, -1F, -1F, -1F, 0F, 0F, 0F, 1F, 0F); // Box 3
		severedarmModel[4].setRotationPoint(0F, 0F, 0F);

		severedarmModel[5].addShapeBox(-3.5F, -0.5F, -0.5F, 7, 1, 1, 0F, 0F, 0F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0F, 0F); // Box 4
		severedarmModel[5].setRotationPoint(0F, 0F, 0F);

		severedarmModel[6].addShapeBox(3.5F, -0.5F, -0.5F, 1, 1, 1, 0F, 0.5F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0.5F, 0F, 0F, 0F, 0F, 0F, 0.4F, -1F, 0F, 0.4F, -1F, 0F, 0F, 0F, 0F); // Box 5
		severedarmModel[6].setRotationPoint(0F, 0F, 0F);

		severedarmModel[7].addShapeBox(3.5F, -0.5F, -0.5F, 1, 1, 1, 0F, 0F, 0F, 0F, 0.4F, -1F, 0F, 0.4F, -1F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0.5F, 0F, 0F); // Box 6
		severedarmModel[7].setRotationPoint(0F, 0F, 0F);


	}


	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo severedarmModel[];
}