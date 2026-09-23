//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2020 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: YourClassName
// Model Creator: 
// Created on: 23.05.2020 - 17:04:08
// Last changed on: 23.05.2020 - 17:04:08

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelDroideka extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelDroideka() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[81];
		bodyModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		bodyModel[1] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Import 
		bodyModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Import 
		bodyModel[3] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Import 
		bodyModel[4] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Import 
		bodyModel[5] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Import 
		bodyModel[6] = new ModelRendererTurbo(this, 89, 9, textureX, textureY); // Import 
		bodyModel[7] = new ModelRendererTurbo(this, 113, 9, textureX, textureY); // Import 
		bodyModel[8] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import 
		bodyModel[9] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 3
		bodyModel[10] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 6
		bodyModel[11] = new ModelRendererTurbo(this, 65, 17, textureX, textureY); // Box 7
		bodyModel[12] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 8
		bodyModel[13] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Box 9
		bodyModel[14] = new ModelRendererTurbo(this, 105, 25, textureX, textureY); // Box 10
		bodyModel[15] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 11
		bodyModel[16] = new ModelRendererTurbo(this, 49, 33, textureX, textureY); // Box 12
		bodyModel[17] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 13
		bodyModel[18] = new ModelRendererTurbo(this, 121, 25, textureX, textureY); // Box 15
		bodyModel[19] = new ModelRendererTurbo(this, 65, 33, textureX, textureY); // Box 16
		bodyModel[20] = new ModelRendererTurbo(this, 81, 33, textureX, textureY); // Box 17
		bodyModel[21] = new ModelRendererTurbo(this, 9, 41, textureX, textureY); // Box 18
		bodyModel[22] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 19
		bodyModel[23] = new ModelRendererTurbo(this, 81, 1, textureX, textureY); // Box 20
		bodyModel[24] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 21
		bodyModel[25] = new ModelRendererTurbo(this, 81, 17, textureX, textureY); // Box 22
		bodyModel[26] = new ModelRendererTurbo(this, 113, 1, textureX, textureY); // Box 23
		bodyModel[27] = new ModelRendererTurbo(this, 33, 41, textureX, textureY); // Box 24
		bodyModel[28] = new ModelRendererTurbo(this, 57, 41, textureX, textureY); // Box 25
		bodyModel[29] = new ModelRendererTurbo(this, 73, 41, textureX, textureY); // Box 27
		bodyModel[30] = new ModelRendererTurbo(this, 97, 41, textureX, textureY); // Box 28
		bodyModel[31] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 29
		bodyModel[32] = new ModelRendererTurbo(this, 25, 49, textureX, textureY); // Box 30
		bodyModel[33] = new ModelRendererTurbo(this, 49, 49, textureX, textureY); // Box 31
		bodyModel[34] = new ModelRendererTurbo(this, 73, 49, textureX, textureY); // Box 34
		bodyModel[35] = new ModelRendererTurbo(this, 97, 49, textureX, textureY); // Box 35
		bodyModel[36] = new ModelRendererTurbo(this, 113, 49, textureX, textureY); // Box 36
		bodyModel[37] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 37
		bodyModel[38] = new ModelRendererTurbo(this, 17, 57, textureX, textureY); // Box 38
		bodyModel[39] = new ModelRendererTurbo(this, 105, 9, textureX, textureY); // Box 39
		bodyModel[40] = new ModelRendererTurbo(this, 33, 57, textureX, textureY); // Box 40
		bodyModel[41] = new ModelRendererTurbo(this, 49, 57, textureX, textureY); // Box 41
		bodyModel[42] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 42
		bodyModel[43] = new ModelRendererTurbo(this, 25, 17, textureX, textureY); // Box 43
		bodyModel[44] = new ModelRendererTurbo(this, 105, 17, textureX, textureY); // Box 44
		bodyModel[45] = new ModelRendererTurbo(this, 17, 33, textureX, textureY); // Box 47
		bodyModel[46] = new ModelRendererTurbo(this, 57, 57, textureX, textureY); // Box 48
		bodyModel[47] = new ModelRendererTurbo(this, 81, 57, textureX, textureY); // Box 49
		bodyModel[48] = new ModelRendererTurbo(this, 1, 65, textureX, textureY); // Box 50
		bodyModel[49] = new ModelRendererTurbo(this, 17, 65, textureX, textureY); // Box 51
		bodyModel[50] = new ModelRendererTurbo(this, 41, 65, textureX, textureY); // Box 52
		bodyModel[51] = new ModelRendererTurbo(this, 89, 65, textureX, textureY); // Box 53
		bodyModel[52] = new ModelRendererTurbo(this, 121, 41, textureX, textureY); // Box 54
		bodyModel[53] = new ModelRendererTurbo(this, 65, 65, textureX, textureY); // Box 55
		bodyModel[54] = new ModelRendererTurbo(this, 73, 65, textureX, textureY); // Box 56
		bodyModel[55] = new ModelRendererTurbo(this, 113, 65, textureX, textureY); // Box 57
		bodyModel[56] = new ModelRendererTurbo(this, 121, 65, textureX, textureY); // Box 58
		bodyModel[57] = new ModelRendererTurbo(this, 9, 73, textureX, textureY); // Box 59
		bodyModel[58] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 60
		bodyModel[59] = new ModelRendererTurbo(this, 73, 57, textureX, textureY); // Box 61
		bodyModel[60] = new ModelRendererTurbo(this, 89, 41, textureX, textureY); // Box 62
		bodyModel[61] = new ModelRendererTurbo(this, 113, 41, textureX, textureY); // Box 63
		bodyModel[62] = new ModelRendererTurbo(this, 17, 49, textureX, textureY); // Box 64
		bodyModel[63] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 65
		bodyModel[64] = new ModelRendererTurbo(this, 17, 73, textureX, textureY); // Box 66
		bodyModel[65] = new ModelRendererTurbo(this, 25, 73, textureX, textureY); // Box 67
		bodyModel[66] = new ModelRendererTurbo(this, 33, 73, textureX, textureY); // Box 68
		bodyModel[67] = new ModelRendererTurbo(this, 33, 73, textureX, textureY); // Box 68
		bodyModel[68] = new ModelRendererTurbo(this, 73, 73, textureX, textureY); // Box 69
		bodyModel[69] = new ModelRendererTurbo(this, 97, 73, textureX, textureY); // Box 70
		bodyModel[70] = new ModelRendererTurbo(this, 17, 81, textureX, textureY); // Box 71
		bodyModel[71] = new ModelRendererTurbo(this, 1, 83, textureX, textureY); // Box 72
		bodyModel[72] = new ModelRendererTurbo(this, 1, 88, textureX, textureY); // Box 73
		bodyModel[73] = new ModelRendererTurbo(this, 1, 91, textureX, textureY); // Box 74
		bodyModel[74] = new ModelRendererTurbo(this, 41, 22, textureX, textureY); // Box 75
		bodyModel[75] = new ModelRendererTurbo(this, 41, 22, textureX, textureY); // Box 76
		bodyModel[76] = new ModelRendererTurbo(this, 41, 22, textureX, textureY); // Box 77
		bodyModel[77] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 78
		bodyModel[78] = new ModelRendererTurbo(this, 20, 86, textureX, textureY); // Box 79
		bodyModel[79] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 80
		bodyModel[80] = new ModelRendererTurbo(this, 33, 83, textureX, textureY); // Box 81

		bodyModel[0].addShapeBox(-3F, 10F, -4F, 2, 5, 7, 0F, 0F, 0F, 0F, 0.9F, 0F, 0F, 0.9F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.9F, 0F, 0F, 0.9F, 0F, 0F, 0F, 0F, 0F); // Import 
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addBox(-2F, -7F, 0F, 5, 16, 2, 0F); // Import 
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(-6F, 7F, 1.8F, 3, 4, 1, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Import 
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(-2F, -8F, -5F, 5, 1, 4, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F); // Import 
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-3F, -8F, -1F, 7, 1, 5, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, 0F, -1.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F); // Import 
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addBox(-7F, -1F, -3F, 15, 2, 2, 0F); // Import 
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(-1.5F, -1.5F, -4F, 4, 4, 4, 0F, 0.5F, 0F, -0.5F, 0.5F, 0F, -0.5F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F); // Import 
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-7F, 0F, -3F, 2, 8, 2, 0F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 3F, 0F, 3.6F, -3F, 0F, 3.6F, -3F, 0F, -4.4F, 3F, 0F, -4.4F); // Import 
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(-11F, 8F, -15F, 1, 1, 2, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F); // Import 
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-3F, 15F, -4F, 3, 1, 7, 0F, 0F, 0F, 0F, -0.1F, 0F, 0F, -0.1F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, -1F, -0.1F, 0F, -1F, -0.1F, 0F, -1F, -1F, 0F, -1F); // Box 3
		bodyModel[9].setRotationPoint(0F, 0F, 0F);

		bodyModel[10].addShapeBox(-10F, 15F, -7.4F, 2, 9, 2, 0F, 0F, 0F, 0F, -1.5F, 0F, -0.3F, -1.5F, 0F, 0.3F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, -0.3F, -1.5F, 0F, 0.3F, 0F, 0F, 0F); // Box 6
		bodyModel[10].setRotationPoint(0F, 0F, 0F);

		bodyModel[11].addShapeBox(-11F, 15F, -8F, 2, 9, 2, 0F, 0F, 0F, 0F, -1F, 0F, -0.6F, -1F, 0F, 0.6F, 0F, 0F, 0F, 0F, -7F, 0F, -1F, 0F, -0.6F, -1F, 0F, 0.6F, 0F, -7F, 0F); // Box 7
		bodyModel[11].setRotationPoint(0F, 0F, 0F);

		bodyModel[12].addShapeBox(-11F, 12F, -3F, 8, 2, 2, 0F, 0F, -2F, 5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, -5F, 0F, 2F, 5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, -5F); // Box 8
		bodyModel[12].setRotationPoint(0F, 0F, 0F);

		bodyModel[13].addShapeBox(-1F, 12F, 3F, 2, 2, 8, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 2F, 0F); // Box 9
		bodyModel[13].setRotationPoint(0F, 0F, 0F);

		bodyModel[14].addShapeBox(-1F, 15F, 9F, 2, 9, 2, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, -7F, 0F, 0F, -7F, 0F); // Box 10
		bodyModel[14].setRotationPoint(0F, 0F, 0F);

		bodyModel[15].addShapeBox(-1F, 15F, 9.5F, 2, 9, 2, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, 0F, -1.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, 0F, -1.5F); // Box 11
		bodyModel[15].setRotationPoint(0F, 0F, 0F);

		bodyModel[16].addShapeBox(6F, 0F, -3F, 2, 8, 2, 0F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, -3F, 0F, 3.6F, 3F, 0F, 3.6F, 3F, 0F, -4.4F, -3F, 0F, -4.4F); // Box 12
		bodyModel[16].setRotationPoint(0F, 0F, 0F);

		bodyModel[17].addShapeBox(-3F, -1F, 3F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.75F, 0F, 0F, 0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F); // Box 13
		bodyModel[17].setRotationPoint(0F, 0F, 0F);

		bodyModel[18].addShapeBox(-3F, -1F, 2F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 15
		bodyModel[18].setRotationPoint(0F, 0F, 0F);

		bodyModel[19].addShapeBox(-3F, -7F, 3F, 7, 3, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.75F, 0F, 0F, 0.75F); // Box 16
		bodyModel[19].setRotationPoint(0F, 0F, 0F);

		bodyModel[20].addShapeBox(-3F, -9F, -1F, 7, 1, 5, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, 0F, -1.5F); // Box 17
		bodyModel[20].setRotationPoint(0F, 0F, 0F);

		bodyModel[21].addShapeBox(-3F, -9F, -5F, 7, 1, 4, 0F, -1F, -1F, 0F, -1F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 18
		bodyModel[21].setRotationPoint(0F, 0F, 0F);

		bodyModel[22].addShapeBox(-7F, 8F, 1.8F, 1, 3, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F); // Box 19
		bodyModel[22].setRotationPoint(0F, 0F, 0F);

		bodyModel[23].addShapeBox(-7F, 7F, 1.8F, 1, 1, 1, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 20
		bodyModel[23].setRotationPoint(0F, 0F, 0F);

		bodyModel[24].addShapeBox(7F, 7F, 1.8F, 1, 1, 1, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 21
		bodyModel[24].setRotationPoint(0F, 0F, 0F);

		bodyModel[25].addShapeBox(4F, 7F, 1.8F, 3, 4, 1, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 22
		bodyModel[25].setRotationPoint(0F, 0F, 0F);

		bodyModel[26].addShapeBox(7F, 8F, 1.8F, 1, 3, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F); // Box 23
		bodyModel[26].setRotationPoint(0F, 0F, 0F);

		bodyModel[27].addShapeBox(-1.5F, -5.5F, -1F, 4, 4, 2, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 2F, 0F, 0F, -3F, 0F, 0F, -3F); // Box 24
		bodyModel[27].setRotationPoint(0F, 0F, 0F);

		bodyModel[28].addShapeBox(-1.5F, -8F, -8F, 4, 1, 3, 0F, -1.25F, -1.5F, 0F, -1.25F, -1.5F, 0F, -1.25F, 0F, 0F, -1.25F, 0F, 0F, -1.25F, 1.5F, 0F, -1.25F, 1.5F, 0F, -1.25F, 0F, 0F, -1.25F, 0F, 0F); // Box 25
		bodyModel[28].setRotationPoint(0F, 0F, 0F);

		bodyModel[29].addShapeBox(-2.5F, -6F, -11F, 6, 1, 3, 0F, -2F, -0.5F, 0F, -2F, -0.5F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, 0.5F, 0F, -2F, 0.5F, 0F, -2F, 0F, 0F, -2F, 0F, 0F); // Box 27
		bodyModel[29].setRotationPoint(0F, 0F, 0F);

		bodyModel[30].addShapeBox(-2.5F, -7F, -11F, 6, 1, 3, 0F, -1F, -0.5F, 0F, -1F, -0.5F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, 0.5F, 0F, -1F, 0.5F, 0F, -1F, 0F, 0F, -1F, 0F, 0F); // Box 28
		bodyModel[30].setRotationPoint(0F, 0F, 0F);

		bodyModel[31].addShapeBox(-2.5F, -6.5F, -14F, 6, 1, 3, 0F, -2F, -0.5F, -2.5F, -2F, -0.5F, -2.5F, -1F, 0F, 0F, -1F, 0F, 0F, -2F, 0.5F, -2.5F, -2F, 0.5F, -2.5F, -1F, 0F, 0F, -1F, 0F, 0F); // Box 29
		bodyModel[31].setRotationPoint(0F, 0F, 0F);

		bodyModel[32].addShapeBox(-2.5F, -6F, -14.5F, 6, 1, 3, 0F, -2F, -1F, -1.5F, -2F, -1F, -1.5F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, 0.5F, -1.5F, -2F, 0.5F, -1.5F, -2F, 0F, 0F, -2F, 0F, 0F); // Box 30
		bodyModel[32].setRotationPoint(0F, 0F, 0F);

		bodyModel[33].addShapeBox(-2.5F, -5.5F, -13F, 6, 1, 2, 0F, -2F, -0.5F, -1.5F, -2F, -0.5F, -1.5F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, -0.5F, -1.5F, -2F, -0.5F, -1.5F, -2F, 0F, 0F, -2F, 0F, 0F); // Box 31
		bodyModel[33].setRotationPoint(0F, 0F, 0F);

		bodyModel[34].addShapeBox(4F, 12F, -3F, 8, 2, 2, 0F, 0F, 0F, 0F, 0F, -2F, 5F, 0F, -2F, -5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 5F, 0F, 2F, -5F, 0F, 0F, 0F); // Box 34
		bodyModel[34].setRotationPoint(0F, 0F, 0F);

		bodyModel[35].addShapeBox(10F, 15F, -8F, 2, 9, 2, 0F, -1F, 0F, -0.6F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0.6F, -1F, 0F, -0.6F, 0F, -7F, 0F, 0F, -7F, 0F, -1F, 0F, 0.6F); // Box 35
		bodyModel[35].setRotationPoint(0F, 0F, 0F);

		bodyModel[36].addShapeBox(10.5F, 15F, -7.1F, 2, 9, 2, 0F, 0F, 0F, 0F, -1.5F, 0F, 0.3F, -1.5F, 0F, -0.3F, 0F, 0F, 0F, 0F, 0F, 0F, -1.5F, 0F, 0.3F, -1.5F, 0F, -0.3F, 0F, 0F, 0F); // Box 36
		bodyModel[36].setRotationPoint(0F, 0F, 0F);

		bodyModel[37].addShapeBox(-11F, 7F, -9F, 3, 1, 3, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 37
		bodyModel[37].setRotationPoint(0F, 0F, 0F);

		bodyModel[38].addShapeBox(-11F, 8F, -12F, 3, 1, 3, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 38
		bodyModel[38].setRotationPoint(0F, 0F, 0F);

		bodyModel[39].addShapeBox(-9F, 8F, -15F, 1, 1, 2, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F); // Box 39
		bodyModel[39].setRotationPoint(0F, 0F, 0F);

		bodyModel[40].addShapeBox(9F, 7F, -9F, 3, 1, 3, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 40
		bodyModel[40].setRotationPoint(0F, 0F, 0F);

		bodyModel[41].addShapeBox(9F, 8F, -12F, 3, 1, 3, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 41
		bodyModel[41].setRotationPoint(0F, 0F, 0F);

		bodyModel[42].addShapeBox(9F, 8F, -15F, 1, 1, 2, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F); // Box 42
		bodyModel[42].setRotationPoint(0F, 0F, 0F);

		bodyModel[43].addShapeBox(11F, 8F, -15F, 1, 1, 2, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0.5F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F); // Box 43
		bodyModel[43].setRotationPoint(0F, 0F, 0F);

		bodyModel[44].addShapeBox(-1.5F, -6.2F, -14F, 1, 1, 3, 0F, -0.3F, -1.5F, 0F, -0.3F, -1.5F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, 0.9F, 0F, -0.3F, 0.9F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F); // Box 44
		bodyModel[44].setRotationPoint(0F, 0F, 0F);

		bodyModel[45].addShapeBox(1.5F, -6.2F, -14F, 1, 1, 3, 0F, -0.3F, -1.5F, 0F, -0.3F, -1.5F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, 0.9F, 0F, -0.3F, 0.9F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F); // Box 47
		bodyModel[45].setRotationPoint(0F, 0F, 0F);

		bodyModel[46].addShapeBox(-2F, 16F, -3F, 5, 1, 5, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, -0.5F, -1F, -1F, -0.5F, -1F, -1F, -0.5F, -1F, -1F, -0.5F, -1F); // Box 48
		bodyModel[46].setRotationPoint(0F, 0F, 0F);

		bodyModel[47].addShapeBox(-9F, 2F, -7F, 2, 6, 2, 0F, -1.6F, 0F, -3.6F, 0.4F, 0F, -3.6F, 0.4F, -1F, 1.4F, -1.6F, -1F, 1.4F, 0.4F, 0F, 2.4F, -1.6F, 0F, 2.4F, -1.6F, 0F, -3.6F, 0.4F, 0F, -3.6F); // Box 49
		bodyModel[47].setRotationPoint(0F, 0F, 0F);

		bodyModel[48].addShapeBox(8F, 2F, -7F, 2, 6, 2, 0F, 0.4F, 0F, -3.6F, -1.6F, 0F, -3.6F, -1.6F, -1F, 1.4F, 0.4F, -1F, 1.4F, -1.6F, 0F, 2.4F, 0.4F, 0F, 2.4F, 0.4F, 0F, -3.6F, -1.6F, 0F, -3.6F); // Box 50
		bodyModel[48].setRotationPoint(0F, 0F, 0F);

		bodyModel[49].addShapeBox(-3F, -7F, -1F, 7, 2, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 3F, 0F, 0F, 3F, 0F); // Box 51
		bodyModel[49].setRotationPoint(0F, 0F, 0F);

		bodyModel[50].addShapeBox(-3F, -7F, 2F, 7, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 52
		bodyModel[50].setRotationPoint(0F, 0F, 0F);

		bodyModel[51].addShapeBox(-3F, -4F, 3F, 7, 3, 2, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.25F, 0F, 0F, -0.25F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.25F, 0F, 0F, -0.25F); // Box 53
		bodyModel[51].setRotationPoint(0F, 0F, 0F);

		bodyModel[52].addShapeBox(3F, -1F, 3F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.75F, 0F, 0F, 0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F); // Box 54
		bodyModel[52].setRotationPoint(0F, 0F, 0F);

		bodyModel[53].addShapeBox(3F, -1F, 2F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 55
		bodyModel[53].setRotationPoint(0F, 0F, 0F);

		bodyModel[54].addShapeBox(1F, -1F, 3F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.75F, 0F, 0F, 0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F); // Box 56
		bodyModel[54].setRotationPoint(0F, 0F, 0F);

		bodyModel[55].addShapeBox(1F, -1F, 2F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 57
		bodyModel[55].setRotationPoint(0F, 0F, 0F);

		bodyModel[56].addShapeBox(-1F, -1F, 3F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.75F, 0F, 0F, 0.75F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F); // Box 58
		bodyModel[56].setRotationPoint(0F, 0F, 0F);

		bodyModel[57].addShapeBox(-1F, -1F, 2F, 1, 7, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.75F, 0F, 0F, -0.75F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 59
		bodyModel[57].setRotationPoint(0F, 0F, 0F);

		bodyModel[58].addShapeBox(0F, -3.2F, -14F, 1, 1, 1, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F, -0.3F, -0.3F, 0F); // Box 60
		bodyModel[58].setRotationPoint(0F, 0F, 0F);

		bodyModel[59].addShapeBox(0F, -3.2F, -13F, 1, 1, 2, 0F, -0.4F, -0.4F, 0F, -0.4F, -0.4F, 0F, -0.4F, 0.6F, 0F, -0.4F, 0.6F, 0F, -0.4F, -0.4F, 0F, -0.4F, -0.4F, 0F, -0.4F, -1.3F, 0F, -0.4F, -1.3F, 0F); // Box 61
		bodyModel[59].setRotationPoint(0F, 0F, 0F);

		bodyModel[60].addShapeBox(0F, -4.2F, -11F, 1, 1, 1, 0F, -0.4F, -0.4F, 0F, -0.4F, -0.4F, 0F, -0.4F, 1.6F, 0F, -0.4F, 1.6F, 0F, -0.4F, -0.3F, 0F, -0.4F, -0.3F, 0F, -0.4F, -2.2F, 0F, -0.4F, -2.2F, 0F); // Box 62
		bodyModel[60].setRotationPoint(0F, 0F, 0F);

		bodyModel[61].addShapeBox(-11F, 8F, -13F, 1, 1, 1, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 63
		bodyModel[61].setRotationPoint(0F, 0F, 0F);

		bodyModel[62].addShapeBox(-9F, 8F, -13F, 1, 1, 1, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 64
		bodyModel[62].setRotationPoint(0F, 0F, 0F);

		bodyModel[63].addShapeBox(9F, 8F, -13F, 1, 1, 1, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 65
		bodyModel[63].setRotationPoint(0F, 0F, 0F);

		bodyModel[64].addShapeBox(11F, 8F, -13F, 1, 1, 1, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.1F, 0.1F, 0.5F, 0.1F, 0.1F, 0.5F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F); // Box 66
		bodyModel[64].setRotationPoint(0F, 0F, 0F);

		bodyModel[65].addShapeBox(-8.5F, -2F, -3F, 1, 4, 2, 0F, -1F, 0F, 0.5F, 1F, 0F, 0.5F, 1F, 0F, 0.5F, -1F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F); // Box 67
		bodyModel[65].setRotationPoint(0F, 0F, 0F);

		bodyModel[66].addShapeBox(8.5F, -2F, -3F, 1, 4, 2, 0F, 1F, 0F, 0.5F, -1F, 0F, 0.5F, -1F, 0F, 0.5F, 1F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F); // Box 68
		bodyModel[66].setRotationPoint(0F, 0F, 0F);

		bodyModel[67].addShapeBox(-3F, 9F, -4F, 7, 1, 7, 0F, -5F, 0F, -1F, -1F, 0F, -1F, -1F, 0F, -1F, -5F, 0F, -1F, -5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -5F, 0F, 0F); // Box 68
		bodyModel[67].setRotationPoint(0F, 0F, 0F);

		bodyModel[68].addShapeBox(-3F, 9F, -4F, 7, 1, 7, 0F, -1F, 0F, -1F, -5F, 0F, -1F, -5F, 0F, -1F, -1F, 0F, -1F, 0F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, 0F, 0F, 0F, 0F); // Box 69
		bodyModel[68].setRotationPoint(0F, 0F, 0F);

		bodyModel[69].addShapeBox(-1F, 9F, 0F, 3, 1, 2, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F); // Box 70
		bodyModel[69].setRotationPoint(0F, 0F, 0F);

		bodyModel[70].addShapeBox(-1F, 9F, -2F, 3, 1, 2, 0F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0F, 0F, 1F); // Box 71
		bodyModel[70].setRotationPoint(0F, 0F, 0F);

		bodyModel[71].addShapeBox(-1F, 8F, 1.8F, 3, 2, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 72
		bodyModel[71].setRotationPoint(0F, 0F, 0F);

		bodyModel[72].addShapeBox(2F, 8.5F, 1.8F, 2, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 73
		bodyModel[72].setRotationPoint(0F, 0F, 0F);

		bodyModel[73].addShapeBox(-3F, 8.5F, 1.8F, 2, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 74
		bodyModel[73].setRotationPoint(0F, 0F, 0F);

		bodyModel[74].addShapeBox(0F, -3.2F, -14.45F, 1, 1, 1, 0F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F); // Box 75
		bodyModel[74].setRotationPoint(0F, 0F, 0F);

		bodyModel[75].addShapeBox(1.5F, -5F, -14.45F, 1, 1, 1, 0F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.45F, -0.4F, -0.35F, -0.45F, -0.4F); // Box 76
		bodyModel[75].setRotationPoint(0F, 0F, 0F);

		bodyModel[76].addShapeBox(-1.5F, -5F, -14.45F, 1, 1, 1, 0F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.35F, -0.4F, -0.35F, -0.45F, -0.4F, -0.35F, -0.45F, -0.4F); // Box 77
		bodyModel[76].setRotationPoint(0F, 0F, 0F);

		bodyModel[77].addShapeBox(2F, 10F, -4F, 2, 5, 7, 0F, 0.9F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.9F, 0F, 0F, 0.9F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.9F, 0F, 0F); // Box 78
		bodyModel[77].setRotationPoint(0F, 0F, 0F);

		bodyModel[78].addShapeBox(-0.5F, 10F, -4F, 2, 5, 7, 0F, -0.4F, 0F, -0.2F, -0.4F, 0F, -0.2F, -0.4F, 0F, 0F, -0.4F, 0F, 0F, -0.4F, 0F, -0.2F, -0.4F, 0F, -0.2F, -0.4F, 0F, 0F, -0.4F, 0F, 0F); // Box 79
		bodyModel[78].setRotationPoint(0F, 0F, 0F);

		bodyModel[79].addShapeBox(1F, 15F, -4F, 3, 1, 7, 0F, -0.1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.1F, 0F, 0F, -0.1F, 0F, -1F, -1F, 0F, -1F, -1F, 0F, -1F, -0.1F, 0F, -1F); // Box 80
		bodyModel[79].setRotationPoint(0F, 0F, 0F);

		bodyModel[80].addShapeBox(-1F, 15F, -4F, 3, 1, 7, 0F, -0.9F, 0F, -0.2F, -0.9F, 0F, -0.2F, -0.9F, 0F, 0F, -0.9F, 0F, 0F, -0.9F, 0F, -1.2F, -0.9F, 0F, -1.2F, -0.9F, 0F, -1F, -0.9F, 0F, -1F); // Box 81
		bodyModel[80].setRotationPoint(0F, 0F, 0F);
		
		bodyModel[74].glow = true;
		bodyModel[75].glow = true;
		bodyModel[76].glow = true;
	}
}