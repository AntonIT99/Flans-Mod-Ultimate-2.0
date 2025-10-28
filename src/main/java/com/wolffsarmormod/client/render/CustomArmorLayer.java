package com.wolffsarmormod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wolffsarmormod.common.item.CustomArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
    public void render(@NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, @NotNull T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch)
    {
        renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.HEAD, pPackedLight);
        renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.LEGS, pPackedLight);
        renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.FEET, pPackedLight);
        renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.CHEST, pPackedLight);
    }

    @SuppressWarnings("unchecked")
    private void renderArmorPiece(PoseStack pPoseStack, MultiBufferSource pBuffer, T pLivingEntity, EquipmentSlot pSlot, int pPackedLight)
    {
        ItemStack itemStack = pLivingEntity.getItemBySlot(pSlot);
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == pSlot)
        {
            getParentModel().copyPropertiesTo((HumanoidModel<T>) armorItem.getModel());
            renderModel(pPoseStack, pBuffer, pPackedLight, armorItem.getModel(), armorItem.getTexture());
        }
    }

    private void renderModel(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Model pModel, ResourceLocation armorResource)
    {
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entityTranslucent(armorResource));
        pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}