//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: GermanAssaultChest
// Model Creator: 
// Created on: 28.02.2016 - 16:11:42
// Last changed on: 28.02.2016 - 16:11:42

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelStormtrooperChest extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelStormtrooperChest() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[43];
		bodyModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		bodyModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 3
		bodyModel[2] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Box 5
		bodyModel[3] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 6
		bodyModel[4] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 8
		bodyModel[5] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 9
		bodyModel[6] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Box 10
		bodyModel[7] = new ModelRendererTurbo(this, 81, 9, textureX, textureY); // Box 11
		bodyModel[8] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 12
		bodyModel[9] = new ModelRendererTurbo(this, 97, 9, textureX, textureY); // Box 13
		bodyModel[10] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 16
		bodyModel[11] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Box 17
		bodyModel[12] = new ModelRendererTurbo(this, 113, 9, textureX, textureY); // Box 18
		bodyModel[13] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Box 19
		bodyModel[14] = new ModelRendererTurbo(this, 57, 17, textureX, textureY); // Box 20
		bodyModel[15] = new ModelRendererTurbo(this, 73, 17, textureX, textureY); // Box 21
		bodyModel[16] = new ModelRendererTurbo(this, 89, 17, textureX, textureY); // Box 22
		bodyModel[17] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 23
		bodyModel[18] = new ModelRendererTurbo(this, 49, 33, textureX, textureY); // Box 30
		bodyModel[19] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 32
		bodyModel[20] = new ModelRendererTurbo(this, 25, 49, textureX, textureY); // Box 43
		bodyModel[21] = new ModelRendererTurbo(this, 49, 49, textureX, textureY); // Box 44
		bodyModel[22] = new ModelRendererTurbo(this, 65, 49, textureX, textureY); // Box 45
		bodyModel[23] = new ModelRendererTurbo(this, 25, 61, textureX, textureY); // Box 39
		bodyModel[24] = new ModelRendererTurbo(this, 81, 64, textureX, textureY); // Box 43
		bodyModel[25] = new ModelRendererTurbo(this, 81, 64, textureX, textureY); // Box 44
		bodyModel[26] = new ModelRendererTurbo(this, 81, 64, textureX, textureY); // Box 45
		bodyModel[27] = new ModelRendererTurbo(this, 2, 43, textureX, textureY); // Box 49
		bodyModel[28] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 50
		bodyModel[29] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 51
		bodyModel[30] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 52
		bodyModel[31] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 54
		bodyModel[32] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 55
		bodyModel[33] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 57
		bodyModel[34] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 58
		bodyModel[35] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 60
		bodyModel[36] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 61
		bodyModel[37] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 62
		bodyModel[38] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 64
		bodyModel[39] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 65
		bodyModel[40] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 66
		bodyModel[41] = new ModelRendererTurbo(this, 52, 94, textureX, textureY); // Box 67
		bodyModel[42] = new ModelRendererTurbo(this, 52, 94, textureX, textureY); // Box 68

		bodyModel[0].addShapeBox(-4F, 0F, -2F, 8, 11, 4, 0F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F); // Import 
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(-4F, 0.9F, -2.9F, 4, 3, 1, 0F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, 0F, -3F, 0.1F, 0F, 0F, -0.9F, 0F, 0F, 0.1F, 0F, -3F, 1.1F, 0F); // Box 3
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(-4F, 1F, -2.2F, 8, 5, 4, 0F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F); // Box 5
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(-4F, 6.2F, -2.2F, 2, 2, 3, 0F, 0.01F, 0.1F, 0.4F, -1F, 0.1F, 0.4F, -1F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, -0.2F, 0.4F, -1F, -0.2F, 0.4F, -1F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F); // Box 6
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-2F, 6.2F, -2.2F, 2, 2, 3, 0F, 1F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, 1F, 0.1F, 0.4F, 1F, -0.2F, 0.4F, -0.25F, -1.2F, 0.4F, -0.25F, -1.2F, 0.4F, 1F, -0.2F, 0.4F); // Box 8
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(0F, 6.2F, -2.2F, 2, 2, 3, 0F, -0.25F, 0.1F, 0.4F, 1F, 0.1F, 0.4F, 1F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, -1.2F, 0.4F, 1F, -0.2F, 0.4F, 1F, -0.2F, 0.4F, -0.25F, -1.2F, 0.4F); // Box 9
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(2F, 6.2F, -2.2F, 2, 2, 3, 0F, -1F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, -1F, 0.1F, 0.4F, -1F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F, -1F, -0.2F, 0.4F); // Box 10
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-0.5F, 6.2F, -2.2F, 1, 2, 3, 0F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, -1.2F, 0.4F, -0.25F, -1.2F, 0.4F, -0.25F, -1.2F, 0.4F, -0.25F, -1.2F, 0.4F); // Box 11
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(2F, 8.7F, -3.2F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 12
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-4F, 8.5F, -2.7F, 8, 2, 1, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 13
		bodyModel[9].setRotationPoint(0F, 0F, 0F);

		bodyModel[10].addShapeBox(-4.2F, 8.5F, -2.7F, 1, 2, 5, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0.4F, 0F, -0.25F, 0.4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.4F, 0F, 0F, 0.4F); // Box 16
		bodyModel[10].setRotationPoint(0F, 0F, 0F);

		bodyModel[11].addShapeBox(-4F, 8.5F, 1.7F, 8, 2, 1, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 17
		bodyModel[11].setRotationPoint(0F, 0F, 0F);

		bodyModel[12].addShapeBox(3.2F, 8.5F, -2.7F, 1, 2, 5, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0.4F, 0F, -0.25F, 0.4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.4F, 0F, 0F, 0.4F); // Box 18
		bodyModel[12].setRotationPoint(0F, 0F, 0F);

		bodyModel[13].addShapeBox(-4F, 0F, -2.5F, 2, 2, 3, 0F, 0.01F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, -0.2F, 0.4F, 0.25F, -0.2F, 0.4F, 0.25F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F); // Box 19
		bodyModel[13].setRotationPoint(0F, 0F, 0F);

		bodyModel[14].addShapeBox(2F, 0F, -2.5F, 2, 2, 3, 0F, 0.25F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.01F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F, 0.01F, -0.2F, 0.4F, 0.25F, -0.2F, 0.4F); // Box 20
		bodyModel[14].setRotationPoint(0F, 0F, 0F);

		bodyModel[15].addShapeBox(-2F, 0F, -2.5F, 2, 2, 3, 0F, -0.25F, 0.1F, 0.4F, -0.5F, -0.9F, 0.4F, -0.5F, -0.9F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, -0.2F, 0.4F, -0.5F, -0.2F, 0.4F, -0.5F, -0.2F, 0.4F, -0.25F, -0.2F, 0.4F); // Box 21
		bodyModel[15].setRotationPoint(0F, 0F, 0F);

		bodyModel[16].addShapeBox(0F, 0F, -2.5F, 2, 2, 3, 0F, -0.5F, -0.9F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.5F, -0.9F, 0.4F, -0.5F, -0.2F, 0.4F, -0.25F, -0.2F, 0.4F, -0.25F, -0.2F, 0.4F, -0.5F, -0.2F, 0.4F); // Box 22
		bodyModel[16].setRotationPoint(0F, 0F, 0F);

		bodyModel[17].addShapeBox(-2F, 2F, 2F, 16, 6, 1, 0F, 0.75F, 1F, 0F, -11.25F, 1F, 0F, -12F, 0F, 0F, 0F, 0F, 0F, 1F, -1.5F, 0F, -11F, -1.5F, 0F, -12F, -3F, 0F, 0F, -3F, 0F); // Box 23
		bodyModel[17].setRotationPoint(0F, 0F, 0F);

		bodyModel[18].addShapeBox(-1.5F, 11.2F, -2F, 3, 2, 4, 0F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, 0.1F, 0.4F, -0.25F, -0.5F, 0.4F, -0.25F, -0.5F, 0.4F, -0.25F, -0.5F, 0.4F, -0.25F, -0.5F, 0.4F); // Box 30
		bodyModel[18].setRotationPoint(0F, 0F, 0F);

		bodyModel[19].addShapeBox(-4.5F, 11.2F, -2F, 3, 2, 4, 0F, -0.49F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, -0.49F, 0.1F, 0.4F, -0.49F, -2F, 0.4F, 0.25F, -1.25F, 0.4F, 0.25F, -1.25F, 0.4F, -0.49F, -2F, 0.4F); // Box 32
		bodyModel[19].setRotationPoint(0F, 0F, 0F);

		bodyModel[20].addShapeBox(-2.5F, -0.2F, -2.2F, 5, 1, 4, 0F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F, 0F, 0.1F, 0.4F); // Box 43
		bodyModel[20].setRotationPoint(0F, 0F, 0F);

		bodyModel[21].addShapeBox(-3.9F, -0.1F, -2.2F, 2, 1, 4, 0F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F); // Box 44
		bodyModel[21].setRotationPoint(0F, 0F, 0F);

		bodyModel[22].addShapeBox(1.9F, -0.1F, -2.2F, 2, 1, 4, 0F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F, 0F, 0.1F, 0.5F); // Box 45
		bodyModel[22].setRotationPoint(0F, 0F, 0F);

		bodyModel[23].addShapeBox(-2F, 2.5F, 2.01F, 20, 12, 1, 0F, 0F, 0.25F, 0F, -16F, 0.25F, 0F, -16F, 0.25F, 0F, 0F, 0.25F, 0F, 0F, -9.75F, 0F, -16F, -9.75F, 0F, -16F, -9.75F, 0F, 0F, -9.75F, 0F); // Box 39
		bodyModel[23].setRotationPoint(0F, 0F, 0F);

		bodyModel[24].addShapeBox(-1.5F, 9F, 2.7F, 3, 1, 1, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 43
		bodyModel[24].setRotationPoint(0F, 0F, 0F);

		bodyModel[25].addShapeBox(-1.5F, 8.4F, 2.7F, 3, 1, 1, 0F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F); // Box 44
		bodyModel[25].setRotationPoint(0F, 0F, 0F);

		bodyModel[26].addShapeBox(-1.5F, 9.6F, 2.7F, 3, 1, 1, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.2F, 0F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F, 0F, -0.5F, -0.2F); // Box 45
		bodyModel[26].setRotationPoint(0F, 0F, 0F);

		bodyModel[27].addShapeBox(-1.5F, 9.8F, -1.5F, 3, 3, 3, 0F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F, 2.52F, 0F, 0.52F); // Box 49
		bodyModel[27].setRotationPoint(0F, 0F, 0F);

		bodyModel[28].addShapeBox(0F, 8.7F, -3.2F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 50
		bodyModel[28].setRotationPoint(0F, 0F, 0F);

		bodyModel[29].addShapeBox(-2F, 8.7F, -3.2F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 51
		bodyModel[29].setRotationPoint(0F, 0F, 0F);

		bodyModel[30].addShapeBox(-4F, 8.7F, -3.2F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 52
		bodyModel[30].setRotationPoint(0F, 0F, 0F);

		bodyModel[31].addShapeBox(2F, 8.7F, 2.25F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 54
		bodyModel[31].setRotationPoint(0F, 0F, 0F);

		bodyModel[32].addShapeBox(-4F, 8.7F, 2.25F, 2, 3, 1, 0F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, 0F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F, -0.5F, -1.1F, -0.25F); // Box 55
		bodyModel[32].setRotationPoint(0F, 0F, 0F);

		bodyModel[33].addShapeBox(0F, 0.9F, -2.9F, 4, 3, 1, 0F, 0F, 0F, 0F, -3F, 0F, 0F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, -0.9F, 0F, -3F, 0.1F, 0F, -3F, 1.1F, 0F, 0F, 0.1F, 0F); // Box 57
		bodyModel[33].setRotationPoint(0F, 0F, 0F);

		bodyModel[34].addShapeBox(-5F, 0.9F, -2.9F, 4, 3, 1, 0F, -1.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.75F, 0F, 0F, -1.75F, 0.1F, 0F, 0F, 0.1F, 0F, 0F, 1.1F, 0F, -1.75F, 1.1F, 0F); // Box 58
		bodyModel[34].setRotationPoint(0F, 0F, 0F);

		bodyModel[35].addShapeBox(-7F, 0.9F, -2.9F, 4, 3, 1, 0F, -2.99F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, -2.99F, 0F, 0F, -2.99F, -0.7F, 0F, -0.25F, 0.1F, 0F, -0.25F, 1.1F, 0F, -2.99F, 0.3F, 0F); // Box 60
		bodyModel[35].setRotationPoint(0F, 0F, 0F);

		bodyModel[36].addShapeBox(-0.75F, 0.9F, -2.9F, 4, 3, 1, 0F, -1.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.75F, 0F, 0F, -1.75F, 0.1F, 0F, 0F, 0.1F, 0F, 0F, 1.1F, 0F, -1.75F, 1.1F, 0F); // Box 61
		bodyModel[36].setRotationPoint(0F, 0F, 0F);

		bodyModel[37].addShapeBox(3F, 0.9F, -2.9F, 4, 3, 1, 0F, -0.25F, 0F, 0F, -2.99F, 0F, 0F, -2.99F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0.1F, 0F, -2.99F, -0.7F, 0F, -2.99F, 0.3F, 0F, -0.25F, 1.1F, 0F); // Box 62
		bodyModel[37].setRotationPoint(0F, 0F, 0F);

		bodyModel[38].addShapeBox(-4.5F, 11.2F, -2F, 3, 2, 4, 0F, -2F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, -2F, 0.1F, 0.4F, -2F, -2F, 0.4F, 0.25F, -0.5F, 0.4F, 0.25F, -0.5F, 0.4F, -2F, -2F, 0.4F); // Box 64
		bodyModel[38].setRotationPoint(0F, 0F, 0F);

		bodyModel[39].addShapeBox(1.5F, 11.2F, -2F, 3, 2, 4, 0F, 0.25F, 0.1F, 0.4F, -0.49F, 0.1F, 0.4F, -0.49F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, -1.25F, 0.4F, -0.49F, -2F, 0.4F, -0.49F, -2F, 0.4F, 0.25F, -1.25F, 0.4F); // Box 65
		bodyModel[39].setRotationPoint(0F, 0F, 0F);

		bodyModel[40].addShapeBox(1.5F, 11.2F, -2F, 3, 2, 4, 0F, 0.25F, 0.1F, 0.4F, -2F, 0.1F, 0.4F, -2F, 0.1F, 0.4F, 0.25F, 0.1F, 0.4F, 0.25F, -0.5F, 0.4F, -2F, -2F, 0.4F, -2F, -2F, 0.4F, 0.25F, -0.5F, 0.4F); // Box 66
		bodyModel[40].setRotationPoint(0F, 0F, 0F);

		bodyModel[41].addShapeBox(-2.75F, 7.6F, -2.66F, 3, 10, 1, 0F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F); // Box 67
		bodyModel[41].setRotationPoint(0F, 0F, 0F);

		bodyModel[42].addShapeBox(-0.25F, 7.6F, -2.66F, 3, 10, 1, 0F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, 0F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F, -1.25F, -9F, -0.25F); // Box 68
		bodyModel[42].setRotationPoint(0F, 0F, 0F);


		leftArmModel = new ModelRendererTurbo[5];
		leftArmModel[0] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 36
		leftArmModel[1] = new ModelRendererTurbo(this, 97, 33, textureX, textureY); // Box 38
		leftArmModel[2] = new ModelRendererTurbo(this, 25, 41, textureX, textureY); // Box 39
		leftArmModel[3] = new ModelRendererTurbo(this, 73, 41, textureX, textureY); // Box 41
		leftArmModel[4] = new ModelRendererTurbo(this, 97, 41, textureX, textureY); // Box 42

		leftArmModel[0].addShapeBox(-1F, -2.09F, -2F, 4, 12, 4, 0F, 0.1F, 0.0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F); // Box 36
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(2F, 7.5F, -2F, 1, 1, 4, 0F, -0.5F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, -0.5F, 0.25F, 0.3F, -0.5F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F, -0.5F, 0.5F, 0.3F); // Box 38
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);

		leftArmModel[2].addShapeBox(-1F, 5.6F, -2F, 4, 1, 4, 0F, 0.11F, 1.5F, 0.3F, 0.3F, 1.25F, 0.3F, 0.3F, 1.25F, 0.3F, 0.11F, 1.5F, 0.3F, 0.11F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F, 0.1F, 0.5F, 0.3F); // Box 39
		leftArmModel[2].setRotationPoint(0F, 0F, 0F);

		leftArmModel[3].addShapeBox(-1F, 1.8F, -2F, 4, 1, 4, 0F, 0.11F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, 0.11F, 0.25F, 0.3F, 0.11F, 1F, 0.3F, 0.3F, 1F, 0.3F, 0.3F, 1F, 0.3F, 0.11F, 1F, 0.3F); // Box 41
		leftArmModel[3].setRotationPoint(0F, 0F, 0F);

		leftArmModel[4].addShapeBox(-1F, -2.2F, -2F, 4, 3, 4, 0F, -0.4F, 0.25F, 0.5F, 0.6F, 0.25F, 0.5F, 0.6F, 0.25F, 0.5F, -0.4F, 0.25F, 0.5F, -0.4F, 0.25F, 0.5F, 0.6F, 1F, 0.5F, 0.6F, 1F, 0.5F, -0.4F, 0.25F, 0.5F); // Box 42
		leftArmModel[4].setRotationPoint(0F, 0F, 0F);


		rightArmModel = new ModelRendererTurbo[6];
		rightArmModel[0] = new ModelRendererTurbo(this, 105, 17, textureX, textureY); // Box 23
		rightArmModel[1] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 24
		rightArmModel[2] = new ModelRendererTurbo(this, 65, 25, textureX, textureY); // Box 25
		rightArmModel[3] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Box 27
		rightArmModel[4] = new ModelRendererTurbo(this, 89, 25, textureX, textureY); // Box 29
		rightArmModel[5] = new ModelRendererTurbo(this, 15, 80, textureX, textureY); // Box 40

		rightArmModel[0].addShapeBox(-3F, -2.09F, -2F, 4, 12, 4, 0F, 0.1F, 0.0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F, 0.1F, 0.15F, 0.1F); // Box 23
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-3F, -2.2F, -2F, 4, 3, 4, 0F, 0.6F, 0.25F, 0.5F, -0.4F, 0.25F, 0.5F, -0.4F, 0.25F, 0.5F, 0.6F, 0.25F, 0.5F, 0.6F, 1F, 0.5F, -0.4F, 0.25F, 0.5F, -0.4F, 0.25F, 0.5F, 0.6F, 1F, 0.5F); // Box 24
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);

		rightArmModel[2].addShapeBox(-3F, 1.8F, -2F, 4, 1, 4, 0F, 0.3F, 0.25F, 0.3F, 0.11F, 0.25F, 0.3F, 0.11F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, 0.3F, 1F, 0.3F, 0.11F, 1F, 0.3F, 0.1F, 1F, 0.3F, 0.3F, 1F, 0.3F); // Box 25
		rightArmModel[2].setRotationPoint(0F, 0F, 0F);

		rightArmModel[3].addShapeBox(-3F, 5.6F, -2F, 4, 1, 4, 0F, 0.3F, 1.5F, 0.3F, 0.11F, 1.25F, 0.3F, 0.11F, 1.25F, 0.3F, 0.3F, 1.5F, 0.3F, 0.3F, 0.5F, 0.3F, 0.11F, 0.5F, 0.3F, 0.11F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F); // Box 27
		rightArmModel[3].setRotationPoint(0F, 0F, 0F);

		rightArmModel[4].addShapeBox(-3F, 7.5F, -2F, 1, 1, 4, 0F, 0.3F, 0.25F, 0.3F, -0.5F, 0.25F, 0.3F, -0.5F, 0.25F, 0.3F, 0.3F, 0.25F, 0.3F, 0.3F, 0.5F, 0.3F, -0.5F, 0.5F, 0.3F, -0.5F, 0.5F, 0.3F, 0.3F, 0.5F, 0.3F); // Box 29
		rightArmModel[4].setRotationPoint(0F, 0F, 0F);

		rightArmModel[5].addShapeBox(-3.35F, 4.05F, -1F, 1, 10, 2, 0F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, -7F, -0.6F, 0F, -7F, -0.6F, 0F, -7F, -0.6F, 0F, -7F, -0.6F); // Box 40
		rightArmModel[5].setRotationPoint(0F, 0F, 0F);


	}
}