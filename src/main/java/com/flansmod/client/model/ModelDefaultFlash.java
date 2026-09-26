package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;

/**
 * Built-in three-frame flash for guns whose pack ships none, selected with
 * {@code FlashModel DefaultFlash}. Without a {@code FlashTexture} it renders with
 * {@link com.flansmodultimate.FlansMod#TEXTURE_DEFAULTFLASH}.
 * <p>
 * Every frame is a disc centred on the model origin, so a gun's {@code muzzleFlashPoint} is the
 * muzzle itself divided by its {@code flashScale}.</p>
 */
public class ModelDefaultFlash extends ModelFlash
{
    private static final int TEXTURE_X = 256;
    private static final int TEXTURE_Y = 128;

    public ModelDefaultFlash()
    {
        flashModel = new ModelRendererTurbo[3][1];
        flashModel[0][0] = frame(165, 2, 13F);
        flashModel[1][0] = frame(0, 2, 13.25F);
        flashModel[2][0] = frame(80, 0, 12F);
        flipAll();
    }

    private ModelRendererTurbo frame(int textureU, int textureV, float inset)
    {
        ModelRendererTurbo frame = new ModelRendererTurbo(this, textureU, textureV, TEXTURE_X, TEXTURE_Y);
        frame.addShapeBox(0F, -8F, -0.5F, 1, 35, 35, 0F,
            -0.45F, -inset, -inset, -0.45F, -inset, -inset, -0.45F, -inset, -inset, -0.45F, -inset, -inset,
            -0.45F, -inset, -inset, -0.45F, -inset, -inset, -0.45F, -inset, -inset, -0.45F, -inset, -inset);
        frame.setRotationPoint(-0.5F, -9.5F, -17F);
        return frame;
    }
}
