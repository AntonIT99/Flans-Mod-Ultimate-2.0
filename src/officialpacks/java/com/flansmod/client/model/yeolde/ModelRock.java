package com.flansmod.client.model.yeolde;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;


public class ModelRock extends ModelBase
{
	public ModelRendererTurbo rockModel;

	public ModelRock()
	{
		rockModel = new ModelRendererTurbo(this, 0, 0, 8, 8);
		rockModel.addBox(-1F, -1F, -1F, 2, 2, 2);
	}

}
