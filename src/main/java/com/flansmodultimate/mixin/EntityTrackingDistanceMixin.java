package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.EntityDistancePolicy;
import com.flansmodultimate.common.entity.EntityTrackingRefresh;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Override the cached registration range only for managed entities, before vanilla server scaling. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class EntityTrackingDistanceMixin implements EntityTrackingRefresh
{
    @Shadow @Final
    Entity entity;
    @Shadow @Final
    private int range;
    @Shadow
    protected abstract int scaledRange(int range);

    @Shadow
    public abstract void updatePlayers(List<ServerPlayer> players);

    @Inject(method = "getEffectiveRange", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateEffectiveRange(CallbackInfoReturnable<Integer> result)
    {
        int effectiveRange = EntityDistancePolicy.trackingRange(entity);
        boolean managed = effectiveRange >= 0;
        if (!managed)
            effectiveRange = range;
        // Keep vanilla's passenger rule, including managed seats and wheels at their parent's range.
        for (Entity passenger : entity.getIndirectPassengers())
        {
            int passengerRange = EntityDistancePolicy.trackingRange(passenger);
            managed |= passengerRange >= 0;
            effectiveRange = Math.max(effectiveRange, passengerRange < 0 ? passenger.getType().clientTrackingRange() * 16 : passengerRange);
        }
        if (managed)
            result.setReturnValue(scaledRange(effectiveRange));
    }

    @Override
    public void flansmodultimateRefreshTracking(List<ServerPlayer> players)
    {
        updatePlayers(players);
    }
}
