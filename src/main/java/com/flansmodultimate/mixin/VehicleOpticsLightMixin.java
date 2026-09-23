package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.VehicleOpticsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Optical night vision affects only this client's lightmap and never grants/removes a potion effect. */
@Mixin(LightTexture.class)
public abstract class VehicleOpticsLightMixin
{
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean flansmodultimate$opticalNightVision(LocalPlayer player, Holder<MobEffect> effect)
    {
        return effect == MobEffects.NIGHT_VISION && VehicleOpticsClient.nightVision() || player.hasEffect(effect);
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float flansmodultimate$opticalNightVisionStrength(LivingEntity player, float partialTick)
    {
        return VehicleOpticsClient.nightVision() ? 1F : GameRenderer.getNightVisionScale(player, partialTick);
    }
}
