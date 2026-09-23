package com.flansmod.client.model.mw;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelMWParachute extends ModelBase
{
	public ModelRendererTurbo backpackModel;
	public ModelRendererTurbo[] parachuteModel = new ModelRendererTurbo[6];
	
	public ModelMWParachute()
	{
		backpackModel = new ModelRendererTurbo(this, 0, 0, 128, 128);
		backpackModel.addBox(-4F, 26F, -6F, 8, 12, 2);
		
		//Outside
		parachuteModel[0] = new ModelRendererTurbo(this, 0, 16, 128, 128);
		parachuteModel[0].addTrapezoid(-12, 38, -12, 24, 24, 24, 0F, -8F, ModelRendererTurbo.MR_TOP);
		parachuteModel[1] = new ModelRendererTurbo(this, 72, 0, 128, 128);
		parachuteModel[1].addTrapezoid(-8, 62, -8, 16, 8, 16, 0F, 4F, ModelRendererTurbo.MR_TOP);
		parachuteModel[2] = new ModelRendererTurbo(this, 0, 64, 128, 128);
		parachuteModel[2].addTrapezoid(-8, 70, -8, 16, 4, 16, 0F, -4F, ModelRendererTurbo.MR_BOTTOM);
		
		//Inside
		parachuteModel[3] = new ModelRendererTurbo(this, 0, 16, 128, 128);
		parachuteModel[3].flip = true;
		parachuteModel[3].addTrapezoid(-12, 38, -12, 24, 24, 24, 0F, -8F, ModelRendererTurbo.MR_TOP);
		parachuteModel[4] = new ModelRendererTurbo(this, 72, 0, 128, 128);
		parachuteModel[4].flip = true;
		parachuteModel[4].addTrapezoid(-8, 62, -8, 16, 8, 16, 0F, 4F, ModelRendererTurbo.MR_TOP);
		parachuteModel[5] = new ModelRendererTurbo(this, 0, 64, 128, 128);
		parachuteModel[5].flip = true;
		parachuteModel[5].addTrapezoid(-8, 70, -8, 16, 4, 16, 0F, -4F, ModelRendererTurbo.MR_BOTTOM);
	}
	
}
