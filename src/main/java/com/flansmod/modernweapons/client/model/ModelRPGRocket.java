package com.flansmod.modernweapons.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelBase;
import org.jetbrains.annotations.NotNull;

public class ModelRPGRocket extends ModelBase
{
	public ModelRendererTurbo[] bulletModel;
	
	public ModelRPGRocket()
	{
		int textureX = 64;
		int textureY = 32;
		
		bulletModel = new ModelRendererTurbo[3];
		
		bulletModel[0] = new ModelRendererTurbo(this, 14, 12, textureX, textureY);
		bulletModel[0].addBox(-1.5F, 0F, -1.5F, 3, 1, 3);
		
		bulletModel[1] = new ModelRendererTurbo(this, 26, 9, textureX, textureY);
		bulletModel[1].addTrapezoid(-2F, 1F, -2F, 4, 3, 4, 0F, -1F, ModelRendererTurbo.MR_TOP);
		
		bulletModel[2] = new ModelRendererTurbo(this, 38, 6, textureX, textureY);
		bulletModel[2].addTrapezoid(-2F, 4F, -2F, 4, 3, 4, 0F, -1F, ModelRendererTurbo.MR_BOTTOM);
	}

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        for (ModelRendererTurbo mrt : bulletModel)
            mrt.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, getScale());
        poseStack.popPose();
    }
}
