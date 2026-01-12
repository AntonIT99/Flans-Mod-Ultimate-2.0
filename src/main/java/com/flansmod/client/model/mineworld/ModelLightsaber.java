//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2019 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: 
// Model Creator: 
// Created on: 31.10.2019 - 22:23:12
// Last changed on: 31.10.2019 - 22:23:12

package com.flansmod.client.model.mineworld;

import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelLightsaber extends ModelGun
{
	int textureX = 64;
	int textureY = 64;

	public ModelLightsaber()
	{
		gunModel = new ModelRendererTurbo[5];
		gunModel[0] = new ModelRendererTurbo(this, 0, 21, textureX, textureY); // Box 0
		gunModel[1] = new ModelRendererTurbo(this, 10, 0, textureX, textureY); // Box 1
		gunModel[2] = new ModelRendererTurbo(this, 15, 0, textureX, textureY); // Box 2
		gunModel[3] = new ModelRendererTurbo(this, 21, 0, textureX, textureY); // Box 3
		gunModel[4] = new ModelRendererTurbo(this, 0, 0, textureX, textureY); // Box 6

		gunModel[0].addShapeBox(0F, 0F, 0F, 16, 6, 16, 0F, 0F, 0F, 0F, -14F, 0F, 0F, -14F, 0F, -14F, 0F, 0F, -14F, 0F, -0.5F, 0F, -14F, -0.5F, 0F, -14F, -0.5F, -14F, 0F, -0.5F, -14F); // Box 0
		gunModel[0].setRotationPoint(-1.5F, -3F, -1F);

		gunModel[1].addShapeBox(0F, 0F, 0F, 1, 16, 1, 0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 1
		gunModel[1].setRotationPoint(-1F, -19F, -0.5F);
		gunModel[1].glowNoDepthWrite = true;

		gunModel[2].addShapeBox(0F, 0F, 0F, 1, 16, 1, 0F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 2
		gunModel[2].setRotationPoint(-1F, -19F, -0.5F);
		gunModel[2].glowAdditive = true;
		
		gunModel[3].addShapeBox(0F, 0F, 0F, 1, 16, 1, 0F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F); // Box 3
		gunModel[3].setRotationPoint(-1F, -19F, -0.5F);
		gunModel[3].glow = true;
		
		gunModel[4].addShapeBox(-0.1F, -1.8F, -0.2F, 1, 1, 1, 0F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F, -0.25F); // Box 6
		gunModel[4].setRotationPoint(0F, 0F, 0F);

		translateAll(0F, 0F, 0F);

		flipAll();
	}
}