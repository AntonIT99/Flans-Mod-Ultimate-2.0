//This File was created with the Minecraft-SMP Modelling Toolbox 2.3.0.0
// Copyright (C) 2018 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

// Model: ParalysisBeam
// Model Creator: 
// Created on: 11.09.2018 - 18:15:52
// Last changed on: 11.09.2018 - 18:15:52

package com.flansmod.client.model.wolffstarwars; //Path where the model is located

import org.lwjgl.opengl.GL11;

import com.flansmod.client.tmt.ModelRendererTurbo;

import com.wolffsmod.api.client.model.ModelBase;
import net.minecraft.world.entity.Entity;

public class ModelStunBeam extends ModelBase //Same as Filename
{
	public ModelRendererTurbo stunBeamModel[];
	
	int textureX = 64;
	int textureY = 32;

	public ModelStunBeam() //Same as Filename
	{
		stunBeamModel = new ModelRendererTurbo[8];
		stunBeamModel[0] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[1] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[2] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[3] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[4] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[5] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[6] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		stunBeamModel[7] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);

		stunBeamModel[0].addBox(0F, 0F, 0F, 1, 1, 4, 0F);
		stunBeamModel[0].setRotationPoint(-4.5F, 0F, -1.5F);

		stunBeamModel[1].addBox(0F, 0F, 0F, 1, 1, 4, 0F);
		stunBeamModel[1].setRotationPoint(4.5F, 0F, -1.5F);

		stunBeamModel[2].addBox(0F, 0F, 0F, 4, 1, 1, 0F);
		stunBeamModel[2].setRotationPoint(-1.5F, 0F, -4.5F);

		stunBeamModel[3].addBox(0F, 0F, 0F, 4, 1, 1, 0F);
		stunBeamModel[3].setRotationPoint(-1.5F, 0F, 4.5F);

		stunBeamModel[4].addShapeBox(0F, 0F, 1F, 1, 1, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, 0F, 2F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, 0F, 2F, 0F, -1F); // Box 0
		stunBeamModel[4].setRotationPoint(4.5F, 0F, 1.5F);

		stunBeamModel[5].addShapeBox(0F, 0F, 1F, 1, 1, 3, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, -1F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, -1F, -3F, 0F, 0F); // Box 4
		stunBeamModel[5].setRotationPoint(-4.5F, 0F, 1.5F);

		stunBeamModel[6].addShapeBox(0F, 0F, 1F, 1, 1, 3, 0F, -3F, 0F, 0F, 2F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F, -3F, 0F, 0F, 2F, 0F, -1F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 5
		stunBeamModel[6].setRotationPoint(-4.5F, 0F, -5.5F);

		stunBeamModel[7].addShapeBox(0F, 0F, 1F, 1, 1, 3, 0F, 2F, 0F, -1F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 2F, 0F, -1F, -3F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 6
		stunBeamModel[7].setRotationPoint(4.5F, 0F, -5.5F);

		for(ModelRendererTurbo part: stunBeamModel)
		{
			part.glow = true;
		}
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		GL11.glTranslatef(-2F / 16F, 2.5F, 4.0F / 16F);
		for(ModelRendererTurbo part: stunBeamModel)
		{
			part.render(f5);
		}
	}
}