//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Droyd
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelDroyd extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 64;

	public ModelDroyd() //Same as Filename
	{
		headModel = new ModelRendererTurbo[42];
		rightArmModel = new ModelRendererTurbo[7];

		initheadModel_1();
		initrightArmModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 122
		headModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 79
		headModel[2] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 80
		headModel[3] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 18
		headModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 22
		headModel[5] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 32
		headModel[6] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 34
		headModel[7] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 2
		headModel[8] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 6
		headModel[9] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 51
		headModel[10] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 54
		headModel[11] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 57
		headModel[12] = new ModelRendererTurbo(this, 97, 9, textureX, textureY); // Box 58
		headModel[13] = new ModelRendererTurbo(this, 9, 1, textureX, textureY); // Box 59
		headModel[14] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 60
		headModel[15] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Box 61
		headModel[16] = new ModelRendererTurbo(this, 33, 17, textureX, textureY); // Box 62
		headModel[17] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 63
		headModel[18] = new ModelRendererTurbo(this, 65, 17, textureX, textureY); // Box 25
		headModel[19] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 34
		headModel[20] = new ModelRendererTurbo(this, 81, 17, textureX, textureY); // Box 35
		headModel[21] = new ModelRendererTurbo(this, 97, 17, textureX, textureY); // Box 36
		headModel[22] = new ModelRendererTurbo(this, 113, 17, textureX, textureY); // Box 37
		headModel[23] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 38
		headModel[24] = new ModelRendererTurbo(this, 17, 25, textureX, textureY); // Box 40
		headModel[25] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Box 41
		headModel[26] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 42
		headModel[27] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 44
		headModel[28] = new ModelRendererTurbo(this, 17, 9, textureX, textureY); // Box 45
		headModel[29] = new ModelRendererTurbo(this, 65, 25, textureX, textureY); // Box 46
		headModel[30] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 47
		headModel[31] = new ModelRendererTurbo(this, 97, 25, textureX, textureY); // Box 48
		headModel[32] = new ModelRendererTurbo(this, 113, 25, textureX, textureY); // Box 61
		headModel[33] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 62
		headModel[34] = new ModelRendererTurbo(this, 73, 33, textureX, textureY); // Box 63
		headModel[35] = new ModelRendererTurbo(this, 89, 33, textureX, textureY); // Box 64
		headModel[36] = new ModelRendererTurbo(this, 105, 33, textureX, textureY); // Box 65
		headModel[37] = new ModelRendererTurbo(this, 9, 33, textureX, textureY); // Box 3
		headModel[38] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Box 43
		headModel[39] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 45
		headModel[40] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 97
		headModel[41] = new ModelRendererTurbo(this, 121, 9, textureX, textureY); // Box 98

		headModel[0].addShapeBox(-1.5F, -1.2F, -4F, 3, 1, 1, 0F,1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F); // Box 122
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(-1.5F, -2.2F, -4F, 1, 1, 1, 0F,0.1F, -0.3F, 0.3F, 0.3F, -0.2F, 0.3F, 0.3F, -0.2F, 0.3F, 0.1F, -0.3F, 0.3F, 1F, 0F, 0.3F, 1.5F, 0F, 0.3F, 1.5F, 0F, 0.3F, 1F, 0F, 0.3F); // Box 79
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(0.5F, -2.2F, -4F, 1, 1, 1, 0F,0.3F, -0.2F, 0.3F, 0.1F, -0.3F, 0.3F, 0.1F, -0.3F, 0.3F, 0.3F, -0.2F, 0.3F, 1.5F, 0F, 0.3F, 1F, 0F, 0.3F, 1F, 0F, 0.3F, 1.5F, 0F, 0.3F); // Box 80
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(2F, -8.6F, -4.3F, 2, 3, 8, 0F,1F, 0.8F, 0.5F, 0.2F, 0.8F, -0.5F, 0.2F, -0.1F, 0.7F, 1F, -0.3F, 1.3F, 0F, -1.2F, 2F, 1F, -1F, 0.7F, 1F, 0.3F, 1.2F, 0F, 0.3F, 2F); // Box 18
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(2F, -10.5F, -4.3F, 2, 1, 8, 0F,1F, 0.2F, -2.3F, -1.4F, -0.2F, -2.7F, -1.4F, -0.5F, -1.2F, 1F, -0.2F, -0.7F, 1F, 0.1F, 0.5F, 0.2F, 0.1F, -0.5F, 0.2F, 1F, 0.7F, 1F, 1.2F, 1.3F); // Box 22
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(-1F, -8.6F, -4.3F, 2, 3, 8, 0F,0F, 0.8F, 0.5F, 0F, 0.8F, 0.5F, 0F, -0.3F, 1.3F, 0F, -0.3F, 1.3F, 1F, -1.2F, 2F, 1F, -1.2F, 2F, 1F, 0.3F, 2F, 1F, 0.3F, 2F); // Box 32
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(0F, -6.1F, -4.3F, 1, 5, 1, 0F,0F, 0.7F, 2F, 1F, 0.7F, 2F, 1F, -0.8F, 2F, 0F, -0.8F, 2F, 0F, -3F, 2.8F, 0.5F, -3.4F, 2.4F, 0.5F, -2.7F, 0F, 0F, -2F, 0F); // Box 34
		headModel[6].setRotationPoint(0F, 0F, 0F);

		headModel[7].addShapeBox(2F, -6.1F, -5.3F, 3, 5, 2, 0F,0F, 0.7F, 1F, 0F, 0.5F, -0.3F, 0F, 0.18F, 1F, 0F, 0.1F, 0F, 0.5F, -3.4F, 1.4F, 0F, -4.1F, 0.2F, 0.1F, -3.8F, 0.79F, 0.5F, -2.7F, 0F); // Box 2
		headModel[7].setRotationPoint(0F, 0F, 0F);

		headModel[8].addShapeBox(2F, -6.1F, 1.7F, 3, 5, 2, 0F,0F, 0F, 1.7F, 0F, -0.35F, 0F, 0F, -0.8F, 1.2F, 0F, -0.8F, 2F, 0F, -1.3F, 1.9F, 0.3F, -0.1F, 0.4F, 0.1F, 0.1F, 0.75F, 0.05F, 0.3F, 1.65F); // Box 6
		headModel[8].setRotationPoint(0F, 0F, 0F);

		headModel[9].addShapeBox(-1.5F, -11.5F, -2.3F, 3, 1, 6, 0F,0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, -0.4F, -0.7F, 0F, -0.4F, -0.7F, 0.2F, 0.1F, -0.3F, 0.2F, 0.1F, -0.3F, 0.2F, 1.4F, 2F, 0.2F, 1.4F, 2F); // Box 51
		headModel[9].setRotationPoint(0F, 0F, 0F);

		headModel[10].addShapeBox(-2F, -6.1F, 2.7F, 3, 5, 1, 0F,0F, 0.7F, 2F, 1F, 0.7F, 2F, 1F, -0.8F, 2F, 0F, -0.8F, 2F, 0F, -3.4F, 2.8F, 0.5F, -3.8F, 2.4F, 1F, 0.3F, 1.65F, 0F, 0.3F, 1.65F); // Box 54
		headModel[10].setRotationPoint(0F, 0F, 0F);

		headModel[11].addShapeBox(-1F, -6.1F, -4.3F, 1, 5, 1, 0F,1F, 0.7F, 2F, 0F, 0.7F, 2F, 0F, -0.8F, 2F, 1F, -0.8F, 2F, 0.5F, -3.4F, 2.4F, 0F, -3F, 2.8F, 0F, -2F, 0F, 0.5F, -2.7F, 0F); // Box 57
		headModel[11].setRotationPoint(0F, 0F, 0F);

		headModel[12].addShapeBox(-1.5F, -11.5F, -4.7F, 7, 3, 3, 0F,0F, -0.8F, -0.3F, -4F, -0.8F, -0.3F, -4F, 0F, -0.3F, 0F, 0F, -0.3F, 0.2F, -0.8F, -0.3F, -3.8F, -0.8F, -0.3F, -3.8F, -1.9F, -0.3F, 0.2F, -1.9F, -0.3F); // Box 58
		headModel[12].setRotationPoint(0F, 0F, 0F);

		headModel[13].addShapeBox(4.1F, -3.1F, -1.3F, 2, 3, 3, 0F,0F, 0.9F, 0.5F, 0.1F, 0.9F, 0F, 0.1F, 0.5F, 0F, 0F, 0.5F, 0.5F, 0F, 0F, 1F, 0.4F, 0F, 0.5F, 0.4F, 0.3F, -0.4F, 0F, 0.4F, 0.1F); // Box 59
		headModel[13].setRotationPoint(0F, 0F, 0F);

		headModel[14].addShapeBox(4.1F, -5F, -1.3F, 2, 1, 3, 0F,0F, 0.7F, -0.6F, 0F, 0.2F, -1F, 0F, 0F, -0.5F, 0F, 0.4F, 0F, 0F, 0F, 0.5F, 0.1F, 0F, 0F, 0.1F, 0.4F, 0F, 0F, 0.4F, 0.5F); // Box 60
		headModel[14].setRotationPoint(0F, 0F, 0F);

		headModel[15].addShapeBox(5.6F, -3.8F, -1.3F, 1, 1, 3, 0F,0F, -0.1F, -0.5F, 0F, -0.1F, -0.7F, 0F, -0.1F, -0.7F, 0F, -0.1F, -0.5F, 0F, -0.1F, -0.5F, 0.1F, -0.1F, -0.7F, 0.1F, -0.1F, -0.7F, 0F, -0.1F, -0.5F); // Box 61
		headModel[15].setRotationPoint(0F, 0F, 0F);

		headModel[16].addShapeBox(5.6F, -4.6F, -1.3F, 1, 1, 3, 0F,0F, -0.1F, -1.1F, -0.05F, -0.1F, -1.1F, -0.05F, -0.1F, -1.1F, 0F, -0.1F, -1.1F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F); // Box 62
		headModel[16].setRotationPoint(0F, 0F, 0F);

		headModel[17].addShapeBox(5.6F, -3F, -1.3F, 1, 1, 3, 0F,0F, -0.1F, -0.2F, 0.1F, -0.1F, -0.2F, 0.1F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -1.1F, 0.15F, -0.1F, -1.1F, 0.15F, -0.1F, -1.1F, 0F, -0.1F, -1.1F); // Box 63
		headModel[17].setRotationPoint(0F, 0F, 0F);

		headModel[18].addShapeBox(-1.5F, -9.1F, 3.2F, 3, 4, 2, 0F,0.2F, 0.5F, 0F, 0.2F, 0.5F, 0F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0.5F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 2F, 0.2F, 0F, 2F); // Box 25
		headModel[18].setRotationPoint(0F, 0F, 0F);

		headModel[19].addShapeBox(2F, -4.6F, 3.7F, 2, 3, 1, 0F,1F, 1F, 0F, 1F, 0.65F, 0F, 1F, -0.8F, 1.2F, 0.3F, -0.8F, 2.34F, 0.5F, 0F, 0F, 1.3F, -0.1F, 0F, 1.1F, 0.3F, 0.75F, 0.3F, 0.5F, 2F); // Box 34
		headModel[19].setRotationPoint(0F, 0F, 0F);

		headModel[20].addShapeBox(-1.5F, -5.1F, 3.2F, 3, 4, 2, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 2F, 0.2F, 0F, 2F, 0.2F, -0.5F, 0F, 0.2F, -0.5F, 0F, 0.2F, 0F, 1.5F, 0.2F, 0F, 1.5F); // Box 35
		headModel[20].setRotationPoint(0F, 0F, 0F);

		headModel[21].addShapeBox(4.4F, -4.6F, 0.7F, 1, 3, 3, 0F,0F, -0.3F, 0F, 0.6F, -0.3F, 0F, 0.6F, -0.8F, 1.2F, -0.6F, -0.8F, 2.2F, 0F, 0F, 0.9F, 0.9F, -0.1F, 0.4F, 0.7F, 0.1F, 0.75F, -0.7F, 0.3F, 1.8F); // Box 36
		headModel[21].setRotationPoint(0F, 0F, 0F);

		headModel[22].addShapeBox(4.4F, -4.8F, 0.7F, 1, 1, 3, 0F,0F, 0.7F, 0.9F, 0.5F, -0.3F, 0.4F, 0.5F, -0.9F, 1F, -0.6F, -0.2F, 1F, 0F, -0.5F, 0F, 0.6F, -0.5F, 0F, 0.6F, 0F, 1.2F, -0.6F, 0F, 2.2F); // Box 37
		headModel[22].setRotationPoint(0F, 0F, 0F);

		headModel[23].addShapeBox(5F, -1.6F, 0.7F, 1, 2, 3, 0F,1F, 0F, 0F, 0.27F, 0.05F, 0F, 0.15F, 0F, -0.5F, 1F, 0F, -0.5F, 0F, -0.5F, 0F, 0.4F, -0.5F, -0.5F, 0.3F, -0.4F, -1.5F, 0F, -0.4F, -1.5F); // Box 38
		headModel[23].setRotationPoint(0F, 0F, 0F);

		headModel[24].addShapeBox(-5F, -6.1F, -5.3F, 3, 5, 2, 0F,0F, 0.5F, -0.3F, 0F, 0.7F, 1F, 0F, 0.1F, 0F, 0F, 0.18F, 1F, 0F, -4.1F, 0.2F, 0.5F, -3.4F, 1.4F, 0.5F, -2.7F, 0F, 0.1F, -3.8F, 0.79F); // Box 40
		headModel[24].setRotationPoint(0F, 0F, 0F);

		headModel[25].addShapeBox(-4F, -8.6F, -4.3F, 2, 3, 8, 0F,0.2F, 0.8F, -0.5F, 1F, 0.8F, 0.5F, 1F, -0.3F, 1.3F, 0.2F, -0.1F, 0.7F, 1F, -1F, 0.7F, 0F, -1.2F, 2F, 0F, 0.3F, 2F, 1F, 0.3F, 1.2F); // Box 41
		headModel[25].setRotationPoint(0F, 0F, 0F);

		headModel[26].addShapeBox(-4F, -10.5F, -4.3F, 2, 1, 8, 0F,-1.4F, -0.2F, -2.7F, 1F, 0.2F, -2.3F, 1F, -0.2F, -0.7F, -1.4F, -0.5F, -1.2F, 0.2F, 0.1F, -0.5F, 1F, 0.1F, 0.5F, 1F, 1.2F, 1.3F, 0.2F, 1F, 0.7F); // Box 42
		headModel[26].setRotationPoint(0F, 0F, 0F);

		headModel[27].addShapeBox(-5F, -6.1F, 1.7F, 3, 5, 2, 0F,0F, -0.35F, 0F, 0F, 0F, 1.7F, 0F, -0.8F, 2F, 0F, -0.8F, 1.2F, 0.3F, -0.1F, 0.4F, 0F, -1.3F, 1.9F, 0.05F, 0.3F, 1.65F, 0.1F, 0.1F, 0.75F); // Box 44
		headModel[27].setRotationPoint(0F, 0F, 0F);

		headModel[28].addShapeBox(-4F, -4.6F, 3.7F, 2, 3, 1, 0F,0.9F, 0.65F, 0F, 1F, 1F, 0F, 0.3F, -0.8F, 2.34F, 1F, -0.8F, 1.2F, 1.3F, -0.1F, 0F, 0.5F, 0F, 0F, 0.3F, 0.5F, 2F, 1.1F, 0.3F, 0.75F); // Box 45
		headModel[28].setRotationPoint(0F, 0F, 0F);

		headModel[29].addShapeBox(-5.4F, -4.6F, 0.7F, 1, 3, 3, 0F,0.6F, -0.3F, 0F, 0F, -0.3F, 0F, -0.6F, -0.8F, 2.2F, 0.6F, -0.8F, 1.2F, 0.9F, -0.1F, 0.4F, 0F, 0F, 0.9F, -0.7F, 0.3F, 1.8F, 0.7F, 0.1F, 0.75F); // Box 46
		headModel[29].setRotationPoint(0F, 0F, 0F);

		headModel[30].addShapeBox(-5.4F, -4.8F, 0.7F, 1, 1, 3, 0F,0.5F, -0.3F, 0.4F, 0F, 0.7F, 0.9F, -0.55F, -0.2F, 1F, 0.5F, -0.9F, 1F, 0.6F, -0.5F, 0F, 0F, -0.5F, 0F, -0.6F, 0F, 2.2F, 0.6F, 0F, 1.2F); // Box 47
		headModel[30].setRotationPoint(0F, 0F, 0F);

		headModel[31].addShapeBox(-6F, -1.6F, 0.7F, 1, 2, 3, 0F,0.27F, 0.05F, 0F, 1F, 0F, 0F, 1F, 0F, -0.5F, 0.15F, 0F, -0.5F, 0.4F, -0.5F, -0.5F, 0F, -0.5F, 0F, 0F, -0.4F, -1.5F, 0.3F, -0.4F, -1.5F); // Box 48
		headModel[31].setRotationPoint(0F, 0F, 0F);

		headModel[32].addShapeBox(-6.1F, -3.1F, -1.3F, 2, 3, 3, 0F,0.1F, 0.9F, 0F, 0F, 0.9F, 0.5F, 0F, 0.5F, 0.5F, 0.1F, 0.5F, 0F, 0.4F, 0F, 0.5F, 0F, 0F, 1F, 0F, 0.4F, 0.1F, 0.4F, 0.3F, -0.4F); // Box 61
		headModel[32].setRotationPoint(0F, 0F, 0F);

		headModel[33].addShapeBox(-6.1F, -5F, -1.3F, 2, 1, 3, 0F,0F, 0.2F, -1F, 0F, 0.7F, -0.6F, 0F, 0.4F, 0F, 0F, 0F, -0.5F, 0.1F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0.4F, 0.5F, 0.1F, 0.4F, 0F); // Box 62
		headModel[33].setRotationPoint(0F, 0F, 0F);

		headModel[34].addShapeBox(-6.6F, -3F, -1.3F, 1, 1, 3, 0F,0.1F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0.1F, -0.1F, -0.2F, 0.15F, -0.1F, -1.1F, 0F, -0.1F, -1.1F, 0F, -0.1F, -1.1F, 0.15F, -0.1F, -1.1F); // Box 63
		headModel[34].setRotationPoint(0F, 0F, 0F);

		headModel[35].addShapeBox(-6.6F, -3.8F, -1.3F, 1, 1, 3, 0F,0F, -0.1F, -0.7F, 0F, -0.1F, -0.5F, 0F, -0.1F, -0.5F, 0F, -0.1F, -0.7F, 0.1F, -0.1F, -0.7F, 0F, -0.1F, -0.5F, 0F, -0.1F, -0.5F, 0.1F, -0.1F, -0.7F); // Box 64
		headModel[35].setRotationPoint(0F, 0F, 0F);

		headModel[36].addShapeBox(-6.6F, -4.6F, -1.3F, 1, 1, 3, 0F,-0.05F, -0.1F, -1.1F, 0F, -0.1F, -1.1F, 0F, -0.1F, -1.1F, -0.05F, -0.1F, -1.1F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F, 0F, -0.1F, -0.2F); // Box 65
		headModel[36].setRotationPoint(0F, 0F, 0F);

		headModel[37].addShapeBox(3F, -6.1F, -3.3F, 1, 5, 5, 0F,1F, 0.15F, -1F, 1F, 0.15F, -1F, 1F, -0.35F, 0F, 1F, 0F, 1.7F, 1F, -0.5F, -0.3F, 1.3F, -0.5F, -0.3F, 1.3F, -0.1F, -0.4F, 1F, -0.1F, 1.9F); // Box 3
		headModel[37].setRotationPoint(0F, 0F, 0F);

		headModel[38].addShapeBox(-4F, -6.1F, -3.3F, 1, 5, 5, 0F,1F, 0.15F, -1F, 1F, 0.15F, -1F, 1F, 0F, 1.7F, 1F, -0.35F, 0F, 1.3F, -0.5F, -0.3F, 1F, -0.5F, -0.3F, 1F, -0.1F, 1.9F, 1.3F, -0.1F, -0.4F); // Box 43
		headModel[38].setRotationPoint(0F, 0F, 0F);

		headModel[39].addShapeBox(-1F, -10.5F, -4.3F, 2, 1, 8, 0F,0F, 0.2F, -2.3F, 0F, 0.2F, -2.3F, 0F, -0.2F, -0.7F, 0F, -0.2F, -0.7F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 1.2F, 1.3F, 0F, 1.2F, 1.3F); // Box 45
		headModel[39].setRotationPoint(0F, 0F, 0F);

		headModel[40].addShapeBox(0F, -4.4F, -5F, 1, 2, 1, 0F,0F, 0F, -1F, -0.5F, 0F, -1F, -0.5F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0.3F, 0F, 0.5F, 0.2F, 0F, 0.5F, 0.4F, 0F, 0F, 0.5F, 0F); // Box 97
		headModel[40].setRotationPoint(0F, 0F, 0F);

		headModel[41].addShapeBox(-1F, -4.4F, -5F, 1, 2, 1, 0F,-0.5F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0.3F, -0.5F, 0F, 0.3F, 0.5F, 0.2F, 0F, 0F, 0.3F, 0F, 0F, 0.5F, 0F, 0.5F, 0.4F, 0F); // Box 98
		headModel[41].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 57, 41, textureX, textureY); // Box 99
		rightArmModel[1] = new ModelRendererTurbo(this, 81, 41, textureX, textureY); // Box 102
		rightArmModel[2] = new ModelRendererTurbo(this, 97, 41, textureX, textureY); // Box 103
		rightArmModel[3] = new ModelRendererTurbo(this, 113, 41, textureX, textureY); // Box 104
		rightArmModel[4] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 105
		rightArmModel[5] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 106
		rightArmModel[6] = new ModelRendererTurbo(this, 121, 33, textureX, textureY); // Box 107

		rightArmModel[0].addBox(-4F, 3F, -1.5F, 6, 8, 3, 0F); // Box 99
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-4F, 3F, -3F, 6, 8, 1, 0F,-1.5F, 0F, 0F, -1.5F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, -1.5F, 0F, 0F, -1.5F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F); // Box 102
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);

		rightArmModel[2].addShapeBox(-4F, 3F, 2F, 6, 8, 1, 0F,0F, 0F, 0.5F, 0F, 0F, 0.5F, -1.5F, 0F, 0F, -1.5F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, -1.5F, 0F, 0F, -1.5F, 0F, 0F); // Box 103
		rightArmModel[2].setRotationPoint(0F, 0F, 0F);

		rightArmModel[3].addShapeBox(-2.5F, 11F, -1.5F, 3, 2, 1, 0F,-1F, 0F, 1F, -1F, 0F, 1F, 1F, 0F, 0F, 1F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 104
		rightArmModel[3].setRotationPoint(0F, 0F, 0F);

		rightArmModel[4].addShapeBox(-2.5F, 11F, 0.5F, 3, 2, 1, 0F,1F, 0F, 0F, 1F, 0F, 0F, -1F, 0F, 1F, -1F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F); // Box 105
		rightArmModel[4].setRotationPoint(0F, 0F, 0F);

		rightArmModel[5].addShapeBox(-2.5F, 11F, -0.5F, 1, 2, 1, 0F,1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 106
		rightArmModel[5].setRotationPoint(0F, 0F, 0F);

		rightArmModel[6].addShapeBox(-0.5F, 11F, -0.5F, 1, 2, 1, 0F,0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 107
		rightArmModel[6].setRotationPoint(0F, 0F, 0F);
	}
}