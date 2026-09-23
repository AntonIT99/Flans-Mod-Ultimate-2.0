//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: ProximityMine
// Model Creator:
// Created on:21.06.2020 - 21:30:47
// Last changed on: 21.06.2020 - 21:30:47

package com.flansmod.client.model.mw;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelProximityMine extends ModelBase
{
	int textureX = 32;
	int textureY = 16;

	public ModelProximityMine()
	{
		proximitymineModel = new ModelRendererTurbo[3];
		proximitymineModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		proximitymineModel[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		proximitymineModel[2] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Import 

		proximitymineModel[0].addBox(-1F, 2F, -4F, 4, 4, 9, 0F); // Import 
		proximitymineModel[0].setRotationPoint(-1F, 0F, -0.5F);

		proximitymineModel[1].addBox(0F, -2F, -4F, 1, 4, 1, 0F); // Import 
		proximitymineModel[1].setRotationPoint(-0.5F, 0F, 0F);

		proximitymineModel[2].addBox(0F, -2F, -4F, 1, 4, 1, 0F); // Import 
		proximitymineModel[2].setRotationPoint(-0.5F, 0F, 7F);


	}


	public ModelRendererTurbo proximitymineModel[];
}
