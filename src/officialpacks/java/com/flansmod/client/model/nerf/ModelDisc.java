package com.flansmod.client.model.nerf;

import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.model.ModelRenderer;


public class ModelDisc extends ModelBase
{
	public ModelRenderer bulletModel;

	public ModelDisc()
	{
		bulletModel = new ModelRenderer(this, 0, 0);
		bulletModel.addBox(-1F, -1F, -0.5F, 2, 2, 1);
	}

}
