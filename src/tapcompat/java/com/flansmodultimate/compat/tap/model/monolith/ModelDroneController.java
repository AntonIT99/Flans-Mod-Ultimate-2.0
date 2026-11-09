//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2024 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: DroneController
// Model Creator: 
// Created on: 04.09.2017 - 20:24:10
// Last changed on: 04.09.2017 - 20:24:10

package com.flansmodultimate.compat.tap.model.monolith; //Path where the model is located

import com.flansmod.client.model.ModelVehicle;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelDroneController extends ModelVehicle //Same as Filename
{
	int textureX = 128;
	int textureY = 256;

	public ModelDroneController() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[35];
		turretModel = new ModelRendererTurbo[22];
		barrelModel = new ModelRendererTurbo[18];

		initbodyModel_1();
		initturretModel_1();
		initbarrelModel_1();

		translateAll(0F, 0F, 0F);


		flipAll();
	}

	private void initbodyModel_1()
	{
		bodyModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 370
		bodyModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 371
		bodyModel[2] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 372
		bodyModel[3] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 373
		bodyModel[4] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 374
		bodyModel[5] = new ModelRendererTurbo(this, 65, 1, textureX, textureY); // Box 375
		bodyModel[6] = new ModelRendererTurbo(this, 81, 1, textureX, textureY); // Box 376
		bodyModel[7] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 377
		bodyModel[8] = new ModelRendererTurbo(this, 113, 1, textureX, textureY); // Box 378
		bodyModel[9] = new ModelRendererTurbo(this, 73, 9, textureX, textureY); // Box 379
		bodyModel[10] = new ModelRendererTurbo(this, 97, 1, textureX, textureY); // Box 380
		bodyModel[11] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 381
		bodyModel[12] = new ModelRendererTurbo(this, 25, 17, textureX, textureY); // Box 382
		bodyModel[13] = new ModelRendererTurbo(this, 9, 1, textureX, textureY); // Box 383
		bodyModel[14] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 384
		bodyModel[15] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 385
		bodyModel[16] = new ModelRendererTurbo(this, 73, 1, textureX, textureY); // Box 386
		bodyModel[17] = new ModelRendererTurbo(this, 89, 1, textureX, textureY); // Box 387
		bodyModel[18] = new ModelRendererTurbo(this, 81, 9, textureX, textureY); // Box 388
		bodyModel[19] = new ModelRendererTurbo(this, 1, 113, textureX, textureY); // Box 59
		bodyModel[20] = new ModelRendererTurbo(this, 105, 89, textureX, textureY); // Box 60
		bodyModel[21] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 61
		bodyModel[22] = new ModelRendererTurbo(this, 1, 97, textureX, textureY); // Box 62
		bodyModel[23] = new ModelRendererTurbo(this, 73, 97, textureX, textureY); // Box 63
		bodyModel[24] = new ModelRendererTurbo(this, 1, 177, textureX, textureY); // Box 64
		bodyModel[25] = new ModelRendererTurbo(this, 105, 89, textureX, textureY); // Box 65
		bodyModel[26] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 66
		bodyModel[27] = new ModelRendererTurbo(this, 1, 97, textureX, textureY); // Box 67
		bodyModel[28] = new ModelRendererTurbo(this, 105, 89, textureX, textureY); // Box 68
		bodyModel[29] = new ModelRendererTurbo(this, 105, 89, textureX, textureY); // Box 69
		bodyModel[30] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 70
		bodyModel[31] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 71
		bodyModel[32] = new ModelRendererTurbo(this, 73, 97, textureX, textureY); // Box 72
		bodyModel[33] = new ModelRendererTurbo(this, 1, 177, textureX, textureY); // Box 73
		bodyModel[34] = new ModelRendererTurbo(this, 1, 177, textureX, textureY); // Box 74

		bodyModel[0].addBox(0F, 0F, 0F, 1, 10, 3, 0F); // Box 370
		bodyModel[0].setRotationPoint(-0.5F, -1.5F, -1.5F);

		bodyModel[1].addShapeBox(0F, 0F, 0F, 1, 10, 3, 0F,0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F); // Box 371
		bodyModel[1].setRotationPoint(0.5F, -1.5F, -1.5F);

		bodyModel[2].addShapeBox(0F, 0F, 0F, 1, 10, 3, 0F,0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F); // Box 372
		bodyModel[2].setRotationPoint(-1.5F, -1.5F, -1.5F);

		bodyModel[3].addShapeBox(0F, 0F, 0F, 1, 1, 10, 0F,0.2F, 1F, 0F, 0.2F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.8F, 0F, 0F, 0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 373
		bodyModel[3].setRotationPoint(-0.5F, 7F, 0F);

		bodyModel[4].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F); // Box 374
		bodyModel[4].setRotationPoint(-0.5F, 8.3F, 8F);

		bodyModel[5].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, -1F, 0.2F, 0F, -1F, 0.2F, 0F, -1F, 0.2F, 0F, -1F); // Box 375
		bodyModel[5].setRotationPoint(-0.5F, 9.3F, 8F);

		bodyModel[6].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F); // Box 376
		bodyModel[6].setRotationPoint(-0.5F, 7.3F, 8F);

		bodyModel[7].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F); // Box 377
		bodyModel[7].setRotationPoint(-0.5F, 8.2F, -11F);

		bodyModel[8].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, -1F, 0.2F, 0F, -1F, 0.2F, 0F, -1F, 0.2F, 0F, -1F); // Box 378
		bodyModel[8].setRotationPoint(-0.5F, 9.2F, -11F);

		bodyModel[9].addShapeBox(0F, 0F, 0F, 1, 1, 3, 0F,0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, -0.8F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F); // Box 379
		bodyModel[9].setRotationPoint(-0.5F, 7.2F, -11F);

		bodyModel[10].addShapeBox(0F, 0F, 0F, 1, 1, 10, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0.2F, 1F, 0F, 0.2F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.8F, 0F, 0F, 0.8F, 0F, 0F); // Box 380
		bodyModel[10].setRotationPoint(-0.5F, 7F, -10F);

		bodyModel[11].addShapeBox(0F, 0F, 0F, 10, 1, 1, 0F,0F, 0F, 0F, 0F, 1F, 0.2F, 0F, 1F, 0.2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.8F, 0F, 0F, 0.8F, 0F, 0F, 0F); // Box 381
		bodyModel[11].setRotationPoint(-10F, 7F, -0.5F);

		bodyModel[12].addShapeBox(0F, 0F, 0F, 10, 1, 1, 0F,0F, 1F, 0.2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F, 0.2F, 0F, 0F, 0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.8F); // Box 382
		bodyModel[12].setRotationPoint(0F, 7F, -0.5F);

		bodyModel[13].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,-0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F); // Box 383
		bodyModel[13].setRotationPoint(-11F, 7.3F, -0.5F);

		bodyModel[14].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F); // Box 384
		bodyModel[14].setRotationPoint(-11F, 8.3F, -0.5F);

		bodyModel[15].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F); // Box 385
		bodyModel[15].setRotationPoint(-11F, 9.3F, -0.5F);

		bodyModel[16].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,-0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, -0.8F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F, 0F, 0F, 0.3F); // Box 386
		bodyModel[16].setRotationPoint(8F, 7.3F, -0.5F);

		bodyModel[17].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F); // Box 387
		bodyModel[17].setRotationPoint(8F, 8.3F, -0.5F);

		bodyModel[18].addShapeBox(0F, 0F, 0F, 3, 1, 1, 0F,0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F, -1F, 0F, 0.2F); // Box 388
		bodyModel[18].setRotationPoint(8F, 9.3F, -0.5F);

		bodyModel[19].addShapeBox(0F, 0F, 0F, 16, 2, 16, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 59
		bodyModel[19].setRotationPoint(29F, -8F, -8F);

		bodyModel[20].addShapeBox(0F, 0F, 0F, 4, 4, 4, 0F,0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F); // Box 60
		bodyModel[20].setRotationPoint(29F, -10F, -8F);

		bodyModel[21].addShapeBox(0F, 0F, 0F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 61
		bodyModel[21].setRotationPoint(29F, -18F, -8F);

		bodyModel[22].addShapeBox(0F, 0F, 0F, 16, 1, 2, 0F,0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F); // Box 62
		bodyModel[22].setRotationPoint(29F, -19F, -8F);

		bodyModel[23].addShapeBox(0F, 0F, 0F, 1, 1, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 63
		bodyModel[23].setRotationPoint(30F, -19F, -6F);

		bodyModel[24].addShapeBox(0F, 0F, 0F, 16, 16, 16, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 64
		bodyModel[24].setRotationPoint(35F, -6F, 0F);
		bodyModel[24].rotateAngleY = -0.31415927F;

		bodyModel[25].addShapeBox(0F, 0F, 0F, 4, 4, 4, 0F,0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F); // Box 65
		bodyModel[25].setRotationPoint(29F, -10F, 4F);

		bodyModel[26].addShapeBox(0F, 0F, 0F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 66
		bodyModel[26].setRotationPoint(29F, -18F, 6F);

		bodyModel[27].addShapeBox(0F, 0F, 0F, 16, 1, 2, 0F,0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F, 0.5F, 0.2F, 0.5F); // Box 67
		bodyModel[27].setRotationPoint(29F, -19F, 6F);

		bodyModel[28].addShapeBox(0F, 0F, 0F, 4, 4, 4, 0F,0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F); // Box 68
		bodyModel[28].setRotationPoint(41F, -10F, 4F);

		bodyModel[29].addShapeBox(0F, 0F, 0F, 4, 4, 4, 0F,0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F, 0.2F); // Box 69
		bodyModel[29].setRotationPoint(41F, -10F, -8F);

		bodyModel[30].addShapeBox(0F, 0F, 0F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 70
		bodyModel[30].setRotationPoint(43F, -18F, 6F);

		bodyModel[31].addShapeBox(0F, 0F, 0F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 71
		bodyModel[31].setRotationPoint(43F, -18F, -8F);

		bodyModel[32].addShapeBox(0F, 0F, 0F, 1, 1, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 72
		bodyModel[32].setRotationPoint(43F, -19F, -6F);

		bodyModel[33].addShapeBox(0F, 0F, 0F, 16, 16, 16, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 73
		bodyModel[33].setRotationPoint(29F, -6F, -21F);
		bodyModel[33].rotateAngleY = 0.08726646F;

		bodyModel[34].addShapeBox(0F, 0F, 0F, 16, 16, 16, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 74
		bodyModel[34].setRotationPoint(25F, -6F, -5F);
		bodyModel[34].rotateAngleY = 0.50614548F;
	}

	private void initturretModel_1()
	{
		turretModel[0] = new ModelRendererTurbo(this, 41, 17, textureX, textureY); // Box 389
		turretModel[1] = new ModelRendererTurbo(this, 89, 17, textureX, textureY); // Box 390
		turretModel[2] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 391
		turretModel[3] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Box 392
		turretModel[4] = new ModelRendererTurbo(this, 73, 33, textureX, textureY); // Box 393
		turretModel[5] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 394
		turretModel[6] = new ModelRendererTurbo(this, 25, 49, textureX, textureY); // Box 395
		turretModel[7] = new ModelRendererTurbo(this, 65, 49, textureX, textureY); // Box 396
		turretModel[8] = new ModelRendererTurbo(this, 1, 57, textureX, textureY); // Box 397
		turretModel[9] = new ModelRendererTurbo(this, 81, 17, textureX, textureY); // Box 398
		turretModel[10] = new ModelRendererTurbo(this, 113, 17, textureX, textureY); // Box 399
		turretModel[11] = new ModelRendererTurbo(this, 1, 25, textureX, textureY); // Box 400
		turretModel[12] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 401
		turretModel[13] = new ModelRendererTurbo(this, 97, 41, textureX, textureY); // Box 402
		turretModel[14] = new ModelRendererTurbo(this, 49, 57, textureX, textureY); // Box 403
		turretModel[15] = new ModelRendererTurbo(this, 25, 65, textureX, textureY); // Box 404
		turretModel[16] = new ModelRendererTurbo(this, 65, 65, textureX, textureY); // Box 405
		turretModel[17] = new ModelRendererTurbo(this, 97, 65, textureX, textureY); // Box 406
		turretModel[18] = new ModelRendererTurbo(this, 1, 73, textureX, textureY); // Box 407
		turretModel[19] = new ModelRendererTurbo(this, 41, 73, textureX, textureY); // Box 408
		turretModel[20] = new ModelRendererTurbo(this, 81, 73, textureX, textureY); // Box 409
		turretModel[21] = new ModelRendererTurbo(this, 17, 81, textureX, textureY); // Box 410

		turretModel[0].addBox(0F, 0F, 0F, 9, 1, 14, 0F); // Box 389
		turretModel[0].setRotationPoint(-4.5F, -2.5F, -7F);

		turretModel[1].addShapeBox(0F, 0F, 0F, 1, 1, 14, 0F,0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F); // Box 390
		turretModel[1].setRotationPoint(4.5F, -2.5F, -7F);

		turretModel[2].addShapeBox(0F, 0F, 0F, 1, 1, 14, 0F,0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F); // Box 391
		turretModel[2].setRotationPoint(-5.5F, -2.5F, -7F);

		turretModel[3].addShapeBox(0F, 0F, 0F, 7, 1, 14, 0F,0F, 0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.5F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, -0.5F); // Box 392
		turretModel[3].setRotationPoint(-3.5F, -3.5F, -7F);

		turretModel[4].addShapeBox(0F, 0F, 0F, 2, 1, 14, 0F,0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, -0.5F, -0.5F, 0F, -1.5F, -0.5F, 0F, -1.5F, 0F, 0F, -0.5F); // Box 393
		turretModel[4].setRotationPoint(3.5F, -3.5F, -7F);

		turretModel[5].addShapeBox(0F, 0F, 0F, 2, 1, 14, 0F,0F, 0.5F, -1F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, -1F, -0.5F, 0F, -1.5F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, -0.5F, 0F, -1.5F); // Box 394
		turretModel[5].setRotationPoint(-5.5F, -3.5F, -7F);

		turretModel[6].addShapeBox(0F, 0F, 0F, 2, 1, 14, 0F,-0.5F, -0.5F, -2F, 1F, 0F, -1F, 1F, 0F, -1F, -0.5F, -0.5F, -2F, 0F, 0.5F, -1F, 0F, 0.5F, 0F, 0F, 0.5F, 0F, 0F, 0.5F, -1F); // Box 395
		turretModel[6].setRotationPoint(-5.5F, -5.5F, -7F);

		turretModel[7].addShapeBox(0F, 0F, 0F, 7, 1, 14, 0F,-1F, 0F, -1F, -1F, -0.5F, -1F, -1F, -0.5F, -1F, -1F, 0F, -1F, 0F, 0.5F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 0.5F, 0F); // Box 396
		turretModel[7].setRotationPoint(-3.5F, -5.5F, -7F);

		turretModel[8].addShapeBox(0F, 0F, 0F, 2, 1, 14, 0F,1F, 0.5F, -1F, -1F, 0.2F, -2F, -1F, 0.2F, -2F, 1F, 0.5F, -1F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F); // Box 397
		turretModel[8].setRotationPoint(3.5F, -4.5F, -7F);

		turretModel[9].addShapeBox(0F, 0F, 0F, 2, 1, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 398
		turretModel[9].setRotationPoint(-6.5F, -4F, -2F);
		turretModel[9].rotateAngleZ = -0.61086524F;

		turretModel[10].addShapeBox(0F, 0F, 0F, 1, 4, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 399
		turretModel[10].setRotationPoint(-8F, -7F, -2F);
		turretModel[10].rotateAngleZ = 0.2443461F;

		turretModel[11].addShapeBox(0F, 0F, 0F, 1, 6, 4, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 400
		turretModel[11].setRotationPoint(-8F, -13F, -2F);
		turretModel[11].rotateAngleZ = 0.01745329F;

		turretModel[12].addShapeBox(0F, 0F, 0F, 1, 2, 6, 0F,0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F, 0.2F, 0F, 0F); // Box 401
		turretModel[12].setRotationPoint(-8F, -14F, -3F);
		turretModel[12].rotateAngleZ = 0.01745329F;

		turretModel[13].addShapeBox(0F, 0F, 0F, 1, 6, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 402
		turretModel[13].setRotationPoint(-7.5F, -16F, -6F);

		turretModel[14].addShapeBox(0F, 0F, 0F, 1, 2, 12, 0F,0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 403
		turretModel[14].setRotationPoint(-7.5F, -18F, -6F);

		turretModel[15].addShapeBox(0F, 0F, 0F, 1, 1, 12, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1F); // Box 404
		turretModel[15].setRotationPoint(-7.5F, -10F, -6F);

		turretModel[16].addShapeBox(0F, 0F, 0F, 1, 6, 12, 0F,0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F); // Box 405
		turretModel[16].setRotationPoint(-6.5F, -16F, -6F);

		turretModel[17].addShapeBox(0F, 0F, 0F, 1, 2, 12, 0F,0F, -0.5F, -2F, 0F, 0F, -2F, 0F, 0F, -2F, 0F, -0.5F, -2F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F); // Box 406
		turretModel[17].setRotationPoint(-6.5F, -18F, -6F);

		turretModel[18].addShapeBox(0F, 0F, 0F, 1, 1, 12, 0F,0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, -0.5F, -1F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, -0.5F, -1F); // Box 407
		turretModel[18].setRotationPoint(-6.5F, -10F, -6F);

		turretModel[19].addShapeBox(0F, 0F, 0F, 1, 2, 12, 0F,0F, 0F, -2F, 0F, -0.5F, -2F, 0F, -0.5F, -2F, 0F, 0F, -2F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F); // Box 408
		turretModel[19].setRotationPoint(-5.5F, -18F, -6F);

		turretModel[20].addShapeBox(0F, 0F, 0F, 1, 6, 12, 0F,0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F); // Box 409
		turretModel[20].setRotationPoint(-5.5F, -16F, -6F);

		turretModel[21].addShapeBox(0F, 0F, 0F, 1, 1, 12, 0F,0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, -0.5F, -1F, 0F, -0.5F, -1F, 0F, 0F, -1F); // Box 410
		turretModel[21].setRotationPoint(-5.5F, -10F, -6F);
	}

	private void initbarrelModel_1()
	{
		barrelModel[0] = new ModelRendererTurbo(this, 33, 89, textureX, textureY); // Box 411
		barrelModel[1] = new ModelRendererTurbo(this, 25, 89, textureX, textureY); // Box 412
		barrelModel[2] = new ModelRendererTurbo(this, 25, 25, textureX, textureY); // Box 413
		barrelModel[3] = new ModelRendererTurbo(this, 121, 9, textureX, textureY); // Box 414
		barrelModel[4] = new ModelRendererTurbo(this, 73, 33, textureX, textureY); // Box 415
		barrelModel[5] = new ModelRendererTurbo(this, 97, 33, textureX, textureY); // Box 416
		barrelModel[6] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 417
		barrelModel[7] = new ModelRendererTurbo(this, 81, 25, textureX, textureY); // Box 418
		barrelModel[8] = new ModelRendererTurbo(this, 121, 1, textureX, textureY); // Box 419
		barrelModel[9] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 420
		barrelModel[10] = new ModelRendererTurbo(this, 97, 17, textureX, textureY); // Box 421
		barrelModel[11] = new ModelRendererTurbo(this, 9, 25, textureX, textureY); // Box 422
		barrelModel[12] = new ModelRendererTurbo(this, 49, 25, textureX, textureY); // Box 423
		barrelModel[13] = new ModelRendererTurbo(this, 89, 25, textureX, textureY); // Box 424
		barrelModel[14] = new ModelRendererTurbo(this, 97, 25, textureX, textureY); // Box 425
		barrelModel[15] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 426
		barrelModel[16] = new ModelRendererTurbo(this, 105, 33, textureX, textureY); // Box 427
		barrelModel[17] = new ModelRendererTurbo(this, 113, 33, textureX, textureY); // Box 428

		barrelModel[0].addShapeBox(8F, -1F, -7F, 2, 8, 14, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 411
		barrelModel[0].setRotationPoint(0F, -19F, 0F);

		barrelModel[1].addShapeBox(7.5F, -15.5F, -21.5F, 1, 37, 43, 0F,-0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F, -0.2F, -15.2F, -15.4F); // Box 412
		barrelModel[1].setRotationPoint(0F, -19F, 0F);

		barrelModel[2].addShapeBox(8F, -1F, 7.2F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 413
		barrelModel[2].setRotationPoint(0F, -19F, 0F);

		barrelModel[3].addShapeBox(8F, -1F, 9.2F, 2, 8, 1, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F); // Box 414
		barrelModel[3].setRotationPoint(0F, -19F, 0F);

		barrelModel[4].addShapeBox(8F, -1F, -9.2F, 2, 8, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 415
		barrelModel[4].setRotationPoint(0F, -19F, 0F);

		barrelModel[5].addShapeBox(8F, -1F, -10.2F, 2, 8, 1, 0F,0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 416
		barrelModel[5].setRotationPoint(0F, -19F, 0F);

		barrelModel[6].addShapeBox(7.5F, 0.5F, 7.7F, 1, 2, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 417
		barrelModel[6].setRotationPoint(0F, -19F, 0F);

		barrelModel[7].addShapeBox(7.5F, 2.8F, -9.7F, 1, 2, 2, 0F,0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 418
		barrelModel[7].setRotationPoint(0F, -19F, 0F);

		barrelModel[8].addShapeBox(7.5F, 3F, 8.2F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 419
		barrelModel[8].setRotationPoint(0F, -19F, 0F);

		barrelModel[9].addShapeBox(7.5F, 4.6F, 8.2F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 420
		barrelModel[9].setRotationPoint(0F, -19F, 0F);

		barrelModel[10].addShapeBox(7.5F, 3.8F, 9F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 421
		barrelModel[10].setRotationPoint(0F, -19F, 0F);

		barrelModel[11].addShapeBox(7.5F, 3.8F, 7.4F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 422
		barrelModel[11].setRotationPoint(0F, -19F, 0F);

		barrelModel[12].addShapeBox(7.5F, -0.199999999999999F, -9.3F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 423
		barrelModel[12].setRotationPoint(0F, -19F, 0F);

		barrelModel[13].addShapeBox(7.5F, 0.600000000000001F, -8.5F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 424
		barrelModel[13].setRotationPoint(0F, -19F, 0F);

		barrelModel[14].addShapeBox(7.5F, 1.4F, -9.3F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 425
		barrelModel[14].setRotationPoint(0F, -19F, 0F);

		barrelModel[15].addShapeBox(7.5F, 0.600000000000001F, -10.1F, 1, 1, 1, 0F,-0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 426
		barrelModel[15].setRotationPoint(0F, -19F, 0F);

		barrelModel[16].addShapeBox(8F, -1F, 9.4F, 2, 2, 1, 0F,-0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, -1F, 0F, -0.3F, -1F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F); // Box 427
		barrelModel[16].setRotationPoint(0F, -19F, 0F);

		barrelModel[17].addShapeBox(8F, -1F, -10.4F, 2, 2, 1, 0F,-0.3F, -1F, 0F, -0.3F, -1F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F, -0.3F, 0F, 0F); // Box 428
		barrelModel[17].setRotationPoint(0F, -19F, 0F);
	}
}