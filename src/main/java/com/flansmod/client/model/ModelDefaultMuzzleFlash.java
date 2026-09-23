package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.FlansMod;

import net.minecraft.resources.ResourceLocation;

public class ModelDefaultMuzzleFlash extends ModelMuzzleFlash
{
    protected ModelRendererTurbo mfModel[];

    public ModelDefaultMuzzleFlash()
    {
        mfModel = new ModelRendererTurbo[3];

        mfModel[0] = new ModelRendererTurbo(this, 0, 0, 16, 16);
        mfModel[1] = new ModelRendererTurbo(this, 0, 0, 16, 16);

        mfModel[2] = new ModelRendererTurbo(this, 0, 8, 16, 16);

        mfModel[0].addBox(0f, -2f, -2f, 0, 4, 4);
        mfModel[0].glow = true;
        mfModel[1].addBox(0f, 0f, -2f, 4, 0, 4);
        mfModel[1].glow = true;
        mfModel[2].addBox(0f, -2f, 0f, 4, 4, 0);
        mfModel[2].glow = true;
    }

    @Override
    public ResourceLocation getTexture()
    {
        return FlansMod.TEXTURE_DEFAULTMUZZLEFLASH;
    }
}
