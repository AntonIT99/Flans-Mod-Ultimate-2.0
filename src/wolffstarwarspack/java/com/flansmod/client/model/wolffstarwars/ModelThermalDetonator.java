//This File was created with the Minecraft-SMP Modelling Toolbox 2.1.1.6
// Copyright (C) 2015 Minecraft-SMP.de
// This file is for Flan's Flying Mod Version 4.0.x+

package com.flansmod.client.model.wolffstarwars;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.wolffsmod.api.client.model.ModelBase;
import net.minecraft.world.entity.Entity;

public class ModelThermalDetonator extends ModelBase
{
	int textureX = 32;
	int textureY = 32;

	public ModelThermalDetonator()
	{
		
		thermaldetonatorModel = new ModelRendererTurbo[5];
		thermaldetonatorModel[0] = new ModelRendererTurbo(this, 17, 1, textureX, textureY); // Import ImportImportBox1
		thermaldetonatorModel[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import ImportBox0
		thermaldetonatorModel[2] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import ImportBox1
		thermaldetonatorModel[3] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import ImportBox3
		thermaldetonatorModel[4] = new ModelRendererTurbo(this, 10, 2, textureX, textureY); // Import ImportBox4

		thermaldetonatorModel[0].addShapeBox(-0.55F, 0F, 0F, 1, 1, 1, 0F, 0.2F, 0F, -0.25F, 0.25F, 0F, -0.25F, 0.25F, 0F, -0.25F, 0.2F, 0F, -0.25F, -0.1F, 0F, -0.25F, 0.25F, 0F, -0.25F, 0.25F, 0F, -0.25F, -0.1F, 0F, -0.25F); // Import ImportImportBox1
		thermaldetonatorModel[0].setRotationPoint(-0.5F, 0.25F, -0.5F);

		thermaldetonatorModel[1].addShapeBox(3F, 0F, 0F, 2, 2, 2, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F, 0F, -0.6F, 0F); // Import ImportBox0
		thermaldetonatorModel[1].setRotationPoint(-4F, -1F, -1F);

		thermaldetonatorModel[2].addShapeBox(0F, 0F, 0F, 2, 2, 2, 0F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Import ImportBox1
		thermaldetonatorModel[2].setRotationPoint(-1F, -2.4F, -1F);

		thermaldetonatorModel[3].addShapeBox(0F, 0F, 0F, 2, 2, 2, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F, -0.2F, -1.4F, -0.2F); // Import ImportBox3
		thermaldetonatorModel[3].setRotationPoint(-1F, 0.4F, -1F);

		thermaldetonatorModel[4].addShapeBox(0.05F, 0.2F, -0.5F, 1, 1, 1, 0F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F, -0.15F); // Import ImportBox4
		thermaldetonatorModel[4].setRotationPoint(0F, 0F, 0F);

		
		thermaldetonatorModel[4].glow = true;

	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		for(int i = 0; i < thermaldetonatorModel.length; i++)
		{
			thermaldetonatorModel[i].render(f5);
		}
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5)
	{
	}

	public ModelRendererTurbo thermaldetonatorModel[];
}