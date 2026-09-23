package com.flansmodultimate.mixin;

import com.flansmodultimate.config.ModCommonConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityNameTagRangeMixin
{
    @ModifyConstant(method = "shouldShowName", constant = @Constant(floatValue = 64F))
    private float flansmodultimate$normalNameTagRange(float vanillaRange)
    {
        return ModCommonConfig.nameTagRenderRange(false);
    }

    @ModifyConstant(method = "shouldShowName", constant = @Constant(floatValue = 32F))
    private float flansmodultimate$sneakingNameTagRange(float vanillaRange)
    {
        return ModCommonConfig.nameTagRenderRange(true);
    }
}
