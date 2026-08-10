package com.flansmodultimate.client.render;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@OnlyIn(Dist.CLIENT)
public class CustomArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    public CustomArmorLayer(RenderLayerParent<T, M> parent)
    {
        super(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0F);
        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.HEAD, packedLight, overlay);
        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.LEGS, packedLight, overlay);
        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.FEET, packedLight, overlay);
        renderArmorPiece(poseStack, buffer, entity, EquipmentSlot.CHEST, packedLight, overlay);
    }

    @SuppressWarnings("unchecked")
    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int packedLight, int overlay)
    {
        ItemStack itemStack = entity.getItemBySlot(slot);
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == slot && ModelCache.getOrLoadTypeModel(armorItem.getConfigType()) instanceof ModelCustomArmour modelCustomArmour)
        {
            ArmorType armorType = armorItem.getConfigType();
            ResourceLocation texture = armorType.getTexture();
            getParentModel().copyPropertiesTo((HumanoidModel<T>) modelCustomArmour);
            
            setModelPartVisibility(modelCustomArmour, slot, entity);

            boolean translucent = ModClientConfig.get().useTranslucentRendering(armorType);
            boolean cull = ModClientConfig.get().useCullingRendering(armorType);
            for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
                modelCustomArmour.renderToBuffer(poseStack, buffer.getBuffer(renderPass.getArmorRenderType(texture, translucent, cull)), packedLight, overlay, 1F, 1F, 1F, 1F, renderPass);
        }
    }

    private void setModelPartVisibility(ModelCustomArmour model, EquipmentSlot slot, LivingEntity entity)
    {
        model.setAllVisible(false);
        
        switch (slot)
        {
            case HEAD ->
            {
                model.head.visible = true;
                model.hat.visible = entity instanceof Player player && player.isModelPartShown(PlayerModelPart.HAT);
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
