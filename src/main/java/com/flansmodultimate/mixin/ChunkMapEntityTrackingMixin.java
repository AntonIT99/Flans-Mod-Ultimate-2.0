package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.EntityDistancePolicy;
import com.flansmodultimate.common.entity.EntityTrackingRefresh;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ChunkMap.class)
public abstract class ChunkMapEntityTrackingMixin
{
    @Shadow @Final
    private Int2ObjectMap<?> entityMap;
    @Shadow @Final
    ServerLevel level;
    @Unique
    private long flansmodultimateDistanceRevision = -1L;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void flansmodultimateRefreshDistances(CallbackInfo callback)
    {
        long revision = EntityDistancePolicy.trackingRevision();
        if (flansmodultimateDistanceRevision == revision)
            return;
        flansmodultimateDistanceRevision = revision;
        for (Object tracker : entityMap.values())
            ((EntityTrackingRefresh) tracker).flansmodultimateRefreshTracking(level.players());
    }
}
