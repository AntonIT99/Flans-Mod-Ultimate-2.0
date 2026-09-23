package com.flansmod.client.model.mw;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;

public class ModelBulletFlash1 extends ModelBase
{
	int textureX = 256;
	int textureY = 256;

	public ModelBulletFlash1()
	{
		bulletflash1Model = new ModelRendererTurbo[3];
		bulletflash1Model[0] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Import
		bulletflash1Model[1] = new ModelRendererTurbo(this, 1, 1, textureX, textureY); // Import Import
		bulletflash1Model[2] = new ModelRendererTurbo(this, 105, 1, textureX, textureY); // Import Import

		bulletflash1Model[0].addBox(-1.5F, 0F, -1.5F, 34, 1, 34, 0F); // Import Import
		bulletflash1Model[0].setRotationPoint(-16F, -22F, -17F);

		bulletflash1Model[1].addTrapezoid(-2F, 1F, -2F, 4, 22, 4, 0F, -1F, ModelRendererTurbo.MR_TOP); // Import Import
		bulletflash1Model[1].setRotationPoint(0F, -22F, -2F);

		bulletflash1Model[2].addTrapezoid(-2F, 4F, -2F, 4, 16, 4, 0F, -1F, ModelRendererTurbo.MR_BOTTOM); // Import Import
		bulletflash1Model[2].setRotationPoint(0F, -3F, -2F);


	}


	public ModelRendererTurbo bulletflash1Model[];
}
