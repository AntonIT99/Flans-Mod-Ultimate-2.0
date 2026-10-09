package com.flansmodultimate.mixin;

import com.flansmodultimate.config.ModCommonConfig;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityNameTagRangeMixin
{
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
