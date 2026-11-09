//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2026 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: Bazz
// Model Creator: 
// Created on: 07.07.2015 - 15:16:48
// Last changed on: 07.07.2015 - 15:16:48

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelBazz extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public ModelBazz() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[36];
		leftArmModel = new ModelRendererTurbo[4];
		rightArmModel = new ModelRendererTurbo[4];
		leftLegModel = new ModelRendererTurbo[7];
		rightLegModel = new ModelRendererTurbo[7];

		initbodyModel_1();
		initleftArmModel_1();
		initrightArmModel_1();
		initleftLegModel_1();
		initrightLegModel_1();
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 65, 17, textureX, textureY); // Box 574
		bodyModel[1] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 575
		bodyModel[2] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 576
		bodyModel[3] = new ModelRendererTurbo(this, 65, 25, textureX, textureY); // Box 577
		bodyModel[4] = new ModelRendererTurbo(this, 113, 25, textureX, textureY); // Box 18
		bodyModel[5] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Box 19
		bodyModel[6] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 20
		bodyModel[7] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 22
		bodyModel[8] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 23
		bodyModel[9] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 24
		bodyModel[10] = new ModelRendererTurbo(this, 113, 1, textureX, textureY); // Box 25
		bodyModel[11] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 26
		bodyModel[12] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Box 27
		bodyModel[13] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 28
		bodyModel[14] = new ModelRendererTurbo(this, 49, 9, textureX, textureY); // Box 29
		bodyModel[15] = new ModelRendererTurbo(this, 65, 9, textureX, textureY); // Box 30
		bodyModel[16] = new ModelRendererTurbo(this, 97, 9, textureX, textureY); // Box 31
		bodyModel[17] = new ModelRendererTurbo(this, 113, 9, textureX, textureY); // Box 32
		bodyModel[18] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Box 33
		bodyModel[19] = new ModelRendererTurbo(this, 81, 17, textureX, textureY); // Box 34
		bodyModel[20] = new ModelRendererTurbo(this, 57, 25, textureX, textureY); // Box 35
		bodyModel[21] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 99
		bodyModel[22] = new ModelRendererTurbo(this, 73, 33, textureX, textureY); // Box 51
		bodyModel[23] = new ModelRendererTurbo(this, 97, 33, textureX, textureY); // Box 129
		bodyModel[24] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 613
		bodyModel[25] = new ModelRendererTurbo(this, 41, 41, textureX, textureY); // Box 614
		bodyModel[26] = new ModelRendererTurbo(this, 1, 49, textureX, textureY); // Box 615
		bodyModel[27] = new ModelRendererTurbo(this, 17, 49, textureX, textureY); // Box 619
		bodyModel[28] = new ModelRendererTurbo(this, 57, 49, textureX, textureY); // Box 620
		bodyModel[29] = new ModelRendererTurbo(this, 81, 49, textureX, textureY); // Box 109
		bodyModel[30] = new ModelRendererTurbo(this, 105, 49, textureX, textureY); // Box 110
		bodyModel[31] = new ModelRendererTurbo(this, 33, 57, textureX, textureY); // Box 111
		bodyModel[32] = new ModelRendererTurbo(this, 49, 57, textureX, textureY); // Box 112
		bodyModel[33] = new ModelRendererTurbo(this, 1, 65, textureX, textureY); // Box 113
		bodyModel[34] = new ModelRendererTurbo(this, 65, 65, textureX, textureY); // Box 114
		bodyModel[35] = new ModelRendererTurbo(this, 81, 65, textureX, textureY); // Box 115

		bodyModel[0].addShapeBox(-4F, 9F, -2F, 4, 3, 4, 0F,0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F); // Box 574
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(-4F, 9.5F, -2F, 4, 1, 4, 0F,0.6F, 0.1F, 0.6F, 0F, 0F, 0.6F, 0F, 0F, 0.6F, 0.6F, 0.1F, 0.6F, 0.6F, -0.3F, 0.6F, 0F, -0.2F, 0.6F, 0F, -0.2F, 0.6F, 0.6F, -0.3F, 0.6F); // Box 575
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(0F, 9F, -2F, 4, 3, 4, 0F,0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F); // Box 576
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(0F, 9.5F, -2F, 4, 1, 4, 0F,0F, 0F, 0.6F, 0.6F, 0.1F, 0.6F, 0.6F, 0.1F, 0.6F, 0F, 0F, 0.6F, 0F, -0.2F, 0.6F, 0.6F, -0.3F, 0.6F, 0.6F, -0.3F, 0.6F, 0F, -0.2F, 0.6F); // Box 577
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-4F, 2.2F, -2.21F, 4, 2, 2, 0F,-0.5F, 0.3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, -1.9F, 0.2F, 0F, -1.7F, 0.2F, -0.5F, -1.2F, -2F, -0.5F, -1.2F, -2F); // Box 18
		bodyModel[4].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[5].addShapeBox(-4F, 0.2F, -2.19F, 4, 2, 2, 0F,-0.2F, -0.7F, -0.9F, -0.3F, -1.5F, -0.3F, -1.5F, -0.5F, -0.8F, 0.5F, 0F, -0.5F, -0.5F, -0.3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F); // Box 19
		bodyModel[5].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[6].addShapeBox(-3.5F, 2.5F, -3.4F, 3, 1, 1, 0F,-0.2F, -0.2F, -0.5F, 0.1F, -0.4F, -0.5F, 0.5F, 0F, 0F, 0F, 0.2F, 0F, 0F, -0.1F, -0.5F, 0.3F, -0.1F, -0.5F, 0.5F, -0.5F, 0.2F, 0F, -0.5F, 0.2F); // Box 20
		bodyModel[6].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[7].addShapeBox(-3.3F, 4.39F, -3.9F, 3, 1, 1, 0F,0F, 0.5F, 0F, -0.4F, 0.5F, 0F, 0.1F, 1F, 0F, 0.2F, 1F, 0F, -0.2F, 0.1F, 0.3F, -0.9F, 0F, 0.3F, -0.5F, -0.1F, 0F, 0F, 0F, 0F); // Box 22
		bodyModel[7].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[8].addShapeBox(-3.5F, 3.29F, -3.4F, 3, 2, 1, 0F,0F, -0.1F, -0.5F, 0.3F, -0.1F, -0.5F, 0.5F, 0.3F, 0.2F, 0F, 0.3F, 0.2F, -0.2F, 0.1F, -0.5F, -0.3F, 0F, -0.5F, 0.5F, -0.5F, 0.2F, 0F, -0.5F, 0.2F); // Box 23
		bodyModel[8].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[9].addShapeBox(-3.3F, 2.4F, -4.9F, 3, 1, 1, 0F,-0.9F, -1.9F, 0F, -1.8F, -1.9F, 0F, -0.7F, -1F, 0F, -0.2F, -0.8F, 0F, -0.8F, 1.2F, 0F, -1.6F, 1.2F, 0F, -0.4F, 0.5F, 0F, 0F, 0.5F, 0F); // Box 24
		bodyModel[9].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[10].addShapeBox(-3.3F, 5.79F, -4.9F, 3, 1, 1, 0F,-0.8F, 1.2F, 0F, -1.6F, 1.2F, 0F, -0.4F, 1.9F, 0F, 0F, 1.9F, 0F, -0.9F, -1.9F, 0F, -1.8F, -1.9F, 0F, -0.9F, -1.4F, -0.3F, -0.2F, -1.3F, -0.3F); // Box 25
		bodyModel[10].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[11].addShapeBox(-3.5F, 3.29F, -2F, 3, 2, 1, 0F,0F, 0.3F, 0.2F, 0.5F, 0.3F, 0.2F, 0.3F, 1F, 0.5F, 0F, 1F, 0.5F, 0F, -0.5F, 0.2F, 0.5F, -0.5F, 0.2F, -0.1F, -1.6F, 0F, -0.2F, -1.6F, 0F); // Box 26
		bodyModel[11].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[12].addShapeBox(0.01F, 0.21F, -2.21F, 4, 2, 2, 0F,-0.3F, -1.5F, -0.3F, -0.2F, -0.7F, -0.9F, 0.5F, 0F, -0.5F, -1.5F, -0.5F, -0.8F, 0F, 0F, 0F, -0.5F, -0.3F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F); // Box 27
		bodyModel[12].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[13].addShapeBox(0F, 2.2F, -2.21F, 4, 2, 2, 0F,0F, 0F, 0F, -0.5F, 0.3F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, -1.7F, 0.2F, -0.5F, -1.9F, 0.2F, -0.5F, -1.2F, -2F, -0.5F, -1.2F, -2F); // Box 28
		bodyModel[13].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[14].addShapeBox(0.5F, 2.5F, -3.4F, 3, 1, 1, 0F,0.1F, -0.4F, -0.5F, -0.2F, -0.2F, -0.5F, 0F, 0.2F, 0F, 0.5F, 0F, 0F, 0.3F, -0.1F, -0.5F, 0F, -0.1F, -0.5F, 0F, -0.5F, 0.2F, 0.5F, -0.5F, 0.2F); // Box 29
		bodyModel[14].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[15].addShapeBox(0.3F, 2.4F, -3.89F, 3, 1, 1, 0F,-0.7F, -1F, 0F, -0.2F, -0.8F, 0F, 0F, -0.3F, 0F, -0.1F, -0.5F, 0F, -0.4F, 0.5F, 0F, 0F, 0.5F, 0F, 0.2F, 0F, 0F, 0.1F, 0F, 0F); // Box 30
		bodyModel[15].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[16].addShapeBox(0.3F, 2.4F, -4.9F, 3, 1, 1, 0F,-1.8F, -1.9F, 0F, -0.9F, -1.9F, 0F, -0.2F, -0.8F, 0F, -0.7F, -1F, 0F, -1.6F, 1.2F, 0F, -0.8F, 1.2F, 0F, 0F, 0.5F, 0F, -0.4F, 0.5F, 0F); // Box 31
		bodyModel[16].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[17].addShapeBox(0.5F, 3.29F, -3.4F, 3, 2, 1, 0F,0.3F, -0.1F, -0.5F, 0F, -0.1F, -0.5F, 0F, 0.3F, 0.2F, 0.5F, 0.3F, 0.2F, -0.3F, 0F, -0.5F, -0.2F, 0.1F, -0.5F, 0F, -0.5F, 0.2F, 0.5F, -0.5F, 0.2F); // Box 32
		bodyModel[17].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[18].addShapeBox(0.3F, 4.39F, -3.9F, 3, 1, 1, 0F,-0.4F, 0.5F, 0F, 0F, 0.5F, 0F, 0.2F, 1F, 0F, 0.1F, 1F, 0F, -0.9F, 0F, 0.3F, -0.2F, 0.1F, 0.3F, 0F, 0F, 0F, -0.5F, -0.1F, 0F); // Box 33
		bodyModel[18].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[19].addShapeBox(0.3F, 5.79F, -4.9F, 3, 1, 1, 0F,-1.6F, 1.2F, 0F, -0.8F, 1.2F, 0F, 0F, 1.9F, 0F, -0.4F, 1.9F, 0F, -1.8F, -1.9F, 0F, -0.9F, -1.9F, 0F, -0.2F, -1.3F, -0.3F, -0.9F, -1.4F, -0.3F); // Box 34
		bodyModel[19].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[20].addShapeBox(0.5F, 3.29F, -2F, 3, 2, 1, 0F,0.5F, 0.3F, 0.2F, 0F, 0.3F, 0.2F, 0F, 1F, 0.5F, 0.3F, 1F, 0.5F, 0.5F, -0.5F, 0.2F, 0F, -0.5F, 0.2F, -0.2F, -1.6F, 0F, -0.1F, -1.6F, 0F); // Box 35
		bodyModel[20].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[21].addShapeBox(-3.3F, 2.4F, -3.89F, 3, 1, 1, 0F,-0.2F, -0.8F, 0F, -0.7F, -1F, 0F, -0.1F, -0.5F, 0F, 0F, -0.3F, 0F, 0F, 0.5F, 0F, -0.4F, 0.5F, 0F, 0.1F, 0F, 0F, 0.2F, 0F, 0F); // Box 99
		bodyModel[21].setRotationPoint(0F, -0.5F, -0.5F);

		bodyModel[22].addShapeBox(0F, -0.2F, -2F, 4, 9, 4, 0F,-1.5F, 0.25F, 0.5F, 0.7F, 0.1F, 0.5F, 0.7F, 0.1F, 0.5F, 0F, 0.2F, 0.5F, 1.5F, 0.7F, 0.4F, 0.3F, 0.5F, 0.4F, 0.3F, 0.5F, 0.4F, 0F, 0.7F, 0.4F); // Box 51
		bodyModel[22].setRotationPoint(0F, 0F, 0F);

		bodyModel[23].addShapeBox(-4F, -0.2F, -2F, 4, 9, 4, 0F,0.7F, 0.1F, 0.4F, -1.5F, 0.25F, 0.4F, 0F, 0.2F, 0.5F, 0.7F, 0.1F, 0.5F, 0.3F, 0.5F, 0.4F, 2.5F, 0.7F, 0.3F, 0F, 0.7F, 0.4F, 0.3F, 0.5F, 0.4F); // Box 129
		bodyModel[23].setRotationPoint(0F, 0F, 0F);

		bodyModel[24].addShapeBox(-5F, -1F, -3F, 3, 1, 6, 0F,0.3F, -0.4F, -0.5F, 0.1F, -0.4F, -0.5F, 1F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 0.5F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 0.5F, 0F, 0F); // Box 613
		bodyModel[24].setRotationPoint(0F, 0F, 0F);

		bodyModel[25].addShapeBox(-6F, 0F, -3F, 4, 4, 5, 0F,-0.5F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, -0.5F, 0F, 0F, -0.8F, 0F, 1.5F, -0.2F, 0F, 3F, 0F, 0F, 0F, -0.6F, 0F, 0F); // Box 614
		bodyModel[25].setRotationPoint(0F, 0F, 0F);

		bodyModel[26].addShapeBox(-6F, 0F, 2F, 6, 8, 1, 0F,-0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -1F, 0F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, -1F, 0F, 0F); // Box 615
		bodyModel[26].setRotationPoint(0F, 0F, 0F);

		bodyModel[27].addShapeBox(0F, 0F, 2F, 6, 8, 1, 0F,0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, -0.6F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, -0.6F, 0F); // Box 619
		bodyModel[27].setRotationPoint(0F, 0F, 0F);

		bodyModel[28].addShapeBox(2F, -1F, -3F, 3, 1, 6, 0F,0.1F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 1F, -0.4F, -0.5F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 2F, 0F, 0F); // Box 620
		bodyModel[28].setRotationPoint(0F, 0F, 0F);

		bodyModel[29].addShapeBox(-6F, 4F, -3F, 4, 4, 5, 0F,-0.8F, 0F, 1.5F, -0.2F, 0F, 3F, 0F, 0F, 0F, -0.6F, 0F, 0F, -0.5F, 0F, 0F, -1F, 0F, 2F, 0F, 0F, 0F, -0.8F, 0F, 0F); // Box 109
		bodyModel[29].setRotationPoint(0F, 0F, 0F);

		bodyModel[30].addShapeBox(-6F, 4F, -3F, 4, 8, 3, 0F,-0.5F, 0F, 0F, -1F, 0F, 2F, 0F, 0F, 0F, -0.8F, 0F, 0F, 1.2F, 0.5F, 1.5F, -1.2F, 1F, 4F, -4F, -2F, 0F, 3.4F, 0F, 0F); // Box 110
		bodyModel[30].setRotationPoint(0F, 4F, 0F);

		bodyModel[31].addShapeBox(-6F, 4F, 0F, 4, 8, 3, 0F,-0.8F, 0F, 0F, 0F, 0F, 0F, 2F, 0.6F, 0F, -1F, 0F, 0F, 3.4F, 0F, 0F, -4F, -2F, 0F, 2F, -2F, 4F, 1.2F, 0F, 1.5F); // Box 111
		bodyModel[31].setRotationPoint(0F, 4F, 0F);

		bodyModel[32].addShapeBox(2F, 0F, -3F, 4, 4, 5, 0F,0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 2F, 0F, 0F, -0.2F, 0F, 3F, -0.8F, 0F, 1.5F, -0.6F, 0F, 0F, 0F, 0F, 0F); // Box 112
		bodyModel[32].setRotationPoint(0F, 0F, 0F);

		bodyModel[33].addShapeBox(2F, 4F, -3F, 4, 4, 5, 0F,-0.2F, 0F, 3F, -0.8F, 0F, 1.5F, -0.6F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 2F, -0.5F, 0F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F); // Box 113
		bodyModel[33].setRotationPoint(0F, 0F, 0F);

		bodyModel[34].addShapeBox(2F, 4F, -3F, 4, 8, 3, 0F,-1F, 0F, 2F, -0.5F, 0F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, -1.2F, 1F, 4F, 1.2F, 0.5F, 1.5F, 3.4F, 0F, 0F, -4F, -2F, 0F); // Box 114
		bodyModel[34].setRotationPoint(0F, 4F, 0F);

		bodyModel[35].addShapeBox(2F, 4F, 0F, 4, 8, 3, 0F,0F, 0F, 0F, -0.8F, 0F, 0F, -1F, 0F, 0F, 2F, 0.6F, 0F, -4F, -2F, 0F, 3.4F, 0F, 0F, 1.2F, 0F, 1.5F, 2F, -2F, 4F); // Box 115
		bodyModel[35].setRotationPoint(0F, 4F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 41, 73, textureX, textureY); // Box 100
		leftArmModel[1] = new ModelRendererTurbo(this, 97, 73, textureX, textureY); // Box 101
		leftArmModel[2] = new ModelRendererTurbo(this, 1, 81, textureX, textureY); // Box 44
		leftArmModel[3] = new ModelRendererTurbo(this, 25, 81, textureX, textureY); // Box 45

		leftArmModel[0].addShapeBox(0F, -1.5F, -2F, 4, 2, 4, 0F,0.5F, 2.3F, 1F, 0F, 2F, 1.3F, 0F, 2F, 1.4F, 0.5F, 2.3F, 1.4F, 2F, 0F, 1.5F, 0.5F, 0F, 1F, 0.5F, 0F, 1F, 2F, 0F, 1.5F); // Box 100
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(0F, 0.5F, -2F, 4, 2, 4, 0F,2F, 0F, 1.5F, 0.5F, 0F, 1F, 0.5F, 0F, 1F, 2F, 0F, 1.5F, 1F, 3.3F, 0.5F, -0.7F, 2F, 0.2F, -0.7F, 2F, 0.2F, 1F, 3.3F, 0.4F); // Box 101
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);

		leftArmModel[2].addShapeBox(-1F, -2F, -2F, 4, 8, 4, 0F,0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.2F, 1.1F, 0.2F, 0.3F, 0.9F, 0.2F, 0.3F, 0.9F, 0.2F, 0.2F, 1.1F, 0.2F); // Box 44
		leftArmModel[2].setRotationPoint(0F, 0F, 0F);

		leftArmModel[3].addShapeBox(-1F, 6.5F, -2F, 4, 1, 4, 0F,0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.2F, 0.6F, 0.2F, 0.3F, 0.4F, 0.2F, 0.3F, 0.4F, 0.2F, 0.2F, 0.6F, 0.2F); // Box 45
		leftArmModel[3].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 97, 65, textureX, textureY); // Box 95
		rightArmModel[1] = new ModelRendererTurbo(this, 17, 73, textureX, textureY); // Box 96
		rightArmModel[2] = new ModelRendererTurbo(this, 49, 81, textureX, textureY); // Box 130
		rightArmModel[3] = new ModelRendererTurbo(this, 73, 81, textureX, textureY); // Box 131

		rightArmModel[0].addShapeBox(-4F, -1.5F, -2F, 4, 2, 4, 0F,0F, 2F, 1.3F, 0.5F, 2.3F, 1F, 0.5F, 2.3F, 1.4F, 0F, 2F, 1.4F, 0.5F, 0F, 1F, 2F, 0F, 1.5F, 2F, 0F, 1.5F, 0.5F, 0F, 1F); // Box 95
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-4F, 0.5F, -2F, 4, 2, 4, 0F,0.5F, 0F, 1F, 2F, 0F, 1.5F, 2F, 0F, 1.5F, 0.5F, 0F, 1F, -0.7F, 2F, 0.2F, 1F, 3.3F, 0.5F, 1F, 3.3F, 0.4F, -0.7F, 2F, 0.2F); // Box 96
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);

		rightArmModel[2].addShapeBox(-3F, -2F, -2F, 4, 8, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.9F, 0.2F, 0.2F, 1.1F, 0.2F, 0.2F, 1.1F, 0.2F, 0.3F, 0.9F, 0.2F); // Box 130
		rightArmModel[2].setRotationPoint(0F, 0F, 0F);

		rightArmModel[3].addShapeBox(-3F, 6.5F, -2F, 4, 1, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.4F, 0.2F, 0.2F, 0.6F, 0.2F, 0.2F, 0.6F, 0.2F, 0.3F, 0.4F, 0.2F); // Box 131
		rightArmModel[3].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 586
		leftLegModel[1] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 587
		leftLegModel[2] = new ModelRendererTurbo(this, 17, 9, textureX, textureY); // Box 593
		leftLegModel[3] = new ModelRendererTurbo(this, 41, 9, textureX, textureY); // Box 70
		leftLegModel[4] = new ModelRendererTurbo(this, 57, 9, textureX, textureY); // Box 71
		leftLegModel[5] = new ModelRendererTurbo(this, 89, 25, textureX, textureY); // Box 594
		leftLegModel[6] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 595

		leftLegModel[0].addShapeBox(-2F, 6.8F, -2F, 4, 4, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 586
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 10.1F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.8F, 0.2F, -1.1F, 1.8F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 2F, 0.2F, 0F, 2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 587
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-2F, 10F, -2F, 4, 1, 4, 0F,0.3F, -0.2F, 0.35F, 0.3F, -0.2F, 0.35F, 0.3F, -1.2F, 0.5F, 0.3F, -1.2F, 0.5F, 0.3F, -0.2F, 0.6F, 0.3F, -0.2F, 0.6F, 0.3F, 0.8F, 0.5F, 0.3F, 0.8F, 0.5F); // Box 593
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(2F, 6.8F, -2F, 1, 4, 4, 0F,-0.5F, 1F, 2F, 0F, 1F, 2F, -0.5F, -1F, -1F, 0F, -1F, -1F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F); // Box 70
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);

		leftLegModel[4].addShapeBox(-3F, 6.8F, -2F, 1, 4, 4, 0F,0F, 1F, 2F, -0.5F, 1F, 2F, 0F, -1F, -1F, -0.5F, -1F, -1F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F); // Box 71
		leftLegModel[4].setRotationPoint(0F, 0F, 0F);

		leftLegModel[5].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 594
		leftLegModel[5].setRotationPoint(0F, 0F, 0F);

		leftLegModel[6].addShapeBox(-2F, 3.4F, -2F, 4, 5, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 595
		leftLegModel[6].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 125
		rightLegModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 126
		rightLegModel[2] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 571
		rightLegModel[3] = new ModelRendererTurbo(this, 89, 9, textureX, textureY); // Box 72
		rightLegModel[4] = new ModelRendererTurbo(this, 105, 9, textureX, textureY); // Box 73
		rightLegModel[5] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 572
		rightLegModel[6] = new ModelRendererTurbo(this, 25, 17, textureX, textureY); // Box 573

		rightLegModel[0].addShapeBox(-2F, 6.8F, -2F, 4, 4, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 125
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 10.1F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.8F, 0.2F, -1.1F, 1.8F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 2F, 0.2F, 0F, 2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 126
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-2F, 10F, -2F, 4, 1, 4, 0F,0.3F, -0.2F, 0.35F, 0.3F, -0.2F, 0.35F, 0.3F, -1.2F, 0.5F, 0.3F, -1.2F, 0.5F, 0.3F, -0.2F, 0.6F, 0.3F, -0.2F, 0.6F, 0.3F, 0.8F, 0.5F, 0.3F, 0.8F, 0.5F); // Box 571
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-3F, 6.8F, -2F, 1, 4, 4, 0F,0F, 1F, 2F, -0.5F, 1F, 2F, 0F, -1F, -1F, -0.5F, -1F, -1F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F); // Box 72
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);

		rightLegModel[4].addShapeBox(2F, 6.8F, -2F, 1, 4, 4, 0F,-0.5F, 1F, 2F, 0F, 1F, 2F, -0.5F, -1F, -1F, 0F, -1F, -1F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F); // Box 73
		rightLegModel[4].setRotationPoint(0F, 0F, 0F);

		rightLegModel[5].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 572
		rightLegModel[5].setRotationPoint(0F, 0F, 0F);

		rightLegModel[6].addShapeBox(-2F, 3.4F, -2F, 4, 5, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 573
		rightLegModel[6].setRotationPoint(0F, 0F, 0F);
	}
}