package com.flansmodultimate.mixin;

import com.flansmodultimate.api.IEquipmentPolicy;
import com.flansmodultimate.client.model.EquipmentAnimationSupport;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Custom armor ownership and resting limb poses for the NeoForge armor rendering overload. */
@SuppressWarnings("java:S107") // Mixin callbacks must match the complete target method signature.
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>>
{
    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateSkipCustomArmorRendering(PoseStack poseStack, MultiBufferSource pBuffer, T livingEntity, EquipmentSlot slot, int packedLight, A model, float limbSwing,
        float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if (livingEntity.getItemBySlot(slot).getItem() instanceof CustomArmorItem)
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;copyPropertiesTo(Lnet/minecraft/client/model/HumanoidModel;)V", shift = At.Shift.AFTER))
    private void flansmodultimateRestingArmor(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, A model, float limbSwing, float limbSwingAmount,
        float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback)
    {
        if (entity instanceof IEquipmentPolicy policy && !policy.flansArmorAnimations())
            EquipmentAnimationSupport.restArmor(model);
    }
}
