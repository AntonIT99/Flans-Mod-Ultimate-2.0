package com.flansmodultimate.mixin;

import com.flansmodultimate.api.ILivingVisualEffects;
import com.flansmodultimate.config.ModCommonConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/** Controls living-entity name range, optional hurt overlays and optional death tipping. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityNameTagRangeMixin
{
    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
    private static void flansmodultimateHurtOverlay(LivingEntity entity, float whiteOverlay, CallbackInfoReturnable<Integer> callback)
    {
        if (entity instanceof ILivingVisualEffects effects && !effects.flansmodultimateHurtFlash())
            callback.setReturnValue(OverlayTexture.pack(OverlayTexture.u(whiteOverlay), OverlayTexture.v(false)));
    }

    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateDeathRotation(LivingEntity entity, CallbackInfoReturnable<Float> callback)
    {
        if (entity instanceof ILivingVisualEffects effects && !effects.flansmodultimateDeathRotation())
            callback.setReturnValue(0F);
    }

    @ModifyConstant(method = "shouldShowName*", constant = @Constant(floatValue = 64F))
    private float flansmodultimateNormalNameTagRange(float vanillaRange)
    {
        return ModCommonConfig.nameTagRenderRange(false);
    }

    @ModifyConstant(method = "shouldShowName*", constant = @Constant(floatValue = 32F))
    private float flansmodultimateSneakingNameTagRange(float vanillaRange)
    {
        return ModCommonConfig.nameTagRenderRange(true);
    }
}
