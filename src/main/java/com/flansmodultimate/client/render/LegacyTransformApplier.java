package com.flansmodultimate.client.render;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.util.ClassLoaderUtils;
import com.flansmodultimate.util.TransformOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.IModelBase;
import com.wolffsmod.api.client.model.ModelBase;
import com.wolffsmod.api.client.model.ModelRenderer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Quaternionf;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LegacyTransformApplier
{
    /** Submit a legacy model to the 26.1 extraction renderer. */
    public static void submitModel(IModelBase model, InfoType infoType, Identifier texture, PoseStack poseStack,
                                   SubmitNodeCollector collector, int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha)
    {
        poseStack.pushPose();
        applyModelTransform(model, infoType, poseStack);

        boolean translucent = ModClientConfig.get().useTranslucentRendering(infoType);
        boolean cull = ModClientConfig.get().useCullingRendering(infoType);
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
        {
            RenderType renderType = renderPass.getRenderType(texture, translucent, cull);
            collector.submitCustomGeometry(poseStack, renderType, (submittedPose, vertexConsumer) -> {
                PoseStack deferredPoseStack = new PoseStack();
                deferredPoseStack.last().set(submittedPose);
                renderModelLayer(model, deferredPoseStack, vertexConsumer, packedLight, packedOverlay,
                    red, green, blue, alpha, renderPass);
            });
        }

        poseStack.popPose();
    }

    public static void renderModel(IModelBase model, InfoType infoType, Identifier texture, PoseStack poseStack, RenderTypeBufferSource buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        poseStack.pushPose();
        applyModelTransform(model, infoType, poseStack);

        boolean translucent = ModClientConfig.get().useTranslucentRendering(infoType);
        boolean cull = ModClientConfig.get().useCullingRendering(infoType);
        for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            renderModelLayer(model, poseStack, buffer.getBuffer(renderPass.getRenderType(texture, translucent, cull)), packedLight, packedOverlay, red, green, blue, alpha, renderPass);

        poseStack.popPose();
    }

    /**
     * Apply constructor-time OpenGL transforms captured from a legacy model.
     * Animated renderers use this entry point before drawing selected model
     * parts instead of asking {@link #renderModel} to draw the entire model.
     */
    public static void applyModelTransform(IModelBase model, InfoType infoType, PoseStack poseStack)
    {
        applyForClass(poseStack, model.getClass().getName());
        if (model instanceof ModelBase modelBase)
            modelBase.setScale(infoType.getModelScale());
    }

    private static void renderModelLayer(IModelBase model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
    {
        float modelScale = model instanceof ModelBase modelBase ? modelBase.getScale() : 1F;

        for (ModelRenderer modelRenderer : model.getBoxList())
        {
            if (modelRenderer instanceof ModelRendererTurbo modelRendererTurbo)
            {
                modelRendererTurbo.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
            }
            else if (renderPass == EnumRenderPass.DEFAULT)
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
