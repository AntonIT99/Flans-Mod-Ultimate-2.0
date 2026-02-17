package com.flansmod.client.model;

import com.flansmod.client.tmt.ModelRendererTurbo;
import com.flansmodultimate.client.model.IFlanTypeModel;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.common.types.ArmorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsmod.api.client.model.ModelRenderer;
import com.wolffsmod.api.client.model.TextureOffset;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelCustomArmour extends HumanoidModel<LivingEntity> implements IFlanTypeModel<ArmorType>
{
    @Getter @Setter
    protected ArmorType type;

    protected ModelRendererTurbo[] headModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] bodyModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] leftArmModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] rightArmModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] leftLegModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] rightLegModel = new ModelRendererTurbo[0];
    protected ModelRendererTurbo[] skirtFrontModel = new ModelRendererTurbo[0]; //Acts like a leg piece, but its pitch is set to the maximum of the two legs
    protected ModelRendererTurbo[] skirtRearModel = new ModelRendererTurbo[0]; //Acts like a leg piece, but its pitch is set to the minimum of the two legs

    @Getter
    private final List<ModelRenderer> boxList = new ArrayList<>();
    @Getter
    private final Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    @Getter @Setter
    private ResourceLocation texture;

    @Setter
    private boolean scaleHead = true;
    @Setter
    private float babyYHeadOffset = 16F;
    @Setter
    private float babyZHeadOffset;
    @Setter
    private float babyHeadScale = 2F;
    @Setter
    private float babyBodyScale = 2F;
    @Setter
    private float bodyYOffset = 24F;

    public ModelCustomArmour()
    {
        super(initRoot());
    }

    private static ModelPart initRoot()
    {
        Map<String, ModelPart> children = new HashMap<>();
        children.put("head", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("hat", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("body", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("right_arm", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("left_arm", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("right_leg", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("left_leg", new ModelPart(new ArrayList<>(), new HashMap<>()));
        return new ModelPart(new ArrayList<>(), children);
    }

    @Override
    public Class<ArmorType> typeClass()
    {
        return ArmorType.class;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, EnumRenderPass.DEFAULT);
    }

    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, EnumRenderPass renderPass)
    {
        float modelScale = type != null ? type.getModelScale() : 1F;

        if (young)
        {
            poseStack.pushPose();
            if (scaleHead)
            {
                float f = 1.5F / babyHeadScale;
                poseStack.scale(f, f, f);
            }
            poseStack.translate(0.0F, babyYHeadOffset * 0.0625F, babyZHeadOffset * 0.0625F);
            renderHeadModels(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
            poseStack.popPose();

            poseStack.pushPose();
            float f1 = 1.0F / babyBodyScale;
            poseStack.scale(f1, f1, f1);
            poseStack.translate(0.0F, bodyYOffset * 0.0625F, 0.0F);
            renderBodyModels(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
            poseStack.popPose();
        }
        else
        {
            renderHeadModels(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
            renderBodyModels(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha, modelScale, renderPass);
        }
    }

    protected void renderHeadModels(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float modelScale, EnumRenderPass renderPass)
    {
        render(headModel, head, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
    }

    protected void renderBodyModels(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float modelScale, EnumRenderPass renderPass)
    {
        render(bodyModel, body, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
        render(leftArmModel, leftArm, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
        render(rightArmModel, rightArm, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
        render(leftLegModel, leftLeg, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
        render(rightLegModel, rightLeg, poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale, renderPass);
        for (ModelRendererTurbo mod : skirtFrontModel)
        {
            mod.rotationPointX = (leftLeg.x + rightLeg.x) / 2F / modelScale;
            mod.rotationPointY = (leftLeg.y + rightLeg.y) / 2F / modelScale;
            mod.rotationPointZ = (leftLeg.z + rightLeg.z) / 2F / modelScale;
            mod.rotateAngleX = Math.min(leftLeg.xRot, rightLeg.xRot);
            mod.rotateAngleY = leftLeg.yRot;
            mod.rotateAngleZ = leftLeg.zRot;
            mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale);
        }
        for (ModelRendererTurbo mod : skirtRearModel)
        {
            mod.rotationPointX = (leftLeg.x + rightLeg.x) / 2F / modelScale;
            mod.rotationPointY = (leftLeg.y + rightLeg.x) / 2F / modelScale;
            mod.rotationPointZ = (leftLeg.z + rightLeg.z) / 2F / modelScale;
            mod.rotateAngleX = Math.max(leftLeg.xRot, rightLeg.xRot);
            mod.rotateAngleY = leftLeg.yRot;
            mod.rotateAngleZ = leftLeg.zRot;
            mod.render(poseStack, vertexConsumer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, modelScale);
        }
    }

    public void render(ModelRendererTurbo[] models, ModelPart bodyPart, PoseStack poseStack, VertexConsumer pBuffer, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, float scale, EnumRenderPass renderPass)
    {
        for (ModelRendererTurbo mod : models)
        {
            poseStack.pushPose();
            bodyPart.translateAndRotate(poseStack);
            mod.render(poseStack, pBuffer, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha, scale, renderPass);
            poseStack.popPose();
        }
    }
}
