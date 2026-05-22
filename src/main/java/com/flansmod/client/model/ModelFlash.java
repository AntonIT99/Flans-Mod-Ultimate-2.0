package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import org.jetbrains.annotations.NotNull;

public class ModelFlash extends ModelBase
{
    protected ModelRendererTurbo[][] flashModel = new ModelRendererTurbo[0][0];

    public void renderFlash(int flashIndex, PoseStack poseStack, VertexConsumer pVertexConsumer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        if (flashModel[flashIndex] != null)
            render(flashModel[flashIndex], poseStack, pVertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
    }

    public void render(ModelRendererTurbo[] flash, PoseStack poseStack, VertexConsumer pVertexConsumer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale)
    {
        for (ModelRendererTurbo model : flash)
            if (model != null)
                model.render(poseStack, pVertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, scale);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        for (ModelRendererTurbo[] mods : flashModel)
        {
            for (ModelRendererTurbo mod : mods)
            {
                if (mod != null)
                    mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale());
            }
        }
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
