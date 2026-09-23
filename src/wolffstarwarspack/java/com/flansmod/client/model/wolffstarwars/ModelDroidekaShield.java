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

public class ModelDroidekaShield extends ModelCustomArmour //Same as Filename
{
	int textureX = 512;
	int textureY = 256;

	public ModelDroidekaShield() //Same as Filename
	{
		bodyModel = new ModelRendererTurbo[31];
		bodyModel[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Box 84
		bodyModel[1] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Box 108
		bodyModel[2] = new ModelRendererTurbo(this, 161, 1, textureX, textureY); // Box 109
		bodyModel[3] = new ModelRendererTurbo(this, 225, 1, textureX, textureY); // Box 110
		bodyModel[4] = new ModelRendererTurbo(this, 281, 1, textureX, textureY); // Box 111
		bodyModel[5] = new ModelRendererTurbo(this, 337, 1, textureX, textureY); // Box 112
		bodyModel[6] = new ModelRendererTurbo(this, 393, 1, textureX, textureY); // Box 113
		bodyModel[7] = new ModelRendererTurbo(this, 449, 1, textureX, textureY); // Box 115
		bodyModel[8] = new ModelRendererTurbo(this, 1, 33, textureX, textureY); // Box 116
		bodyModel[9] = new ModelRendererTurbo(this, 265, 33, textureX, textureY); // Box 117
		bodyModel[10] = new ModelRendererTurbo(this, 321, 33, textureX, textureY); // Box 118
		bodyModel[11] = new ModelRendererTurbo(this, 377, 33, textureX, textureY); // Box 119
		bodyModel[12] = new ModelRendererTurbo(this, 433, 33, textureX, textureY); // Box 120
		bodyModel[13] = new ModelRendererTurbo(this, 57, 41, textureX, textureY); // Box 121
		bodyModel[14] = new ModelRendererTurbo(this, 97, 57, textureX, textureY); // Box 122
		bodyModel[15] = new ModelRendererTurbo(this, 153, 57, textureX, textureY); // Box 123
		bodyModel[16] = new ModelRendererTurbo(this, 209, 57, textureX, textureY); // Box 124
		bodyModel[17] = new ModelRendererTurbo(this, 1, 65, textureX, textureY); // Box 125
		bodyModel[18] = new ModelRendererTurbo(this, 265, 65, textureX, textureY); // Box 126
		bodyModel[19] = new ModelRendererTurbo(this, 321, 65, textureX, textureY); // Box 127
		bodyModel[20] = new ModelRendererTurbo(this, 377, 65, textureX, textureY); // Box 128
		bodyModel[21] = new ModelRendererTurbo(this, 433, 65, textureX, textureY); // Box 129
		bodyModel[22] = new ModelRendererTurbo(this, 57, 73, textureX, textureY); // Box 130
		bodyModel[23] = new ModelRendererTurbo(this, 97, 89, textureX, textureY); // Box 131
		bodyModel[24] = new ModelRendererTurbo(this, 153, 89, textureX, textureY); // Box 132
		bodyModel[25] = new ModelRendererTurbo(this, 209, 89, textureX, textureY); // Box 133
		bodyModel[26] = new ModelRendererTurbo(this, 1, 97, textureX, textureY); // Box 134
		bodyModel[27] = new ModelRendererTurbo(this, 265, 97, textureX, textureY); // Box 135
		bodyModel[28] = new ModelRendererTurbo(this, 321, 97, textureX, textureY); // Box 136
		bodyModel[29] = new ModelRendererTurbo(this, 377, 97, textureX, textureY); // Box 137
		bodyModel[30] = new ModelRendererTurbo(this, 433, 97, textureX, textureY); // Box 138

		bodyModel[0].addBox(-17.5F, 2.85F, -7.6F, 36, 12, 12, 0F); // Box 84
		bodyModel[0].setRotationPoint(0F, 0F, 0F);

		bodyModel[1].addBox(-5.5F, -9.15F, -7.6F, 12, 36, 12, 0F); // Box 108
		bodyModel[1].setRotationPoint(0F, 0F, 0F);

		bodyModel[2].addBox(-5.5F, 2.85F, -19.6F, 12, 12, 36, 0F); // Box 109
		bodyModel[2].setRotationPoint(0F, 0F, 0F);

		bodyModel[3].addShapeBox(-17.5F, 2.85F, -19.6F, 12, 12, 12, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 110
		bodyModel[3].setRotationPoint(0F, 0F, 0F);

		bodyModel[4].addShapeBox(6.5F, 2.85F, -19.6F, 12, 12, 12, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 111
		bodyModel[4].setRotationPoint(0F, 0F, 0F);

		bodyModel[5].addShapeBox(6.5F, 2.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F); // Box 112
		bodyModel[5].setRotationPoint(0F, 0F, 0F);

		bodyModel[6].addShapeBox(-17.5F, 2.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F); // Box 113
		bodyModel[6].setRotationPoint(0F, 0F, 0F);

		bodyModel[7].addShapeBox(6.5F, -9.15F, -7.6F, 12, 12, 12, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 115
		bodyModel[7].setRotationPoint(0F, 0F, 0F);

		bodyModel[8].addShapeBox(-17.5F, -9.15F, -7.6F, 12, 12, 12, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 116
		bodyModel[8].setRotationPoint(0F, 0F, 0F);

		bodyModel[9].addShapeBox(-5.5F, -9.15F, -19.6F, 12, 12, 12, 0F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 117
		bodyModel[9].setRotationPoint(0F, 0F, 0F);

		bodyModel[10].addShapeBox(-5.5F, -9.15F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 118
		bodyModel[10].setRotationPoint(0F, 0F, 0F);

		bodyModel[11].addShapeBox(6.5F, 14.85F, -7.6F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, 0F, 0F, 0F); // Box 119
		bodyModel[11].setRotationPoint(0F, 0F, 0F);

		bodyModel[12].addShapeBox(-5.5F, 14.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, -4F); // Box 120
		bodyModel[12].setRotationPoint(0F, 0F, 0F);

		bodyModel[13].addShapeBox(-5.5F, 14.85F, -19.6F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 121
		bodyModel[13].setRotationPoint(0F, 0F, 0F);

		bodyModel[14].addShapeBox(-17.5F, 14.85F, -7.6F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, 0F); // Box 122
		bodyModel[14].setRotationPoint(0F, 0F, 0F);

		bodyModel[15].addShapeBox(-17.5F, -9.15F, -19.6F, 12, 12, 12, 0F, -7F, -4F, -7F, 0F, 0F, -12F, 0F, 0F, 0F, -4F, -4F, 0F, -4F, 0F, -4F, 0F, 0F, -12F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 123
		bodyModel[15].setRotationPoint(0F, 0F, 0F);

		bodyModel[16].addShapeBox(-17.5F, -9.15F, -19.6F, 12, 12, 12, 0F, -7F, -4F, -7F, 0F, -4F, -4F, 0F, 0F, 0F, -11.99F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -11.99F, 0F, 0F); // Box 124
		bodyModel[16].setRotationPoint(0F, 0F, 0F);

		bodyModel[17].addShapeBox(6.5F, -9.15F, -19.6F, 12, 12, 12, 0F, 0F, -4F, -4F, -7F, -4F, -7F, -11.99F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, -11.99F, 0F, 0F, 0F, 0F, 0F); // Box 125
		bodyModel[17].setRotationPoint(0F, 0F, 0F);

		bodyModel[18].addShapeBox(6.5F, -9.15F, -19.6F, 12, 12, 12, 0F, 0F, 0F, -11.99F, -7F, -4F, -7F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -11.99F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F); // Box 126
		bodyModel[18].setRotationPoint(0F, 0F, 0F);

		bodyModel[19].addShapeBox(-17.5F, -9.15F, 4.4F, 12, 12, 12, 0F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, -7F, -4F, -7F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F); // Box 127
		bodyModel[19].setRotationPoint(0F, 0F, 0F);

		bodyModel[20].addShapeBox(6.5F, -9.15F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, -12F, 0F, 0F, -7F, -4F, -7F, 0F, -4F, -4F, 0F, 0F, 0F, -12F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F); // Box 128
		bodyModel[20].setRotationPoint(0F, 0F, 0F);

		bodyModel[21].addShapeBox(-17.5F, -9.15F, 4.4F, 12, 12, 12, 0F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, -7F, -4F, -7F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, -4F, 0F, -4F); // Box 129
		bodyModel[21].setRotationPoint(0F, 0F, 0F);

		bodyModel[22].addShapeBox(6.5F, -9.15F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, -4F, -4F, 0F, -7F, -4F, -7F, 0F, 0F, -11.99F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, -11.99F); // Box 130
		bodyModel[22].setRotationPoint(0F, 0F, 0F);

		bodyModel[23].addShapeBox(-17.5F, 14.85F, 4.4F, 12, 12, 12, 0F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, -7F, -4F, -7F); // Box 131
		bodyModel[23].setRotationPoint(0F, 0F, 0F);

		bodyModel[24].addShapeBox(-17.5F, 14.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, -4F, 0F, -4F, -4F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, -7F, -4F, -7F); // Box 132
		bodyModel[24].setRotationPoint(0F, 0F, 0F);

		bodyModel[25].addShapeBox(6.5F, 14.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, -12F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, 0F, 0F, -7F, -4F, -7F, 0F, -4F, -4F); // Box 133
		bodyModel[25].setRotationPoint(0F, 0F, 0F);

		bodyModel[26].addShapeBox(-17.5F, 14.85F, -19.6F, 12, 12, 12, 0F, -4F, 0F, -4F, 0F, 0F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, -7F, -4F, -7F, 0F, 0F, -12F, 0F, 0F, 0F, -4F, -4F, 0F); // Box 134
		bodyModel[26].setRotationPoint(0F, 0F, 0F);

		bodyModel[27].addShapeBox(-17.5F, 14.85F, -19.6F, 12, 12, 12, 0F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, 0F, 0F, -7F, -4F, -7F, 0F, -4F, -4F, 0F, 0F, 0F, -12F, 0F, 0F); // Box 135
		bodyModel[27].setRotationPoint(0F, 0F, 0F);

		bodyModel[28].addShapeBox(6.5F, 14.85F, -19.6F, 12, 12, 12, 0F, 0F, 0F, 0F, -4F, 0F, -4F, -12F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, -4F, -7F, -4F, -7F, -12F, 0F, 0F, 0F, 0F, 0F); // Box 136
		bodyModel[28].setRotationPoint(0F, 0F, 0F);

		bodyModel[29].addShapeBox(6.5F, 14.85F, -19.6F, 12, 12, 12, 0F, 0F, 0F, -12F, -4F, 0F, -4F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -12F, -7F, -4F, -7F, -4F, -4F, 0F, 0F, 0F, 0F); // Box 137
		bodyModel[29].setRotationPoint(0F, 0F, 0F);

		bodyModel[30].addShapeBox(6.5F, 14.85F, 4.4F, 12, 12, 12, 0F, 0F, 0F, 0F, 0F, 0F, 0F, -4F, 0F, -4F, 0F, 0F, -12F, 0F, 0F, 0F, -4F, -4F, 0F, -7F, -4F, -7F, 0F, 0F, -12F); // Box 138
		bodyModel[30].setRotationPoint(0F, 0F, 0F);

		
		for (int i = 0; i<bodyModel.length; i++)
		{
			bodyModel[i].glow = true;
		}


	}
}