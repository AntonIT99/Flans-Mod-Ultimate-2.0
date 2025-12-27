package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;

public class ModelCasing extends ModelBase
{
    protected ModelRendererTurbo[] casingModel = new ModelRendererTurbo[0];

    public void renderCasing(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale, boolean glowingParts)
    {
        for (ModelRendererTurbo model : casingModel)
            if (model != null)
                model.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, glowingParts);
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
