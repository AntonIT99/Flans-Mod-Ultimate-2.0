package com.flansmodultimate.apocalyse.client.render;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** The five-part 1.12.2 bomb and its two-sided spherical blast, on the original 64 by 64 texture. */
public final class NukeDropModel extends ModelBase
{
    private final ModelRendererTurbo[] bomb = new ModelRendererTurbo[5];
    private final ModelRendererTurbo[] blast = new ModelRendererTurbo[2];

    public NukeDropModel()
    {
        textureWidth = 64;
        textureHeight = 64;
        for (int i = 0; i < bomb.length; i++)
            bomb[i] = new ModelRendererTurbo(this, 0, 0, 64, 64);
        bomb[0].addBox(-2F, -4F, -2F, 4, 8, 4);
        bomb[1].addTrapezoid(-2F, -6F, -2F, 4, 2, 4, 0F, -1F, ModelRendererTurbo.MR_BOTTOM);
        bomb[2].addTrapezoid(-2F, 4F, -2F, 4, 2, 4, 0F, -1F, ModelRendererTurbo.MR_TOP);
        bomb[3].addTrapezoid(-2F, 6F, -2F, 4, 2, 4, 0F, -1F, ModelRendererTurbo.MR_BOTTOM);
        bomb[4].addBox(-2F, 8F, -2F, 4, 2, 4);
        for (int i = 0; i < blast.length; i++)
        {
            blast[i] = new ModelRendererTurbo(this, 32, 0, 64, 64);
            blast[i].flip = i == 1;
            blast[i].addSphere(0F, 0F, 0F, 16F, 16, 16, 32, 32);
        }
    }

    public void renderBomb(PoseStack pose, VertexConsumer vertices, int light, int overlay)
    {
        for (ModelRendererTurbo part : bomb)
            part.render(pose, vertices, light, overlay, 1F, 1F, 1F, 1F, 1F);
    }

    public void renderBlast(PoseStack pose, VertexConsumer vertices, int light, int overlay, float alpha)
    {
        for (ModelRendererTurbo part : blast)
            part.render(pose, vertices, light, overlay, 1F, 1F, 1F, alpha, 1F);
    }
}
