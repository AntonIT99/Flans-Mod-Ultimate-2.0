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
            model.setupAnim(state);
            model.setYoung(state.isBaby);
            setModelPartVisibility(model, slot, state);

            boolean translucent = ModClientConfig.get().useTranslucentRendering(armorType);
            boolean cull = ModClientConfig.get().useCullingRendering(armorType);
            for (EnumRenderPass renderPass : ModelCache.getRenderPasses(model))
            {
                collector.submitCustomGeometry(poseStack, renderPass.getArmorRenderType(texture, translucent, cull),
                    (submittedPose, vertices) -> {
                        PoseStack deferred = new PoseStack();
                        deferred.last().set(submittedPose);
                        model.renderToBuffer(deferred, vertices, packedLight, overlay,
                            1F, 1F, 1F, 1F, renderPass);
                    });
            }
        }
    }

    private void setModelPartVisibility(ModelCustomArmour model, EquipmentSlot slot, S state)
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
                model.hat.visible = state instanceof AvatarRenderState avatar && avatar.showHat;
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
}
