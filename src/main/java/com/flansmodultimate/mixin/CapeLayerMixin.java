package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.ArmorCapeLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;

/** Hides a player's own cape while their armour draws its CapeTexture instead. */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin
{
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
        at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$hideCapeUnderArmorCape(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                          AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                                                          float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                                          CallbackInfo callback)
    {
        if (ArmorCapeLayer.getCape(player) != null)
            callback.cancel();
    }
}
