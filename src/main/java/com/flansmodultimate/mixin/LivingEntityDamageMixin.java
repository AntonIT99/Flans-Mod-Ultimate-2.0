package com.flansmodultimate.mixin;

import com.flansmodultimate.event.handler.CommonEventHandler;
import com.flansmodultimate.platform.neoforge.NeoForgeDamageContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Stack;

/** Restores Forge's pre-armor LivingHurtEvent timing after shields and cooldown. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin
{
    @Shadow(remap = false)
    protected Stack<DamageContainer> damageContainers;

    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE",
        target = "Ljava/util/Stack;peek()Ljava/lang/Object;", ordinal = 0, remap = false), cancellable = true)
    private void flansmodultimate$applyLivingHurt(DamageSource source, float amount, CallbackInfo callback)
    {
        LivingEntity entity = (LivingEntity) (Object) this;
        NeoForgeDamageContext damage = new NeoForgeDamageContext(entity, damageContainers.peek());
        CommonEventHandler.applyLivingHurt(damage);
        if (damage.isCanceled() || damage.amount() <= 0F)
            callback.cancel();
    }
}
