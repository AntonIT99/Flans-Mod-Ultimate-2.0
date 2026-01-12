package com.flansmodultimate.util;

import com.flansmod.client.model.ModelBomb;
import com.flansmod.client.model.ModelBullet;
import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.render.ERenderPass;
import com.flansmodultimate.common.types.InfoType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LegacyTransformApplier
{
    @OnlyIn(Dist.CLIENT)
    public static void renderModel(IModelBase model, InfoType infoType, ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        poseStack.pushPose();
        try
        {
            applyForClass(poseStack, model.getClass().getName());
            if (model instanceof ModelBase modelBase)
                modelBase.setScale(infoType.getModelScale());

            if (model.getClass() == ModelBullet.class || model.getClass() == ModelBomb.class)
            {
                model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, packedOverlay, red, green, blue, alpha);
            }
            else
            {
                for (ERenderPass renderPass : ERenderPass.ORDER)
                    renderModelLayer(model, poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, packedOverlay, red, green, blue, alpha, renderPass);
            }
        }
        finally
        {
            poseStack.popPose();
        }
    }

    private static void renderModelLayer(IModelBase model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, ERenderPass renderPass)
    {
        float modelScale = model instanceof ModelBase modelBase ? modelBase.getScale() : 1F;

        for (ModelRenderer modelRenderer : model.getBoxList())
        {
            if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo)
            {
                modelRendererTurbo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
            }
            else if (renderPass == ERenderPass.DEFAULT)
            {
                modelRenderer.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale);
            }
        }
    }

    private static void applyForClass(PoseStack poseStack, String legacyClassFqn)
    {
        List<TransformOp> ops = ClassLoaderUtils.getTransforms().get(legacyClassFqn);
        if (ops == null || ops.isEmpty())
            return;

        for (TransformOp op : ops)
        {
            switch (op.kind)
            {
                case TRANSLATE -> poseStack.translate(op.args[0], op.args[1], op.args[2]);
                case SCALE -> poseStack.scale(op.args[0], op.args[1], op.args[2]);
                case ROTATE -> {
                    float angle = op.args[0];
                    float x = op.args[1];
                    float y = op.args[2];
                    float z = op.args[3];

                    if (x == 1 && y == 0 && z == 0)
                    {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
                    }
                    else if (x == 0 && y == 1 && z == 0)
                    {
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
                    }
                    else if (x == 0 && y == 0 && z == 1)
                    {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
                    }
                    else
                    {
                        float len = (float) Math.sqrt(x * x + y * y + z * z);
                        if (len != 0.0f)
                        {
                            float nx = x / len;
                            float ny = y / len;
                            float nz = z / len;
                            Quaternionf q = new Quaternionf().fromAxisAngleRad(nx, ny, nz, (float) Math.toRadians(angle));
                            poseStack.mulPose(q);
                        }
                    }
                }
            }
        }
    }
}
