package com.flansmodultimate.client.render;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource pBuffer, T entity, EquipmentSlot pSlot, int packedLight, int overlay)
    {
        ItemStack itemStack = entity.getItemBySlot(pSlot);
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == pSlot && ModelCache.getOrLoadTypeModel(armorItem.getConfigType()) instanceof ModelCustomArmour modelCustomArmour)
        {
            ResourceLocation texture = armorItem.getConfigType().getTexture();
            getParentModel().copyPropertiesTo((HumanoidModel<T>) modelCustomArmour);
            renderModel(modelCustomArmour, texture, poseStack, pBuffer, packedLight, overlay);
        }
    }

    private void renderModel(ModelCustomArmour model, ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int overlay)
    {
        for (EnumRenderPass renderPass : EnumRenderPass.ORDER)
            model.renderToBuffer(poseStack, buffer.getBuffer(renderPass.getRenderType(texture)), packedLight, overlay, 1F, 1F, 1F, 1F, renderPass);
    }
}