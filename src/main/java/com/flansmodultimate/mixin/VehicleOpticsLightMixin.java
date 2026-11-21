package com.flansmodultimate.mixin;

import com.flansmodultimate.client.render.LightmapPixels;
import com.flansmodultimate.client.render.thermal.ThermalVision;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hooks into the lightmap:
 * <ul>
 * <li>Optical night vision affects only this client's lightmap and never grants/removes a potion effect.</li>
 * <li>Exposes the lightmap pixels, which light distant driveable pictures drawn outside the vanilla shaders.</li>
 * </ul>
 */
@Mixin(LightTexture.class)
public abstract class VehicleOpticsLightMixin implements LightmapPixels
{
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private boolean flansmodultimateOpticalNightVision(LocalPlayer player, MobEffect effect)
    {
        return effect == MobEffects.NIGHT_VISION && ThermalVision.opticalNightVision() || player.hasEffect(effect);
    }

    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float flansmodultimateOpticalNightVisionStrength(LivingEntity player, float partialTick)
    {
        return ThermalVision.opticalNightVision() ? 1F : GameRenderer.getNightVisionScale(player, partialTick);
    }

    @Override
    public NativeImage flansmodultimateLightPixels()
    {
        return lightPixels;
    }
}
