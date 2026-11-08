package com.flansmodultimate.compat.tap.model.mw;

import com.flansmodultimate.client.model.ModelBase;

import com.flansmod.client.tmt.ModelRendererTurbo;

public class ModelRoundGrenade extends ModelBase 
{
	public ModelRendererTurbo headModel;
	public ModelRendererTurbo bodyModel;

	public ModelRoundGrenade()
	{
		bodyModel = new ModelRendererTurbo(this, 0, 0, 16, 8);
		bodyModel.addBox(-1F, -1F, -1F, 2, 2, 2);
		headModel = new ModelRendererTurbo(this, 8, 0, 16, 8);
		headModel.addBox(-0.5F, -0.5F, 0.5F, 1, 1, 1);
		bodyModel.rotateAngleX = -3.14159265F / 2F;
		headModel.rotateAngleX = -3.14159265F / 2F;
	}

}
