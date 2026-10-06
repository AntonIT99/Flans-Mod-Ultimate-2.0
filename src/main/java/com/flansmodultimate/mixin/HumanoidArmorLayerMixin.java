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

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>>
{
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void skipCustomArmorRendering(PoseStack poseStack, MultiBufferSource pBuffer, T pLivingEntity, EquipmentSlot pSlot, int packedLight, A pModel, CallbackInfo ci)
    {
        if (pLivingEntity.getItemBySlot(pSlot).getItem() instanceof CustomArmorItem)
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;copyPropertiesTo(Lnet/minecraft/client/model/HumanoidModel;)V", shift = At.Shift.AFTER))
    private void restingArmor(PoseStack poseStack, MultiBufferSource buffer, T entity, EquipmentSlot slot, int light, A model, CallbackInfo callback)
    {
        if (entity instanceof IEquipmentPolicy policy && !policy.flansArmorAnimations())
            EquipmentAnimationSupport.restArmor(model);
    }
}
