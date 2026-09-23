package com.flansmod.client.model.zombie;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelBearTrap extends ModelBase
{
	public ModelRendererTurbo[] mineModel;
	public ModelRendererTurbo buttonModel;
	public ModelRendererTurbo[] spikeModel;

	public ModelBearTrap()
	{
		mineModel = new ModelRendererTurbo[7];
		mineModel[0] = new ModelRendererTurbo(this, 4, 0, 32, 8);
		mineModel[0].addBox(-2F, 0F, -3F, 4, 1, 1);
		mineModel[1] = new ModelRendererTurbo(this, 14, 0, 32, 8);
		mineModel[1].addBox(-3F, 0F, -2F, 1, 1, 4);
		mineModel[2] = new ModelRendererTurbo(this, 14, 0, 32, 8);
		mineModel[2].addBox(-3F, 0F, -2F, 1, 1, 4);
		mineModel[2].rotateAngleY = 3.14159265F;
		mineModel[3] = new ModelRendererTurbo(this, 4, 0, 32, 8);
		mineModel[3].addBox(-2F, 0F, -3F, 4, 1, 1);
		mineModel[3].rotateAngleY = 3.14159265F;

		mineModel[4] = new ModelRendererTurbo(this, 14, 0, 32, 8);
		mineModel[4].addBox(-0.5F, 0F, -2F, 1, 1, 4);
		mineModel[5] = new ModelRendererTurbo(this, 22, 0, 32, 8);
		mineModel[5].addBox(-0.5F, 0F, -5F, 1, 1, 2);
		mineModel[6] = new ModelRendererTurbo(this, 22, 0, 32, 8);
		mineModel[6].addBox(-0.5F, 0F, 3F, 1, 1, 2);
		
		buttonModel = new ModelRendererTurbo(this, 0, 0, 32, 8);
		buttonModel.addBox(-0.5F, 0.5F, -0.5F, 1, 1, 1);
		
		spikeModel = new ModelRendererTurbo[10];
		
		spikeModel[0] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[0].addTrapezoid(-2F, 1F, -3F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);
		
		spikeModel[1] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[1].addTrapezoid(-3F, 1F, -2F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);
		
		spikeModel[2] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[2].addTrapezoid(-3F, 1F, -0.5F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		spikeModel[3] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[3].addTrapezoid(-3F, 1F, 1F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		spikeModel[4] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[4].addTrapezoid(-2F, 1F, 2F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		
		spikeModel[5] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[5].addTrapezoid(1F, 1F, -3F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);
		
		spikeModel[6] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[6].addTrapezoid(2F, 1F, -2F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		spikeModel[7] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[7].addTrapezoid(2F, 1F, -0.5F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		spikeModel[8] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[8].addTrapezoid(2F, 1F, 1F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);

		spikeModel[9] = new ModelRendererTurbo(this, 0, 0, 32, 8);
		spikeModel[9].addTrapezoid(1F, 1F, 2F, 1, 1, 1, 0F, -0.5F, ModelRendererTurbo.MR_BOTTOM);


	}
	
}
