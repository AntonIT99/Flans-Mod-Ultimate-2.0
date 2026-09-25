package com.flansmodultimate.mixin;

import com.flansmodultimate.common.item.CustomArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;

/**
 * Lets {@code OnWaterWalking} armor stand on still water.
 *
 * <p>Vanilla asks this method, as it does for striders on lava, when building a
 * liquid source's collision shape and when choosing fluid or land movement. Both
 * sides run it, so the local player's client-side movement needs no sync.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWaterWalkingMixin
{
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$walkOnWater(FluidState fluid, CallbackInfoReturnable<Boolean> callback)
    {
        if (CustomArmorItem.canWalkOnFluid((LivingEntity) (Object) this, fluid))
            callback.setReturnValue(true);
    }
}
