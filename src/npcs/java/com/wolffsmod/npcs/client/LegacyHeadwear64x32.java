package com.wolffsmod.npcs.client;

import noppes.npcs.client.model.ModelScaleRenderer;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.shared.client.model.Model2DRenderer;

/** The 1.7.10 pixel headwear faces, sampled from a 64x32 skin. */
public final class LegacyHeadwear64x32 extends ModelScaleRenderer
{
    public LegacyHeadwear64x32()
    {
        super(null, EnumParts.HEAD);
        addFace(32, 8, -4.641F, 0.8F, 4.64F, 0F, (float) Math.PI / 2F, 0F, 0.58F);
        addFace(48, 8, 4.639F, 0.8F, -4.64F, 0F, (float) -Math.PI / 2F, 0F, 0.58F);
        addFace(40, 8, -4.64F, 0.801F, -4.641F, 0F, 0F, 0F, 0.58F);
        addFace(56, 8, 4.64F, 0.801F, 4.639F, 0F, (float) Math.PI, 0F, 0.58F);
        addFace(40, 0, -4.64F, -8.5F, -4.64F, (float) -Math.PI / 2F, 0F, 0F, 0.5799F);
        addFace(48, 0, -4.64F, 0F, -4.64F, (float) -Math.PI / 2F, 0F, 0F, 0.5799F);
    }

    private void addFace(int u, int v, float x, float y, float z, float xRot, float yRot, float zRot, float scale)
    {
        // The layer supplies the NPC's skin through Model2DRenderer.textureOverride.
        Model2DRenderer face = new Model2DRenderer(64, 32, u, v, 8, 8, null);
        face.setPos(x, y, z);
        face.setScale(scale);
        face.setThickness(0.65F);
        face.xRot = xRot;
        face.yRot = yRot;
        face.zRot = zRot;
        addChild(face);
    }
}
