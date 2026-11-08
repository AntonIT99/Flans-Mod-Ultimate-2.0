//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Ted
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelTed extends ModelCustomArmour //Same as Filename
{
	int textureX = 64;
	int textureY = 128;

	public ModelTed() //Same as Filename
	{
		headModel = new ModelRendererTurbo[14];

		initheadModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 19
		headModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 20
		headModel[2] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 29
		headModel[3] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 17
		headModel[4] = new ModelRendererTurbo(this, 9, 9, textureX, textureY); // Box 16
		headModel[5] = new ModelRendererTurbo(this, 25, 9, textureX, textureY); // Box 18
		headModel[6] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 16
		headModel[7] = new ModelRendererTurbo(this, 25, 17, textureX, textureY); // Box 29
		headModel[8] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 30
		headModel[9] = new ModelRendererTurbo(this, 33, 33, textureX, textureY); // Box 31
		headModel[10] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 33
		headModel[11] = new ModelRendererTurbo(this, 41, 41, textureX, textureY); // Box 34
		headModel[12] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 35
		headModel[13] = new ModelRendererTurbo(this, 41, 9, textureX, textureY); // Box 36

		headModel[0].addShapeBox(-4F, -2F, -4.7F, 4, 1, 1, 0F,0F, -1.5F, -0.3F, -1F, 0.5F, -0.3F, -1F, 0.5F, -0.3F, 0F, -1.5F, -0.3F, -2.5F, 0F, -0.3F, 0.2F, -1F, -0.3F, 0.2F, -1F, -0.3F, -2.5F, 0F, -0.3F); // Box 19
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(-4F, -1F, -3.9F, 8, 2, 2, 0F,0F, 0F, 0.2F, 0F, 0F, 0.2F, 0.2F, 2F, 2.8F, 0.2F, 2F, 2.8F, 0.2F, -0.5F, 0.2F, 0.2F, -0.5F, 0.2F, 0.2F, -0.5F, 0.2F, 0.2F, -0.5F, 0.2F); // Box 20
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(-4F, 0.5F, -3.9F, 8, 1, 2, 0F,0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, -1.3F, 0.5F, 0.5F, -1.3F, 0.5F, 0.5F, -1.5F, 0.9F, -0.8F, -1.5F, 0.9F, -0.8F); // Box 29
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(0F, -2F, -4.7F, 4, 1, 1, 0F,-1F, 0.5F, -0.3F, 0F, -1.5F, -0.3F, 0F, -1.5F, -0.3F, -1F, 0.5F, -0.3F, 0.2F, -1F, -0.3F, -2.5F, 0F, -0.3F, -2.5F, 0F, -0.3F, 0.2F, -1F, -0.3F); // Box 17
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-5.5F, -7.5F, -3.9F, 2, 3, 8, 0F,-0.5F, 0F, 0.2F, 0.3F, 0F, 0.2F, 0.3F, 0F, 0.8F, -0.5F, 0F, 0.8F, 0.2F, 0F, 0.2F, 0F, -0.5F, 0.2F, 0F, 0F, 1.2F, 0.2F, 0.5F, 1.2F); // Box 16
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-3F, -5F, -4F, 2, 1, 1, 0F,0.4F, -0.6F, 0.2F, 0F, -0.5F, 0.2F, 0F, -0.5F, 0.2F, 0.4F, -0.6F, 0.2F, 0.2F, 0.1F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0.1F, 0.2F); // Box 18
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(1F, -5F, -4F, 2, 1, 1, 0F,0F, -0.5F, 0.2F, 0.4F, -0.6F, 0.2F, 0.4F, -0.6F, 0.2F, 0F, -0.5F, 0.2F, 0.4F, 0F, 0.2F, 0.2F, 0.1F, 0.2F, 0F, 0.1F, 0.2F, 0F, 0F, 0.2F); // Box 16
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(-5F, -9.5F, -5F, 7, 3, 10, 0F,-1F, 0F, -1.5F, -1F, 0.5F, -2F, -1F, -1F, 0F, -1F, -1F, 0F, 0F, 0.5F, 0F, 1F, 0F, 0F, 1F, 0F, 1F, 0F, 0F, 1F); // Box 29
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(-5F, -6.5F, 3F, 10, 5, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 30
		headModel[8].setRotationPoint(0F, 0F, 0F);

		headModel[9].addShapeBox(-5.5F, -4.5F, -0.9F, 2, 3, 5, 0F,0.5F, 0F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0.5F, 1.2F, 0.5F, -0.5F, 1.2F, -0.5F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, 0.8F, -0.5F, 0F, 0.8F); // Box 31
		headModel[9].setRotationPoint(0F, 0F, 0F);

		headModel[10].addShapeBox(0F, -9.5F, -5F, 5, 3, 10, 0F,-1F, 0F, -1.5F, -1F, -0.5F, -1.5F, -1F, -1F, 0F, -1F, -1F, 0F, 1F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0F, 1F, 1F, 0F, 1F); // Box 33
		headModel[10].setRotationPoint(0F, 0F, 0F);

		headModel[11].addShapeBox(3.5F, -7.5F, -3.9F, 2, 3, 8, 0F,0.3F, 0F, 0.2F, -0.5F, 0F, 0.2F, -0.5F, 0F, 0.8F, 0.3F, 0F, 0.8F, 0F, -0.5F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0.5F, 1.2F, 0F, 0F, 1.2F); // Box 34
		headModel[11].setRotationPoint(0F, 0F, 0F);

		headModel[12].addShapeBox(3.5F, -4.5F, -0.9F, 2, 3, 5, 0F,0F, 0.5F, 0.2F, 0F, 0F, 0.2F, 0F, -0.5F, 1.2F, 0F, 0.5F, 1.2F, 0.3F, 0F, -0.8F, -0.5F, 0F, -0.8F, -0.5F, 0F, 0.8F, 0.3F, 0F, 0.8F); // Box 35
		headModel[12].setRotationPoint(0F, 0F, 0F);

		headModel[13].addShapeBox(-4F, -1.5F, 3F, 8, 2, 2, 0F,1F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, -2F, -1F, 0F, -2F, -1F, 0F, -2F, -0.5F, 1F, -2F, -0.5F, 1F); // Box 36
		headModel[13].setRotationPoint(0F, 0F, 0F);
	}
}