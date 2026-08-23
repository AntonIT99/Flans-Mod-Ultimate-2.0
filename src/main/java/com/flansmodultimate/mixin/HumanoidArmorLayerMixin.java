package com.flansmodultimate.mixin;

import com.flansmodultimate.common.item.CustomArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin
{
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void skipCustomArmorRendering(PoseStack poseStack, SubmitNodeCollector collector,
                                          ItemStack itemStack, EquipmentSlot slot, int packedLight,
                                          HumanoidRenderState state, CallbackInfo ci)
    {
        if (itemStack.getItem() instanceof CustomArmorItem)
            ci.cancel();
    }
}
