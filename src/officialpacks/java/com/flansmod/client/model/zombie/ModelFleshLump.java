package com.flansmod.client.model.zombie;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelFleshLump extends ModelBase
{
	public ModelRendererTurbo[] fleshModel;

	public ModelFleshLump()
	{
		fleshModel = new ModelRendererTurbo[2];

		fleshModel[0] = new ModelRendererTurbo(this, 0, 0, 16, 16);
		fleshModel[0].addShapeBox(-2F, -2F, -3F, 5, 5, 4, 0F, /* 0 */ 0.5F, 0.5F, -0.5F, /* 1 */ 0F, 0.5F, 0F, /* 2 */ 0F, 0F, 0F, /* 3 */ -1F, 0.5F, 0F, /* 4 */ 0.5F, 0F, 0F, /* 5 */ 1F, 0F, 0F, /* 6 */ -0.5F, 0.5F, 1F, /* 7 */ 0.5F, 0.5F, 1F);
		fleshModel[1] = new ModelRendererTurbo(this, 8, 0, 16, 16);
		fleshModel[1].addShapeBox(-2F, -2F, 0F, 4, 5, 5, 0F, /* 0 */ 0F, -1F, 0.5F, /* 1 */ -0.5F, 0.5F, 0F, /* 2 */ 1F, 0F, 0F, /* 3 */ 0.5F, 0.5F, 0.5F, /* 4 */ -1F, 1F, 0F, /* 5 */ -1F, 0F, 0.5F, /* 6 */ 1F, 0F, 0F, /* 7 */ 0.5F, 0F, -1F);
	}

}
