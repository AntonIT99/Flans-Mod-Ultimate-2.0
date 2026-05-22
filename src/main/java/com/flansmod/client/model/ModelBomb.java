package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import org.jetbrains.annotations.NotNull;

public class ModelBomb extends ModelBase
{
    protected ModelRendererTurbo[] bombModel;

    public ModelBomb()
    {
        super();

        bombModel = new ModelRendererTurbo[4];

        bombModel[0] = new ModelRendererTurbo(this, 104, 0, 128, 64);
        bombModel[1] = new ModelRendererTurbo(this, 104, 0, 128, 64);

        bombModel[2] = new ModelRendererTurbo(this, 56, 8, 128, 64);
        bombModel[3] = new ModelRendererTurbo(this, 56, 8, 128, 64);

        bombModel[0].addTrapezoid(-2F, 0F, -2F, 4, 1, 4, 0.0F, 1.0F, ModelRendererTurbo.MR_TOP);
        bombModel[1].addBox(-2F, 1F, -2F, 4, 6, 4, 0.0F);
        bombModel[2].addTrapezoid(-2F, 7F, -2F, 4, 1, 4, 0.0F, 1.0F, ModelRendererTurbo.MR_BOTTOM);
        bombModel[3].addBox(-2F, 8F, -2F, 4, 2, 4, 0.0F);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        for (ModelRendererTurbo mod : bombModel)
        {
            mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale());
        }
    }
}
