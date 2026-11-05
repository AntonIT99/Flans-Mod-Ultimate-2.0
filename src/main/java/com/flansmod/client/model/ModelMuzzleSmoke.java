package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelBase;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** A Tyrants and Plebeians smoke puff model, used by the {@code MuzzleSmokeModel} and {@code ChamberSmokeModel} gun settings. */
public class ModelMuzzleSmoke extends ModelBase
{
    protected ModelRendererTurbo[] smokeModel = new ModelRendererTurbo[0];

    @SuppressWarnings("java:S107") // Mirrors ModelCasing.renderCasing, which passes the render state explicitly
    public void renderSmoke(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float scale,
        EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo model : smokeModel)
            if (model != null)
                model.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, scale, renderPass);
    }

    protected void flipAll()
    {
        for (ModelRendererTurbo model : smokeModel)
        {
            model.doMirror(false, true, true);
            model.setRotationPoint(model.rotationPointX, -model.rotationPointY, -model.rotationPointZ);
        }
    }
}
