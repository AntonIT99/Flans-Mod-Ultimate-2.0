package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;

public class ModelCasing extends ModelBase
{
    protected ModelRendererTurbo[] casingModel = new ModelRendererTurbo[0];

    public void renderCasing(PoseStack poseStack, VertexConsumer pVertexConsumer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for (ModelRendererTurbo model : casingModel)
            if (model != null)
                model.render(poseStack, pVertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
    }

    protected void flipAll()
    {
        for (ModelRendererTurbo casing : casingModel)
        {
            casing.doMirror(false, true, true);
            casing.setRotationPoint(casing.rotationPointX, -casing.rotationPointY, -casing.rotationPointZ);
        }
    }
}
