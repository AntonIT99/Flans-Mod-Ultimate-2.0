package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.FlansMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;

public class ModelDefaultMuzzleFlash extends ModelMuzzleFlash
{
    public ModelRendererTurbo mfModel[];

    public ModelDefaultMuzzleFlash()
    {
        mfModel = new ModelRendererTurbo[3];

        mfModel[0] = new ModelRendererTurbo(this, 0, 0, 16, 16);
        mfModel[1] = new ModelRendererTurbo(this, 0, 0, 16, 16);

        mfModel[2] = new ModelRendererTurbo(this, 0, 8, 16, 16);

        mfModel[0].addBox(0f, -2f, -2f, 0, 4, 4);
        mfModel[1].addBox(0f, 0f, -2f, 4, 0, 4);
        mfModel[2].addBox(0f, -2f, 0f, 4, 4, 0);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        for (ModelRendererTurbo mod : mfModel)
        {
            mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale());
        }
    }

    @Override
    public ResourceLocation getTexture()
    {
        return FlansMod.muzzleFlashTexture;
    }
}
