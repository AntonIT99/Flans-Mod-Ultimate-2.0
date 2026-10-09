package com.flansmodultimate.mixin;

import com.flansmodultimate.client.ModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

/** Applies Mecha force-dark upgrades without mutating chunks or light data. */
@Mixin(LightEngine.class)
public abstract class ForceDarkLightMixin
{
    // The (Object) cast is required, and the check can be true: the mixin class is unrelated to SkyLightEngine until merged.
    @SuppressWarnings({"java:S1905", "ConstantValue"})
    @Inject(method = "getLightValue", at = @At("RETURN"), cancellable = true)
    private void flansmodultimateApplyForceDark(BlockPos pos, CallbackInfoReturnable<Integer> callback)
    {
        if ((Object) this instanceof SkyLightEngine)
            callback.setReturnValue(ModClient.applyForceDarkSkyLight(pos, callback.getReturnValueI()));
    }
}
