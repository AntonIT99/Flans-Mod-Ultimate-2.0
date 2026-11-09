//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: BazzHeadwear
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelBazzHeadwear extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 64;

	public ModelBazzHeadwear() //Same as Filename
	{
		headModel = new ModelRendererTurbo[21];

		initheadModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 58
		headModel[1] = new ModelRendererTurbo(this, 9, 1, textureX, textureY); // Box 122
		headModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 62
		headModel[3] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 63
		headModel[4] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 64
		headModel[5] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 65
		headModel[6] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 66
		headModel[7] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Box 67
		headModel[8] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 68
		headModel[9] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 69
		headModel[10] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 70
		headModel[11] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 71
		headModel[12] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 72
		headModel[13] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 73
		headModel[14] = new ModelRendererTurbo(this, 41, 9, textureX, textureY); // Box 74
		headModel[15] = new ModelRendererTurbo(this, 81, 9, textureX, textureY); // Box 75
		headModel[16] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 76
		headModel[17] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 77
		headModel[18] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 78
		headModel[19] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 79
		headModel[20] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 80

		headModel[0].addShapeBox(1F, -4F, -4.5F, 2, 1, 1, 0F,0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 58
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(-1F, -1.5F, -4F, 2, 1, 1, 0F,1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F); // Box 122
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(1F, -4.6F, -4.5F, 2, 1, 1, 0F,-0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 62
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(1F, -3.4F, -4.5F, 2, 1, 1, 0F,0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F); // Box 63
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-3F, -3.4F, -4.5F, 2, 1, 1, 0F,0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F); // Box 64
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-3F, -4F, -4.5F, 2, 1, 1, 0F,0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 65
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(-3F, -4.6F, -4.5F, 2, 1, 1, 0F,-0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, -0.6F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 66
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(-1F, -5F, -4.3F, 1, 1, 1, 0F,0.5F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, -1F, 0F, 0.5F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 1F, 0F); // Box 67
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(0F, -5F, -4.3F, 1, 1, 1, 0F,0F, 0F, 0F, 0.5F, -1F, 0F, 0.5F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 1F, 0F, 0.5F, 1F, 0F, 0F, 0F, 0F); // Box 68
		headModel[8].setRotationPoint(0F, 0F, 0F);

		headModel[9].addShapeBox(-4.5F, -4F, -4.3F, 2, 1, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 69
		headModel[9].setRotationPoint(0F, 0F, 0F);

		headModel[10].addShapeBox(2.5F, -4F, -4.3F, 2, 1, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 70
		headModel[10].setRotationPoint(0F, 0F, 0F);

		headModel[11].addShapeBox(2F, -11F, 2F, 2, 3, 2, 0F,0F, 0F, 0F, 0F, -0.2F, 0F, 0F, -0.5F, 0F, 0F, -0.2F, 0F, 1F, 0F, 1F, -0.5F, 0F, 1F, -0.5F, 0F, -0.5F, 1F, 0F, -0.5F); // Box 71
		headModel[11].setRotationPoint(0F, 0F, 0F);

		headModel[12].addShapeBox(-4F, -11F, 2F, 2, 3, 2, 0F,0F, -0.2F, 0F, 0F, 0F, 0F, 0F, -0.2F, 0F, 0F, -0.5F, 0F, -0.5F, 0F, 1F, 1F, 0F, 1F, 1F, 0F, -0.5F, -0.5F, 0F, -0.5F); // Box 72
		headModel[12].setRotationPoint(0F, 0F, 0F);

		headModel[13].addShapeBox(2F, -7.5F, 1F, 9, 3, 9, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 73
		headModel[13].setRotationPoint(0F, 0F, 0F);

		headModel[14].addShapeBox(2F, -10.5F, 1F, 9, 3, 9, 0F,-3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 74
		headModel[14].setRotationPoint(0F, 0F, 0F);

		headModel[15].addShapeBox(2F, -4.5F, 1F, 9, 3, 9, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F); // Box 75
		headModel[15].setRotationPoint(0F, 0F, 0F);

		headModel[16].addShapeBox(-11F, -10.5F, 1F, 9, 3, 9, 0F,-3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 76
		headModel[16].setRotationPoint(0F, 0F, 0F);

		headModel[17].addShapeBox(-11F, -7.5F, 1F, 9, 3, 9, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 77
		headModel[17].setRotationPoint(0F, 0F, 0F);

		headModel[18].addShapeBox(-11F, -4.5F, 1F, 9, 3, 9, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F, -3F, 0F, -3F); // Box 78
		headModel[18].setRotationPoint(0F, 0F, 0F);

		headModel[19].addShapeBox(-1F, -2.5F, -4F, 1, 1, 1, 0F,0F, -0.2F, 0.3F, -0.4F, -0.2F, 0.3F, -0.4F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F); // Box 79
		headModel[19].setRotationPoint(0F, 0F, 0F);

		headModel[20].addShapeBox(0F, -2.5F, -4F, 1, 1, 1, 0F,-0.4F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, -0.4F, -0.2F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F); // Box 80
		headModel[20].setRotationPoint(0F, 0F, 0F);
	}
}