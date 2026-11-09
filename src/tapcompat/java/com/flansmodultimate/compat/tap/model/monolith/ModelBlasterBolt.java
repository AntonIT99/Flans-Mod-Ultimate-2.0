//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2025 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: BlasterBolt
// Model Creator:
// Created on:18.03.2024 - 01:41:46
// Last changed on: 18.03.2024 - 01:41:46

package com.flansmodultimate.compat.tap.model.monolith;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmod.client.tmt.Coord2D;
import com.flansmod.client.tmt.Shape2D;

public class ModelBlasterBolt extends ModelBase
{
	int textureX = 32;
	int textureY = 32;

	public ModelBlasterBolt()
	{
		blasterboltModel = new ModelRendererTurbo[6];
		blasterboltModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 24
		blasterboltModel[1] = new ModelRendererTurbo(this, 9, 1, textureX, textureY); // Box 2
		blasterboltModel[2] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Box 3
		blasterboltModel[3] = new ModelRendererTurbo(this, 25, 1, textureX, textureY); // Box 4
		blasterboltModel[4] = new ModelRendererTurbo(this, 1, 9, textureX, textureY); // Box 5
		blasterboltModel[5] = new ModelRendererTurbo(this, 9, 9, textureX, textureY); // Box 6

		blasterboltModel[0].addShapeBox(0F, 0F, 0F, 1, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F); // Box 24
		blasterboltModel[0].setRotationPoint(-0.5F, 0F, -0.5F);

		blasterboltModel[1].addShapeBox(0F, 0F, 0F, 1, 1, 1, 0F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F, -0.2F, -0.6F, -0.2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 2
		blasterboltModel[1].setRotationPoint(-0.5F, -7F, -0.5F);

		blasterboltModel[2].addShapeBox(0F, 0F, 0F, 1, 6, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 3
		blasterboltModel[2].setRotationPoint(-0.5F, -6F, -0.5F);

		blasterboltModel[3].addShapeBox(0F, 0F, 0F, 1, 7, 1, 0F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F, 0.4F, 0F, 0.4F); // Box 4
		blasterboltModel[3].setRotationPoint(-0.5F, -6.5F, -0.5F);

		blasterboltModel[4].addShapeBox(0F, 0F, 0F, 1, 1, 1, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F); // Box 5
		blasterboltModel[4].setRotationPoint(-0.5F, -7F, -0.5F);

		blasterboltModel[5].addShapeBox(0F, 0F, 0F, 1, 1, 1, 0F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F, 0.4F, -0.5F, 0.4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 6
		blasterboltModel[5].setRotationPoint(-0.5F, 0F, -0.5F);
		
		
		blasterboltModel[0].glow = true;
		blasterboltModel[1].glow = true;
		blasterboltModel[2].glow = true;
		blasterboltModel[3].glow = false;
		blasterboltModel[4].glow = false;
		blasterboltModel[5].glow = false;


	}


	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo blasterboltModel[];
}