package com.flansmodultimate.mixin;

import com.flansmodultimate.common.item.CustomArmorItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Armour hooks on {@link LivingEntity}.
 *
 * <ul>
 * <li>Water walking: lets {@code OnWaterWalking} armor stand on still water. Vanilla asks
 * {@code canStandOnFluid}, as it does for striders on lava, when building a liquid source's
 * collision shape and when choosing fluid or land movement. Both sides run it, so the local
 * player's client-side movement needs no sync.</li>
 * <li>Underwater air: {@code AirSupply} and {@code SnorkelDepth} armor slow or replace the air
 * loss of {@code decreaseAirSupply}; the server's air value is synced to the client as usual.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class LivingEntityWaterWalkingMixin
{
    // Water walking

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateWalkOnWater(FluidState fluid, CallbackInfoReturnable<Boolean> callback)
    {
        if (CustomArmorItem.canWalkOnFluid((LivingEntity) (Object) this, fluid))
            callback.setReturnValue(true);
    }

    // Underwater air

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateArmorAirSupply(int air, CallbackInfoReturnable<Integer> callback)
    {
        int adjusted = CustomArmorItem.adjustUnderwaterAir((LivingEntity) (Object) this, air);
        if (adjusted >= 0)
            callback.setReturnValue(adjusted);
    }
}
