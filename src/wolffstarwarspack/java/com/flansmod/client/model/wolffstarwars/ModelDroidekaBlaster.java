//This File was created with the Minecraft-SMP Modelling Toolbox 2.2.2.4
// Copyright (C) 2016 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: E5
// Model Creator: 
// Created on: 21.10.2016 - 23:07:57
// Last changed on: 21.10.2016 - 23:07:57

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelGun;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelDroidekaBlaster extends ModelGun //Same as Filename
{
	int textureX = 16;
	int textureY = 16;

	public ModelDroidekaBlaster() //Same as Filename
	{
		gunModel = new ModelRendererTurbo[1];
		gunModel[0] = new ModelRendererTurbo(this, 0, 0, textureX, textureY);
		gunModel[0].addBox(0F, 0F, 1F, 11, 1, 1, 0F);
		hasFlash = false;

		translateAll(0F, 0F, 0F);
		flipAll();
	}
}