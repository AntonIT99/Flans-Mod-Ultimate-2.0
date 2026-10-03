package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.EntityDistancePolicy;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The owning NPC is culled before its detached Flan model renderer is called. */
@Mixin(Entity.class)
public abstract class NpcRenderDistanceMixin
{
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$npcDistance(double distanceSquared, CallbackInfoReturnable<Boolean> result)
    {
        Entity entity = (Entity)(Object)this;
        if (EntityDistancePolicy.isFlanNpc(entity))
        {
            double distance = EntityDistancePolicy.renderDistance(entity, true);
            result.setReturnValue(distanceSquared < distance * distance);
        }
    }
}
