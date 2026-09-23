//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: RoundGrenade
// Model Creator:
// Created on:21.06.2020 - 21:25:37
// Last changed on: 21.06.2020 - 21:25:37

package com.flansmod.client.model.mw;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelRoundGrenade extends ModelBase
{
	int textureX = 16;
	int textureY = 4;

	public ModelRoundGrenade()
	{
		roundgrenadeModel = new ModelRendererTurbo[2];
		roundgrenadeModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		roundgrenadeModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Import 

		roundgrenadeModel[0].addBox(-2F, 0F, -3F, 2, 2, 2, 0F); // Import 
		roundgrenadeModel[0].setRotationPoint(0F, 0F, 2F);

		roundgrenadeModel[1].addBox(-2F, 0F, -3F, 1, 3, 1, 0F); // Import 
		roundgrenadeModel[1].setRotationPoint(0.5F, -0.5F, 2.5F);


	}


	public ModelRendererTurbo roundgrenadeModel[];
}
