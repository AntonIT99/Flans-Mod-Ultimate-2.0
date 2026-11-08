//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: YakubBrain
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelYakubBrain extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelYakubBrain() //Same as Filename
	{
		headModel = new ModelRendererTurbo[9];

		initheadModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 1, 72, textureX, textureY); // Box 21
		headModel[1] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 23
		headModel[2] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 24
		headModel[3] = new ModelRendererTurbo(this, 65, 25, textureX, textureY); // Box 25
		headModel[4] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 26
		headModel[5] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 18
		headModel[6] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 16
		headModel[7] = new ModelRendererTurbo(this, 65, 49, textureX, textureY); // Box 29
		headModel[8] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Box 30

		headModel[0].addShapeBox(-14F, -17F, -6F, 28, 13, 14, 0F,-7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F, -7F, -4F, 0F); // Box 21
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(-7F, -16F, -6F, 14, 3, 14, 0F,-4F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 23
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(-7F, -8F, -6F, 14, 4, 14, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, -2F, -3F, 0F, -2F, -3F, 0F, -4F, -3F, 0F, -4F); // Box 24
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(-8F, -13F, -3F, 16, 5, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 25
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-8F, -8F, -3F, 16, 5, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, 0F, -4F, 0F, 0F, -4F, 0F, -5F, -4F, 0F, -5F); // Box 26
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-3F, -5.2F, -4.5F, 2, 1, 1, 0F,0.4F, -0.1F, 0.2F, 0F, -0.5F, 0.2F, 0F, -0.5F, 0.2F, 0.4F, -0.1F, 0.2F, 0.2F, -0.1F, 0.2F, 0.4F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, -0.1F, 0.2F); // Box 18
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(1F, -5.2F, -4.5F, 2, 1, 1, 0F,0F, -0.5F, 0.2F, 0.4F, -0.1F, 0.2F, 0.4F, -0.1F, 0.2F, 0F, -0.5F, 0.2F, 0.4F, 0.2F, 0.2F, 0.2F, -0.1F, 0.2F, 0F, -0.1F, 0.2F, 0F, 0.2F, 0.2F); // Box 16
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(-8F, -16F, -3F, 16, 3, 12, 0F,-4F, 0.5F, -1F, -4F, 0.5F, -1F, -4F, 0.5F, -5F, -4F, 0.5F, -5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 29
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(-2F, -16.5F, -4F, 4, 2, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 2F, 0F, 0F, -1.8F, 0F, 0F, -1.8F, 0F, 0F, -0.8F, 0F, 0F, -0.8F, 0F, 0F); // Box 30
		headModel[8].setRotationPoint(0F, 0F, 0F);
	}
}