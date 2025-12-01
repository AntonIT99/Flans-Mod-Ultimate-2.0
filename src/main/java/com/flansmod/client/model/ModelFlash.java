package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;

public class ModelFlash extends ModelBase
{
    protected ModelRendererTurbo[][] flashModel = new ModelRendererTurbo[0][0];

    public void renderFlash(int flashIndex, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        if (flashModel[flashIndex] != null)
            render(flashModel[flashIndex], pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
    }

    public void render(ModelRendererTurbo[] flash, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for (ModelRendererTurbo model : flash)
            if (model != null)
                model.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
    }

    protected void flipAll()
    {
        for(ModelRendererTurbo[] model : flashModel)
        {
            for (ModelRendererTurbo flash : model)
            {
                flash.doMirror(false, true, true);
                flash.setRotationPoint(flash.rotationPointX, -flash.rotationPointY, -flash.rotationPointZ);
            }
        }
    }
}
