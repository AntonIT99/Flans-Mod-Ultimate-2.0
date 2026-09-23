package com.flansmod.client.model.wolffstarwars;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelSWMine extends ModelBase 
{
	public ModelRendererTurbo[] mineModel;
	public ModelRendererTurbo buttonModel;

	public ModelSWMine()
	{
		mineModel = new ModelRendererTurbo[3];
		mineModel[0] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		mineModel[0].addBox(-2F, 0F, -3F, 4, 2, 6);
		mineModel[1] = new ModelRendererTurbo(this, 14, 0, 32, 8);
		mineModel[1].addBox(-3F, 0F, -2F, 1, 2, 4);
		mineModel[2] = new ModelRendererTurbo(this, 14, 0, 32, 8);
		mineModel[2].addBox(-3F, 0F, -2F, 1, 2, 4);
		mineModel[2].rotateAngleY = 3.14159265F;
		buttonModel = new ModelRendererTurbo(this, 0, 0, 32, 8);
		buttonModel.addBox(-0.5F, 1.5F, -0.5F, 1, 1, 1);
		buttonModel.glow = true;
	}
	
}
