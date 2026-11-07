package com.flansmod.client.model.legacy;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelStickGrenade164 extends ModelBase
{
	public ModelRendererTurbo headModel;
	public ModelRendererTurbo stickModel;

	public ModelStickGrenade164()
	{
		stickModel = new ModelRendererTurbo(this, 0, 0, 16, 8);
		stickModel.addBox(-0.5F, -0.5F, -3F, 1, 1, 4);
		headModel = new ModelRendererTurbo(this, 8, 0, 16, 8);
		headModel.addBox(-1F, -1F, 1F, 2, 2, 2);
		stickModel.rotateAngleX = -3.14159265F / 2F;
		headModel.rotateAngleX = -3.14159265F / 2F;
	}
	}