package com.flansmodultimate.client.render;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CustomArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M>
{
    public CustomArmorLayer(RenderLayerParent<S, M> parent)
    {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot)
    {
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0F);
        renderArmorPiece(poseStack, collector, state.headEquipment, EquipmentSlot.HEAD, packedLight, overlay, state);
        renderArmorPiece(poseStack, collector, state.legsEquipment, EquipmentSlot.LEGS, packedLight, overlay, state);
        renderArmorPiece(poseStack, collector, state.feetEquipment, EquipmentSlot.FEET, packedLight, overlay, state);
        renderArmorPiece(poseStack, collector, state.chestEquipment, EquipmentSlot.CHEST, packedLight, overlay, state);
    }

    private void renderArmorPiece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack itemStack,
                                  EquipmentSlot slot, int packedLight, int overlay, S state)
    {
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == slot && ModelCache.getOrLoadTypeModel(armorItem.getConfigType()) instanceof ModelCustomArmour model)
        {
            ArmorType armorType = armorItem.getConfigType();
            Identifier texture = armorType.getTexture();
            HumanoidPose pose = HumanoidPose.capture(getParentModel());
            boolean young = state.isBaby;
            boolean showHat = state instanceof AvatarRenderState avatar && avatar.showHat;

            boolean translucent = ModClientConfig.get().useTranslucentRendering(armorType);
            boolean cull = ModClientConfig.get().useCullingRendering(armorType);
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            {
                collector.submitCustomGeometry(poseStack, renderPass.getArmorRenderType(texture, translucent, cull),
                    (submittedPose, vertices) -> {
                        PoseStack deferred = new PoseStack();
                        deferred.last().set(submittedPose);
                        pose.apply(model);
                        model.setYoung(young);
                        setModelPartVisibility(model, slot, showHat);
                        model.renderToBuffer(deferred, vertices, packedLight, overlay,
                            1F, 1F, 1F, 1F, renderPass);
                    });
            }
        }
    }

    private void setModelPartVisibility(ModelCustomArmour model, EquipmentSlot slot, boolean showHat)
    {
        model.head.visible = false;
        model.hat.visible = false;
        model.body.visible = false;
        model.rightArm.visible = false;
        model.leftArm.visible = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible = false;
        
        switch (slot)
        {
            case HEAD ->
            {
                model.head.visible = true;
                model.hat.visible = showHat;
            }
            case CHEST ->
            {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS ->
            {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET ->
            {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default ->
            {
                // no-op
            }
        }
    }

    /**
     * The parent model has already been animated by {@link LivingEntityRenderer} before layers are submitted.
     * Capturing that pose preserves its vanilla pivots and any renderer-specific proportions, which an empty
     * custom model cannot reconstruct by running {@link HumanoidModel#setupAnim(HumanoidRenderState)} itself.
     */
    private record HumanoidPose(PartPose head, PartPose hat, PartPose body, PartPose rightArm,
                                PartPose leftArm, PartPose rightLeg, PartPose leftLeg)
    {
        private static HumanoidPose capture(HumanoidModel<?> model)
        {
            return new HumanoidPose(
                PartPose.capture(model.head),
                PartPose.capture(model.hat),
                PartPose.capture(model.body),
                PartPose.capture(model.rightArm),
                PartPose.capture(model.leftArm),
                PartPose.capture(model.rightLeg),
                PartPose.capture(model.leftLeg));
        }

        private void apply(ModelCustomArmour model)
        {
            head.apply(model.head);
            hat.apply(model.hat);
            body.apply(model.body);
            rightArm.apply(model.rightArm);
            leftArm.apply(model.leftArm);
            rightLeg.apply(model.rightLeg);
            leftLeg.apply(model.leftLeg);
        }
    }

    private record PartPose(float x, float y, float z, float xRot, float yRot, float zRot,
                            float xScale, float yScale, float zScale)
    {
        private static PartPose capture(ModelPart part)
        {
            return new PartPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                part.xScale, part.yScale, part.zScale);
        }

        private void apply(ModelPart part)
        {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
        }
    }
}
