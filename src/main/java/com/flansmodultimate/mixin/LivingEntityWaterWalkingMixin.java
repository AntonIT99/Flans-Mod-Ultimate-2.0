package com.flansmodultimate.mixin;

import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.platform.neoforge.NeoForgeDamageContext;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;

import java.util.Stack;

/**
 * Hooks on {@link LivingEntity}.
 *
 * <ul>
 * <li>Hurt timing: restores Forge pre-armor hurt timing after shields and cooldown.</li>
 * <li>Water walking: lets {@code OnWaterWalking} armor stand on still water. Vanilla asks
 * {@code canStandOnFluid}, as it does for striders on lava, when building a liquid source's
 * collision shape and when choosing fluid or land movement. Both sides run it, so the local
 * player's client-side movement needs no sync.</li>
 * <li>Underwater air: {@code AirSupply} and {@code SnorkelDepth} armor slow or replace the air
 * loss of {@code decreaseAirSupply}; the server's air value is synced to the client as usual.
 * NeoForge's {@code baseTick} derives the {@code LivingBreatheEvent} consume amount from
 * {@code decreaseAirSupply} every tick, so a snorkel's refill becomes a negative consumption.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class LivingEntityWaterWalkingMixin
{
    @Shadow(remap = false)
    protected Stack<DamageContainer> damageContainers;

    // NeoForge hurt timing: keep the shared pre-armor behavior after shield and cooldown handling.
    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/util/Stack;peek()Ljava/lang/Object;", ordinal = 0, remap = false), cancellable = true)
    private void flansmodultimateApplyLivingHurt(DamageSource source, float amount, CallbackInfo callback)
    {
        LivingEntity entity = (LivingEntity) (Object) this;
        NeoForgeDamageContext damage = new NeoForgeDamageContext(entity, damageContainers.peek());
        CommonEventHandler.applyLivingHurt(damage);
        if (damage.isCanceled() || damage.amount() <= 0F)
            callback.cancel();
    }

    // Water-walking armor collision and movement.
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
