//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2025 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: highwayman
// Model Creator: 
// Created on: 19.02.2021 - 23:38:47
// Last changed on: 19.02.2021 - 23:38:47

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class Modelhighwayman extends ModelCustomArmour //Same as Filename
{
	int textureX = 128;
	int textureY = 128;

	public Modelhighwayman() //Same as Filename
	{
		headModel = new ModelRendererTurbo[7];
		bodyModel = new ModelRendererTurbo[25];
		leftArmModel = new ModelRendererTurbo[2];
		rightArmModel = new ModelRendererTurbo[2];
		leftLegModel = new ModelRendererTurbo[10];
		rightLegModel = new ModelRendererTurbo[10];

		initheadModel_1();
		initbodyModel_1();
		initleftArmModel_1();
		initrightArmModel_1();
		initleftLegModel_1();
		initrightLegModel_1();
	}

	private void initheadModel_1()
	{
		headModel[0] = new ModelRendererTurbo(this, 89, 57, textureX, textureY); // Box 622
		headModel[1] = new ModelRendererTurbo(this, 17, 65, textureX, textureY); // Box 623
		headModel[2] = new ModelRendererTurbo(this, 49, 65, textureX, textureY); // Box 624
		headModel[3] = new ModelRendererTurbo(this, 73, 73, textureX, textureY); // Box 626
		headModel[4] = new ModelRendererTurbo(this, 33, 49, textureX, textureY); // Box 627
		headModel[5] = new ModelRendererTurbo(this, 73, 65, textureX, textureY); // Box 628
		headModel[6] = new ModelRendererTurbo(this, 1, 73, textureX, textureY); // Box 629

		headModel[0].addShapeBox(-4F, -2.5F, -4F, 4, 3, 8, 0F,0.5F, -0.3F, 0.2F, 0F, 0.3F, 0.9F, 0F, 1F, 0.5F, 0.2F, 0.6F, 0.2F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.2F, 0F, 0F, 0F); // Box 622
		headModel[0].setRotationPoint(0F, 0F, 0F);

		headModel[1].addShapeBox(0F, -2.5F, -4F, 4, 3, 8, 0F,0F, 0.3F, 0.9F, 0.5F, -0.3F, 0.2F, 0.2F, 0.6F, 0.2F, 0F, 1F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.2F); // Box 623
		headModel[1].setRotationPoint(0F, 0F, 0F);

		headModel[2].addShapeBox(-4F, 0.5F, -4F, 4, 2, 8, 0F,0F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.2F, 0F, 0F, 0F, -0.8F, 0.2F, -0.8F, 0F, 1F, -0.3F, 0F, 0.3F, -0.8F, -0.8F, -0.2F, -0.8F); // Box 624
		headModel[2].setRotationPoint(0F, 0F, 0F);

		headModel[3].addShapeBox(0F, 0.5F, -4F, 4, 2, 8, 0F,0F, 0F, 0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.2F, 0F, 1F, -0.3F, -0.8F, 0.2F, -0.8F, -0.8F, -0.2F, -0.8F, 0F, 0.3F, -0.8F); // Box 626
		headModel[3].setRotationPoint(0F, 0F, 0F);

		headModel[4].addShapeBox(-1F, -3.5F, 4.3F, 2, 2, 2, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, -0.3F, -0.4F, 0F, -0.3F, -0.4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0.2F, -0.7F, -0.5F, 0.2F, -0.7F); // Box 627
		headModel[4].setRotationPoint(0F, 0F, 0F);

		headModel[5].addShapeBox(0F, -3F, 4.3F, 4, 1, 4, 0F,0F, 0F, 0F, -1F, 0F, -1F, 1.5F, -1F, 0.6F, -1.5F, -0.6F, -1.4F, 0F, 0F, 0F, -1F, 0.2F, -1.5F, 0.51F, 1F, 1F, -2F, 0.7F, -1F); // Box 628
		headModel[5].setRotationPoint(0F, 0F, 0F);

		headModel[6].addShapeBox(-4F, -3F, 4.3F, 4, 1, 4, 0F,-1F, 0F, -1F, 0F, 0F, 0F, -1.5F, -0.6F, -1.4F, 1.5F, -1F, 0.6F, -1F, 0.2F, -1.5F, 0F, 0F, 0F, -2F, 0.7F, -1F, 0.51F, 1F, 1F); // Box 629
		headModel[6].setRotationPoint(0F, 0F, 0F);
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 41, 9, textureX, textureY); // Box 574
		bodyModel[1] = new ModelRendererTurbo(this, 65, 9, textureX, textureY); // Box 575
		bodyModel[2] = new ModelRendererTurbo(this, 89, 9, textureX, textureY); // Box 576
		bodyModel[3] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 577
		bodyModel[4] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 578
		bodyModel[5] = new ModelRendererTurbo(this, 113, 1, textureX, textureY); // Box 579
		bodyModel[6] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 580
		bodyModel[7] = new ModelRendererTurbo(this, 33, 9, textureX, textureY); // Box 581
		bodyModel[8] = new ModelRendererTurbo(this, 57, 9, textureX, textureY); // Box 582
		bodyModel[9] = new ModelRendererTurbo(this, 81, 9, textureX, textureY); // Box 583
		bodyModel[10] = new ModelRendererTurbo(this, 105, 9, textureX, textureY); // Box 584
		bodyModel[11] = new ModelRendererTurbo(this, 113, 9, textureX, textureY); // Box 585
		bodyModel[12] = new ModelRendererTurbo(this, 73, 25, textureX, textureY); // Box 51
		bodyModel[13] = new ModelRendererTurbo(this, 113, 17, textureX, textureY); // Box 121
		bodyModel[14] = new ModelRendererTurbo(this, 121, 17, textureX, textureY); // Box 122
		bodyModel[15] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Box 129
		bodyModel[16] = new ModelRendererTurbo(this, 17, 25, textureX, textureY); // Box 607
		bodyModel[17] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Box 608
		bodyModel[18] = new ModelRendererTurbo(this, 17, 49, textureX, textureY); // Box 613
		bodyModel[19] = new ModelRendererTurbo(this, 41, 49, textureX, textureY); // Box 614
		bodyModel[20] = new ModelRendererTurbo(this, 65, 49, textureX, textureY); // Box 615
		bodyModel[21] = new ModelRendererTurbo(this, 81, 49, textureX, textureY); // Box 619
		bodyModel[22] = new ModelRendererTurbo(this, 97, 49, textureX, textureY); // Box 620
		bodyModel[23] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 621
		bodyModel[24] = new ModelRendererTurbo(this, 0, 81, textureX, textureY); // Box 630

		bodyModel[0].addShapeBox(-4F, 9F, -2F, 4, 3, 4, 0F,0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F); // Box 574
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addShapeBox(-4F, 9.5F, -2F, 4, 1, 4, 0F,0.6F, 0.1F, 0.6F, 0F, 0F, 0.6F, 0F, 0F, 0.6F, 0.6F, 0.1F, 0.6F, 0.6F, -0.3F, 0.6F, 0F, -0.2F, 0.6F, 0F, -0.2F, 0.6F, 0.6F, -0.3F, 0.6F); // Box 575
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addShapeBox(0F, 9F, -2F, 4, 3, 4, 0F,0F, -0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0.4F, 0.2F, 0.55F, 0F, -0.2F, 0.55F, 0F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0.4F); // Box 576
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(0F, 9.5F, -2F, 4, 1, 4, 0F,0F, 0F, 0.6F, 0.6F, 0.1F, 0.6F, 0.6F, 0.1F, 0.6F, 0F, 0F, 0.6F, 0F, -0.2F, 0.6F, 0.6F, -0.3F, 0.6F, 0.6F, -0.3F, 0.6F, 0F, -0.2F, 0.6F); // Box 577
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(-2.5F, 9.2F, -2F, 1, 1, 1, 0F,-0.1F, 0.25F, 0.8F, -0.1F, 0.2F, 0.8F, -0.1F, 0.2F, 0.6F, -0.1F, 0.25F, 0.6F, 0.1F, 0.5F, 0.8F, -0.3F, 0.6F, 0.8F, -0.3F, 0.6F, 0.6F, -0.1F, 0.5F, 0.6F); // Box 578
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(1.5F, 9.2F, -2F, 1, 1, 1, 0F,-0.1F, 0.2F, 0.8F, -0.1F, 0.25F, 0.8F, -0.1F, 0.25F, 0.6F, -0.1F, 0.2F, 0.6F, -0.3F, 0.6F, 0.8F, 0.1F, 0.5F, 0.8F, -0.1F, 0.5F, 0.6F, -0.3F, 0.6F, 0.6F); // Box 579
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(-1F, 8.8F, -2.7F, 2, 1, 1, 0F,0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F); // Box 580
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(-1F, 9.9F, -2.7F, 2, 1, 1, 0F,0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F); // Box 581
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(-1.3F, 8.85F, -2.7F, 1, 2, 1, 0F,-0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F); // Box 582
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(0.3F, 8.85F, -2.7F, 1, 2, 1, 0F,-0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F, -0.3F, -0.6F, 0F); // Box 583
		bodyModel[9].setRotationPoint(0F, 0F, 0F);

		bodyModel[10].addShapeBox(1.5F, 9.2F, 1F, 1, 1, 1, 0F,-0.1F, 0.2F, 0.6F, -0.1F, 0.25F, 0.6F, -0.1F, 0.25F, 0.8F, -0.1F, 0.2F, 0.8F, -0.3F, 0.6F, 0.6F, -0.1F, 0.5F, 0.6F, 0.1F, 0.5F, 0.8F, -0.3F, 0.6F, 0.8F); // Box 584
		bodyModel[10].setRotationPoint(0F, 0F, 0F);

		bodyModel[11].addShapeBox(-2.5F, 9.2F, 1F, 1, 1, 1, 0F,-0.1F, 0.25F, 0.6F, -0.1F, 0.2F, 0.6F, -0.1F, 0.2F, 0.8F, -0.1F, 0.25F, 0.8F, -0.1F, 0.5F, 0.6F, -0.3F, 0.6F, 0.6F, -0.3F, 0.6F, 0.8F, 0.1F, 0.5F, 0.8F); // Box 585
		bodyModel[11].setRotationPoint(0F, 0F, 0F);

		bodyModel[12].addShapeBox(0F, -0.2F, -2F, 4, 9, 4, 0F,0.5F, 0.25F, 0.5F, 0.7F, 0.1F, 0.5F, 0.7F, 0.1F, 0.5F, 0F, 0.2F, 0.5F, 0.5F, 0.7F, 0.4F, 0.3F, 0.5F, 0.4F, 0.3F, 0.5F, 0.4F, 0F, 0.7F, 0.4F); // Box 51
		bodyModel[12].setRotationPoint(0F, 0F, 0F);

		bodyModel[13].addShapeBox(-0.75F, 5F, -2.9F, 1, 1, 1, 0F,-0.7F, -0.2F, -0.300000000000001F, 0.2F, -0.2F, -0.300000000000002F, 0.2F, -0.2F, -0.300000000000001F, -0.7F, -0.2F, -0.300000000000001F, -0.7F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.300000000000001F, -0.7F, -0.3F, -0.300000000000001F); // Box 121
		bodyModel[13].setRotationPoint(0F, 0F, 0F);

		bodyModel[14].addShapeBox(-0.75F, 7F, -3.1F, 1, 1, 1, 0F,-0.7F, -0.2F, -0.300000000000001F, 0.2F, -0.2F, -0.300000000000002F, 0.2F, -0.2F, -0.300000000000001F, -0.7F, -0.2F, -0.300000000000001F, -0.7F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.300000000000001F, -0.7F, -0.3F, -0.300000000000001F); // Box 122
		bodyModel[14].setRotationPoint(0F, 0F, 0F);

		bodyModel[15].addShapeBox(-4F, -0.2F, -2F, 4, 9, 4, 0F,0.7F, 0.1F, 0.4F, 0.5F, 0.25F, 0.4F, 0F, 0.2F, 0.5F, 0.7F, 0.1F, 0.5F, 0.3F, 0.5F, 0.4F, 0.5F, 0.7F, 0.3F, 0F, 0.7F, 0.4F, 0.3F, 0.5F, 0.4F); // Box 129
		bodyModel[15].setRotationPoint(0F, 0F, 0F);

		bodyModel[16].addShapeBox(-0.75F, 3F, -2.9F, 1, 1, 1, 0F,-0.7F, -0.2F, -0.300000000000001F, 0.2F, -0.2F, -0.300000000000002F, 0.2F, -0.2F, -0.300000000000001F, -0.7F, -0.2F, -0.300000000000001F, -0.7F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.300000000000001F, -0.7F, -0.3F, -0.300000000000001F); // Box 607
		bodyModel[16].setRotationPoint(0F, 0F, 0F);

		bodyModel[17].addShapeBox(-0.75F, 1F, -2.9F, 1, 1, 1, 0F,-0.7F, -0.2F, -0.300000000000001F, 0.2F, -0.2F, -0.300000000000002F, 0.2F, -0.2F, -0.300000000000001F, -0.7F, -0.2F, -0.300000000000001F, -0.7F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.299999999999999F, 0.2F, -0.3F, -0.300000000000001F, -0.7F, -0.3F, -0.300000000000001F); // Box 608
		bodyModel[17].setRotationPoint(0F, 0F, 0F);

		bodyModel[18].addShapeBox(-5F, -1F, -3F, 3, 1, 6, 0F,0.3F, -0.4F, -0.5F, 0.1F, -0.4F, -0.5F, 1F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 0.5F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, 0.5F, 0F, 0F); // Box 613
		bodyModel[18].setRotationPoint(0F, 0F, 0F);

		bodyModel[19].addShapeBox(-6F, 0F, -3F, 4, 8, 5, 0F,-0.5F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, 0F, -0.5F, 0F, 0F, -0.8F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, -0.8F, 0F, 0F); // Box 614
		bodyModel[19].setRotationPoint(0F, 0F, 0F);

		bodyModel[20].addShapeBox(-6F, 0F, 2F, 6, 8, 1, 0F,-0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -1F, 0F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, -1F, 0F, 0F); // Box 615
		bodyModel[20].setRotationPoint(0F, 0F, 0F);

		bodyModel[21].addShapeBox(0F, 0F, 2F, 6, 8, 1, 0F,0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, -0.6F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, -0.6F, 0F); // Box 619
		bodyModel[21].setRotationPoint(0F, 0F, 0F);

		bodyModel[22].addShapeBox(2F, -1F, -3F, 3, 1, 6, 0F,0.1F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 0.3F, -0.4F, -0.5F, 1F, -0.4F, -0.5F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 2F, 0F, 0F); // Box 620
		bodyModel[22].setRotationPoint(0F, 0F, 0F);

		bodyModel[23].addShapeBox(2F, 0F, -3F, 4, 8, 5, 0F,0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 2F, 0F, 0F, 0F, -1F, 0F, -0.8F, 0F, 0F, -0.8F, 0F, 0F, 0F, -1F, 0F); // Box 621
		bodyModel[23].setRotationPoint(0F, 0F, 0F);

		bodyModel[24].addShapeBox(-6F, 9.5F, -1.5F, 2, 5, 3, 0F,0.3F, -0.4F, 0.3F, 0F, -0.6F, 0.3F, 0F, -0.2F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, -0.2F, -0.5F, 0.2F, -0.2F, -0.5F, -0.2F, -0.7F, 0.3F, -0.4F, -0.7F); // Box 630
		bodyModel[24].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftArmModel_1()
	{
		leftArmModel[0] = new ModelRendererTurbo(this, 97, 25, textureX, textureY); // Box 44
		leftArmModel[1] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 45

		leftArmModel[0].addShapeBox(-1F, -2F, -2F, 4, 8, 4, 0F,0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.2F, 1.1F, 0.2F, 0.3F, 0.9F, 0.2F, 0.3F, 0.9F, 0.2F, 0.2F, 1.1F, 0.2F); // Box 44
		leftArmModel[0].setRotationPoint(0F, 0F, 0F);

		leftArmModel[1].addShapeBox(-1F, 6.5F, -2F, 4, 1, 4, 0F,0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.2F, 0.6F, 0.2F, 0.3F, 0.4F, 0.2F, 0.3F, 0.4F, 0.2F, 0.2F, 0.6F, 0.2F); // Box 45
		leftArmModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightArmModel_1()
	{
		rightArmModel[0] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 130
		rightArmModel[1] = new ModelRendererTurbo(this, 49, 41, textureX, textureY); // Box 131

		rightArmModel[0].addShapeBox(-3F, -2F, -2F, 4, 8, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.9F, 0.2F, 0.2F, 1.1F, 0.2F, 0.2F, 1.1F, 0.2F, 0.3F, 0.9F, 0.2F); // Box 130
		rightArmModel[0].setRotationPoint(0F, 0F, 0F);

		rightArmModel[1].addShapeBox(-3F, 6.5F, -2F, 4, 1, 4, 0F,0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.3F, 0.1F, 0.1F, 0.1F, 0.1F, 0.3F, 0.4F, 0.2F, 0.2F, 0.6F, 0.2F, 0.2F, 0.6F, 0.2F, 0.3F, 0.4F, 0.2F); // Box 131
		rightArmModel[1].setRotationPoint(0F, 0F, 0F);
	}

	private void initleftLegModel_1()
	{
		leftLegModel[0] = new ModelRendererTurbo(this, 33, 17, textureX, textureY); // Box 586
		leftLegModel[1] = new ModelRendererTurbo(this, 57, 17, textureX, textureY); // Box 587
		leftLegModel[2] = new ModelRendererTurbo(this, 121, 9, textureX, textureY); // Box 588
		leftLegModel[3] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 589
		leftLegModel[4] = new ModelRendererTurbo(this, 73, 17, textureX, textureY); // Box 590
		leftLegModel[5] = new ModelRendererTurbo(this, 81, 17, textureX, textureY); // Box 591
		leftLegModel[6] = new ModelRendererTurbo(this, 89, 17, textureX, textureY); // Box 592
		leftLegModel[7] = new ModelRendererTurbo(this, 97, 17, textureX, textureY); // Box 593
		leftLegModel[8] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 594
		leftLegModel[9] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 595

		leftLegModel[0].addShapeBox(-2F, 6.8F, -2F, 4, 4, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 586
		leftLegModel[0].setRotationPoint(0F, 0F, 0F);

		leftLegModel[1].addShapeBox(-2F, 10.1F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.8F, 0.2F, -1.1F, 1.8F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 2F, 0.2F, 0F, 2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 587
		leftLegModel[1].setRotationPoint(0F, 0F, 0F);

		leftLegModel[2].addShapeBox(-0.5F, 11F, 2.5F, 1, 1, 2, 0F,-0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.8F, 0F, -0.2F, -0.8F, 0F); // Box 588
		leftLegModel[2].setRotationPoint(0F, 0F, 0F);

		leftLegModel[3].addShapeBox(-0.5F, 11F, 1.5F, 1, 1, 1, 0F,0.9F, -0.2F, 0F, 0.9F, -0.2F, 0F, -0.1F, -0.2F, 0.5F, -0.1F, -0.2F, 0.5F, 0.9F, -0.2F, 0F, 0.9F, -0.2F, 0F, -0.1F, -0.2F, 0.5F, -0.1F, -0.2F, 0.5F); // Box 589
		leftLegModel[3].setRotationPoint(0F, 0F, 0F);

		leftLegModel[4].addShapeBox(-0.5F, 10.5F, 3.3F, 1, 1, 2, 0F,-0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, -0.8F, 0F, -0.4F, -0.8F, 0F, -0.4F, -0.9F, 0F, -0.4F, -0.9F, 0F, -0.4F, 0.3F, -0.2F, -0.4F, 0.3F, -0.2F); // Box 590
		leftLegModel[4].setRotationPoint(0F, 0F, 0F);

		leftLegModel[5].addShapeBox(-0.5F, 10.5F, 3.3F, 1, 1, 2, 0F,-0.4F, -0.8F, 0F, -0.4F, -0.8F, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.3F, -0.2F, -0.4F, 0.3F, -0.2F, -0.4F, -0.9F, 0F, -0.4F, -0.9F, 0F); // Box 591
		leftLegModel[5].setRotationPoint(0F, 0F, 0F);

		leftLegModel[6].addShapeBox(-0.5F, 10.5F, 3.8F, 1, 1, 1, 0F,-0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F); // Box 592
		leftLegModel[6].setRotationPoint(0F, 0F, 0F);

		leftLegModel[7].addShapeBox(-2F, 10F, -2F, 4, 1, 4, 0F,0.3F, -0.2F, 0.35F, 0.3F, -0.2F, 0.35F, 0.3F, -1.2F, 0.5F, 0.3F, -1.2F, 0.5F, 0.3F, -0.2F, 0.6F, 0.3F, -0.2F, 0.6F, 0.3F, 0.8F, 0.5F, 0.3F, 0.8F, 0.5F); // Box 593
		leftLegModel[7].setRotationPoint(0F, 0F, 0F);

		leftLegModel[8].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 594
		leftLegModel[8].setRotationPoint(0F, 0F, 0F);

		leftLegModel[9].addShapeBox(-2F, 3.4F, -2F, 4, 5, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 595
		leftLegModel[9].setRotationPoint(0F, 0F, 0F);
	}

	private void initrightLegModel_1()
	{
		rightLegModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 125
		rightLegModel[1] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 126
		rightLegModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 564
		rightLegModel[3] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 566
		rightLegModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 567
		rightLegModel[5] = new ModelRendererTurbo(this, 57, 1, textureX, textureY); // Box 569
		rightLegModel[6] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 570
		rightLegModel[7] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 571
		rightLegModel[8] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 572
		rightLegModel[9] = new ModelRendererTurbo(this, 17, 9, textureX, textureY); // Box 573

		rightLegModel[0].addShapeBox(-2F, 6.8F, -2F, 4, 4, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 125
		rightLegModel[0].setRotationPoint(0F, 0F, 0F);

		rightLegModel[1].addShapeBox(-2F, 10.1F, -2F, 4, 2, 4, 0F,0.2F, -1.1F, 1.8F, 0.2F, -1.1F, 1.8F, 0.2F, 0.5F, 0.2F, 0.2F, 0.5F, 0.2F, 0.2F, 0F, 2F, 0.2F, 0F, 2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F); // Box 126
		rightLegModel[1].setRotationPoint(0F, 0F, 0F);

		rightLegModel[2].addShapeBox(-0.5F, 11F, 2.5F, 1, 1, 2, 0F,-0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, 0.3F, 0F, -0.2F, 0.3F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.2F, 0F, -0.2F, -0.8F, 0F, -0.2F, -0.8F, 0F); // Box 564
		rightLegModel[2].setRotationPoint(0F, 0F, 0F);

		rightLegModel[3].addShapeBox(-0.5F, 11F, 1.5F, 1, 1, 1, 0F,0.9F, -0.2F, 0F, 0.9F, -0.2F, 0F, -0.1F, -0.2F, 0.5F, -0.1F, -0.2F, 0.5F, 0.9F, -0.2F, 0F, 0.9F, -0.2F, 0F, -0.1F, -0.2F, 0.5F, -0.1F, -0.2F, 0.5F); // Box 566
		rightLegModel[3].setRotationPoint(0F, 0F, 0F);

		rightLegModel[4].addShapeBox(-0.5F, 10.5F, 3.3F, 1, 1, 2, 0F,-0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, -0.8F, 0F, -0.4F, -0.8F, 0F, -0.4F, -0.9F, 0F, -0.4F, -0.9F, 0F, -0.4F, 0.3F, -0.2F, -0.4F, 0.3F, -0.2F); // Box 567
		rightLegModel[4].setRotationPoint(0F, 0F, 0F);

		rightLegModel[5].addShapeBox(-0.5F, 10.5F, 3.3F, 1, 1, 2, 0F,-0.4F, -0.8F, 0F, -0.4F, -0.8F, 0F, -0.4F, 0.4F, -0.2F, -0.4F, 0.4F, -0.2F, -0.4F, 0.3F, -0.2F, -0.4F, 0.3F, -0.2F, -0.4F, -0.9F, 0F, -0.4F, -0.9F, 0F); // Box 569
		rightLegModel[5].setRotationPoint(0F, 0F, 0F);

		rightLegModel[6].addShapeBox(-0.5F, 10.5F, 3.8F, 1, 1, 1, 0F,-0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F, -0.4F, 0.7F, -0.25F); // Box 570
		rightLegModel[6].setRotationPoint(0F, 0F, 0F);

		rightLegModel[7].addShapeBox(-2F, 10F, -2F, 4, 1, 4, 0F,0.3F, -0.2F, 0.35F, 0.3F, -0.2F, 0.35F, 0.3F, -1.2F, 0.5F, 0.3F, -1.2F, 0.5F, 0.3F, -0.2F, 0.6F, 0.3F, -0.2F, 0.6F, 0.3F, 0.8F, 0.5F, 0.3F, 0.8F, 0.5F); // Box 571
		rightLegModel[7].setRotationPoint(0F, 0F, 0F);

		rightLegModel[8].addShapeBox(-2F, 0F, -2F, 4, 3, 4, 0F,0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F, 0.38F, 0.2F, 0.38F); // Box 572
		rightLegModel[8].setRotationPoint(0F, 0F, 0F);

		rightLegModel[9].addShapeBox(-2F, 3.4F, -2F, 4, 5, 4, 0F,0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0.4F, 0.2F, 0.4F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F); // Box 573
		rightLegModel[9].setRotationPoint(0F, 0F, 0F);
	}
}