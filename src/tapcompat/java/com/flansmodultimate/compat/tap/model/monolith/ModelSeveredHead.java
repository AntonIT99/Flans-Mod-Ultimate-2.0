//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2024 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: SeveredHead
// Model Creator:
// Created on:18.06.2024 - 20:36:17
// Last changed on: 18.06.2024 - 20:36:17

package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelSeveredHead extends ModelBase
{
	int textureX = 64;
	int textureY = 32;

	public ModelSeveredHead()
	{
		severedheadModel = new ModelRendererTurbo[7];
		severedheadModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 0
		severedheadModel[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 2
		severedheadModel[2] = new ModelRendererTurbo(this, 41, 9, textureX, textureY); // Box 3
		severedheadModel[3] = new ModelRendererTurbo(this, 33, 17, textureX, textureY); // Box 4
		severedheadModel[4] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 5
		severedheadModel[5] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 6
		severedheadModel[6] = new ModelRendererTurbo(this, 17, 25, textureX, textureY); // Box 7

		severedheadModel[0].addBox(-4F, -4F, -4F, 8, 8, 8, 0F); // Box 0
		severedheadModel[0].setRotationPoint(0F, 0F, 0F);

		severedheadModel[1].addBox(-0.5F, -2F, -4.2F, 1, 1, 1, 0F); // Box 2
		severedheadModel[1].setRotationPoint(0F, 0F, 0F);

		severedheadModel[2].addShapeBox(-1.5F, -3F, 4F, 3, 1, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 1.5F, 0F, -1F, 1F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, -1.5F, 0F, -1F, -1F, -1F); // Box 3
		severedheadModel[2].setRotationPoint(0F, 0F, 0F);

		severedheadModel[3].addShapeBox(-0.5F, -1F, 4F, 2, 1, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0.5F, 0F, -1F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1.5F, 0.5F, 0F, 1F, -1F); // Box 4
		severedheadModel[3].setRotationPoint(0F, 0F, 0F);

		severedheadModel[4].addShapeBox(0.5F, -3F, 4F, 1, 3, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, -1F, -1F, -1F, -1F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 1.5F, 0.5F, 0F, -2F, 0.5F, 0F); // Box 5
		severedheadModel[4].setRotationPoint(0F, 0F, 0F);

		severedheadModel[5].addShapeBox(-2.5F, -3F, 4F, 1, 2, 3, 0F, -1F, 0F, 0F, 1F, 0F, 0F, 0F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -1F, 0F, 0F, 1F, 0F, 0F, 0F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F); // Box 6
		severedheadModel[5].setRotationPoint(0F, 0F, 0F);

		severedheadModel[6].addShapeBox(-1.5F, -1F, 4F, 2, 1, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, -0.5F, -1F, 2F, -1.5F, 1.5F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0.5F, -1F, 2F, 1.5F, 1.5F); // Box 7
		severedheadModel[6].setRotationPoint(0F, 0F, 0F);


	}


	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo severedheadModel[];
}