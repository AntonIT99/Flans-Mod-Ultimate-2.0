package com.flansmodultimate.client.render;

import com.flansmod.client.model.ModelCustomArmour;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
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
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource pBuffer, int packedLight, @NotNull T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch)
    {
        renderArmorPiece(poseStack, pBuffer, pLivingEntity, EquipmentSlot.HEAD, packedLight);
        renderArmorPiece(poseStack, pBuffer, pLivingEntity, EquipmentSlot.LEGS, packedLight);
        renderArmorPiece(poseStack, pBuffer, pLivingEntity, EquipmentSlot.FEET, packedLight);
        renderArmorPiece(poseStack, pBuffer, pLivingEntity, EquipmentSlot.CHEST, packedLight);
    }

    @SuppressWarnings("unchecked")
    private void renderArmorPiece(PoseStack poseStack, MultiBufferSource pBuffer, T pLivingEntity, EquipmentSlot pSlot, int packedLight)
    {
        ItemStack itemStack = pLivingEntity.getItemBySlot(pSlot);
        Item item = itemStack.getItem();

        if (item instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == pSlot && ModelCache.getOrLoadTypeModel(armorItem.getConfigType()) instanceof ModelCustomArmour modelCustomArmour)
        {
            ResourceLocation texture = armorItem.getConfigType().getTexture();
            getParentModel().copyPropertiesTo((HumanoidModel<T>) modelCustomArmour);
            renderModel(poseStack, pBuffer, packedLight, modelCustomArmour, texture);
        }
    }

    private void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ModelCustomArmour model, ResourceLocation texture)
    {
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F, false);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderTypes.emissiveGlowAdditiveDepthWrite(texture)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F, true);
    }
}