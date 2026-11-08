//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2024 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: TPVarmed
// Model Creator:
// Created on:14.07.2024 - 19:35:11
// Last changed on: 14.07.2024 - 19:35:11

package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelTPVarmed extends ModelBase
{
	int textureX = 64;
	int textureY = 64;

	public ModelTPVarmed()
	{
		tpvarmedModel = new ModelRendererTurbo[33];
		tpvarmedModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 6
		tpvarmedModel[1] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 7
		tpvarmedModel[2] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 8
		tpvarmedModel[3] = new ModelRendererTurbo(this, 33, 1, textureX, textureY); // Box 9
		tpvarmedModel[4] = new ModelRendererTurbo(this, 41, 1, textureX, textureY); // Box 10
		tpvarmedModel[5] = new ModelRendererTurbo(this, 49, 1, textureX, textureY); // Box 11
		tpvarmedModel[6] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 12
		tpvarmedModel[7] = new ModelRendererTurbo(this, 17, 17, textureX, textureY); // Box 13
		tpvarmedModel[8] = new ModelRendererTurbo(this, 33, 17, textureX, textureY); // Box 15
		tpvarmedModel[9] = new ModelRendererTurbo(this, 49, 17, textureX, textureY); // Box 16
		tpvarmedModel[10] = new ModelRendererTurbo(this, 17, 25, textureX, textureY); // Box 17
		tpvarmedModel[11] = new ModelRendererTurbo(this, 41, 25, textureX, textureY); // Box 18
		tpvarmedModel[12] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 20
		tpvarmedModel[13] = new ModelRendererTurbo(this, 1, 17, textureX, textureY); // Box 21
		tpvarmedModel[14] = new ModelRendererTurbo(this, 25, 33, textureX, textureY); // Box 22
		tpvarmedModel[15] = new ModelRendererTurbo(this, 41, 33, textureX, textureY); // Box 23
		tpvarmedModel[16] = new ModelRendererTurbo(this, 33, 25, textureX, textureY); // Box 24
		tpvarmedModel[17] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 27
		tpvarmedModel[18] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 28
		tpvarmedModel[19] = new ModelRendererTurbo(this, 9, 41, textureX, textureY); // Box 42
		tpvarmedModel[20] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 43
		tpvarmedModel[21] = new ModelRendererTurbo(this, 9, 41, textureX, textureY); // Box 48
		tpvarmedModel[22] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 49
		tpvarmedModel[23] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 50
		tpvarmedModel[24] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 51
		tpvarmedModel[25] = new ModelRendererTurbo(this, 9, 41, textureX, textureY); // Box 52
		tpvarmedModel[26] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 53
		tpvarmedModel[27] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 54
		tpvarmedModel[28] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 55
		tpvarmedModel[29] = new ModelRendererTurbo(this, 9, 41, textureX, textureY); // Box 56
		tpvarmedModel[30] = new ModelRendererTurbo(this, 57, 33, textureX, textureY); // Box 57
		tpvarmedModel[31] = new ModelRendererTurbo(this, 17, 41, textureX, textureY); // Box 58
		tpvarmedModel[32] = new ModelRendererTurbo(this, 1, 41, textureX, textureY); // Box 59

		tpvarmedModel[0].addShapeBox(0F, 0F, 0F, 4, 10, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 6
		tpvarmedModel[0].setRotationPoint(-2F, -5F, -3F);

		tpvarmedModel[1].addShapeBox(0F, 0F, 0F, 1, 9, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 8F, 0F, 1F, -8F, 0F, 1F, -8F, 0F, -1F, 8F, 0F, -1F); // Box 7
		tpvarmedModel[1].setRotationPoint(-2F, 4F, -3F);

		tpvarmedModel[2].addShapeBox(0F, 0F, 0F, 1, 9, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -8F, 0F, 1F, 8F, 0F, 1F, 8F, 0F, -1F, -8F, 0F, -1F); // Box 8
		tpvarmedModel[2].setRotationPoint(1F, 4F, -3F);

		tpvarmedModel[3].addShapeBox(0F, 0F, 0F, 1, 9, 1, 0F, -8F, 0F, 1F, 8F, 0F, 1F, 8F, 0F, -1F, -8F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 9
		tpvarmedModel[3].setRotationPoint(1F, -13F, -3F);

		tpvarmedModel[4].addShapeBox(0F, 0F, 0F, 1, 9, 1, 0F, 8F, 0F, 1F, -8F, 0F, 1F, -8F, 0F, -1F, 8F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 10
		tpvarmedModel[4].setRotationPoint(-2F, -13F, -3F);

		tpvarmedModel[5].addShapeBox(0F, 0F, 0F, 4, 10, 1, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F, 0.2F, 0.2F, 0F); // Box 11
		tpvarmedModel[5].setRotationPoint(-2F, -5F, -3.3F);

		tpvarmedModel[6].addShapeBox(0F, 0F, 0F, 1, 1, 8, 0F, -0.45F, -0.45F, 0F, -0.45F, -0.45F, 0F, -0.4F, -0.4F, 0F, -0.4F, -0.4F, 0F, -0.45F, -0.45F, 0F, -0.45F, -0.45F, 0F, -0.4F, -0.4F, 0F, -0.4F, -0.4F, 0F); // Box 12
		tpvarmedModel[6].setRotationPoint(-2F, -5F, -11.3F);

		tpvarmedModel[7].addShapeBox(0F, 0F, 0F, 3, 1, 4, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 13
		tpvarmedModel[7].setRotationPoint(-1.5F, -0.5F, 2F);

		tpvarmedModel[8].addShapeBox(0F, 0F, 0F, 3, 1, 4, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.4F, -0.5F, 0F, -1.4F, -0.5F, 0F, -1F, 0F, 0F, -1F, 0F, 0F); // Box 15
		tpvarmedModel[8].setRotationPoint(-1.5F, 0.5F, 2F);

		tpvarmedModel[9].addShapeBox(0F, 0F, 0F, 3, 1, 4, 0F, -1.4F, -0.5F, 0F, -1.4F, -0.5F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 16
		tpvarmedModel[9].setRotationPoint(-1.5F, -1.5F, 2F);

		tpvarmedModel[10].addShapeBox(0F, 0F, 0F, 3, 1, 5, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.4F, -0.4F, 0F, -1.4F, -0.4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.4F, -0.4F, 0F, -1.4F, -0.4F, 0F); // Box 17
		tpvarmedModel[10].setRotationPoint(-1.5F, -0.5F, 6F);

		tpvarmedModel[11].addShapeBox(0F, 0F, 0F, 3, 1, 5, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1.2F, -0.5F, -3F, -1.2F, -0.5F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, -1.4F, 0.4F, 0F, -1.4F, 0.4F, 0F); // Box 18
		tpvarmedModel[11].setRotationPoint(-1.5F, -1.5F, 6F);

		tpvarmedModel[12].addShapeBox(0F, 0F, 0F, 3, 1, 5, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -1.4F, 0.4F, 0F, -1.4F, 0.4F, 0F, -1F, 0F, 0F, -1F, 0F, 0F, -1.2F, -0.5F, -3F, -1.2F, -0.5F, -3F); // Box 20
		tpvarmedModel[12].setRotationPoint(-1.5F, 0.5F, 6F);

		tpvarmedModel[13].addShapeBox(0F, 0F, 0F, 1, 1, 2, 0F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F, -0.2F); // Box 21
		tpvarmedModel[13].setRotationPoint(-0.5F, -0.5F, 9F);

		tpvarmedModel[14].addShapeBox(0F, 0F, 0F, 2, 1, 4, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.5F, 0F, 0F, -0.5F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 22
		tpvarmedModel[14].setRotationPoint(-1F, -0.5F, 0F);

		tpvarmedModel[15].addShapeBox(0F, 0F, 0F, 3, 4, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 23
		tpvarmedModel[15].setRotationPoint(-1.5F, 2F, -4F);

		tpvarmedModel[16].addShapeBox(0F, 0F, 0F, 2, 1, 2, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 24
		tpvarmedModel[16].setRotationPoint(-1F, 5.5F, -3.5F);

		tpvarmedModel[17].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 27
		tpvarmedModel[17].setRotationPoint(-10F, 6.5F, -4.5F);

		tpvarmedModel[18].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F); // Box 28
		tpvarmedModel[18].setRotationPoint(-10F, 12.5F, -4.5F);

		tpvarmedModel[19].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F); // Box 42
		tpvarmedModel[19].setRotationPoint(-10F, 12.5F, -4.5F);

		tpvarmedModel[20].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 43
		tpvarmedModel[20].setRotationPoint(-10F, 6.5F, -4.5F);

		tpvarmedModel[21].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F); // Box 48
		tpvarmedModel[21].setRotationPoint(9F, 12.5F, -4.5F);

		tpvarmedModel[22].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 49
		tpvarmedModel[22].setRotationPoint(9F, 6.5F, -4.5F);

		tpvarmedModel[23].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 50
		tpvarmedModel[23].setRotationPoint(9F, 6.5F, -4.5F);

		tpvarmedModel[24].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F); // Box 51
		tpvarmedModel[24].setRotationPoint(9F, 12.5F, -4.5F);

		tpvarmedModel[25].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F); // Box 52
		tpvarmedModel[25].setRotationPoint(9F, -12.5F, -4.5F);

		tpvarmedModel[26].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 53
		tpvarmedModel[26].setRotationPoint(9F, -18.5F, -4.5F);

		tpvarmedModel[27].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 54
		tpvarmedModel[27].setRotationPoint(9F, -18.5F, -4.5F);

		tpvarmedModel[28].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F); // Box 55
		tpvarmedModel[28].setRotationPoint(9F, -12.5F, -4.5F);

		tpvarmedModel[29].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F); // Box 56
		tpvarmedModel[29].setRotationPoint(-10F, -12.5F, -4.5F);

		tpvarmedModel[30].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, -5F, 0F, 0F, 5F, 0F, 0F, 5F, 0F, -0.8F, -5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 57
		tpvarmedModel[30].setRotationPoint(-10F, -18.5F, -4.5F);

		tpvarmedModel[31].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 58
		tpvarmedModel[31].setRotationPoint(-10F, -18.5F, -4.5F);

		tpvarmedModel[32].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 5F, 0F, 0F, -5F, 0F, 0F, -5F, 0F, -0.8F, 5F, 0F, -0.8F); // Box 59
		tpvarmedModel[32].setRotationPoint(-10F, -12.5F, -4.5F);


	}


	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo tpvarmedModel[];
}