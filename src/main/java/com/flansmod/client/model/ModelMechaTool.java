package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.types.MechaItemType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wolffsmod.api.client.model.ModelBase;
import lombok.Getter;
import lombok.Setter;

/** Pass-aware model base used by legacy mecha tools and upgrades. */
@SuppressWarnings({"unused", "java:S1104"})
public class ModelMechaTool extends ModelBase implements IFlanTypeModel<MechaItemType>
{
    private static final float MODEL_UNIT = 1F / 16F;

    /** Common, non-moving tool body. */
    public ModelRendererTurbo[] baseModel = new ModelRendererTurbo[0];
    /** Parts rotating around the tool's longitudinal axis. */
    public ModelRendererTurbo[] drillModel = new ModelRendererTurbo[0];
    /** Parts rotating around their individual Y-axis pivots. */
    public ModelRendererTurbo[] sawModel = new ModelRendererTurbo[0];

    @Getter @Setter
    protected MechaItemType type;

    @Override
    public Class<MechaItemType> typeClass()
    {
        return MechaItemType.class;
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                       float red, float green, float blue, float alpha, float scale, EnumRenderPass renderPass)
    {
        renderPart(baseModel, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
    }

    public void renderDrill(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha, float scale, float spinDegrees,
                            EnumRenderPass renderPass)
    {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(spinDegrees));
        renderPart(drillModel, poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        poseStack.popPose();
    }

    public void renderSaw(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha, float scale, float spinDegrees,
                          EnumRenderPass renderPass)
    {
        if (sawModel == null)
            return;

        for (ModelRendererTurbo part : sawModel)
        {
            if (part == null)
                continue;

            poseStack.pushPose();
            float pivotX = part.rotationPointX * MODEL_UNIT * scale;
            float pivotY = part.rotationPointY * MODEL_UNIT * scale;
            float pivotZ = part.rotationPointZ * MODEL_UNIT * scale;
            poseStack.translate(pivotX, pivotY, pivotZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));
            poseStack.translate(-pivotX, -pivotY, -pivotZ);
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha, scale, renderPass);
            poseStack.popPose();
        }
    }

    public void renderAll(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha, float scale, float spinDegrees,
                          EnumRenderPass renderPass)
    {
        render(poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, renderPass);
        renderDrill(poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, spinDegrees, renderPass);
        renderSaw(poseStack, vertexConsumer, packedLight, packedOverlay,
            red, green, blue, alpha, scale, spinDegrees, renderPass);
    }

    private static void renderPart(ModelRendererTurbo[] parts, PoseStack poseStack, VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay, float red, float green, float blue, float alpha,
                                   float scale, EnumRenderPass renderPass)
    {
        if (parts == null)
            return;
        for (ModelRendererTurbo part : parts)
        {
            if (part != null)
                part.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                    red, green, blue, alpha, scale, renderPass);
        }
    }
}
