package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.EntityDistancePolicy;
import com.flansmodultimate.common.entity.EntityPairingUpdate;
import com.flansmodultimate.common.entity.EntityTrackingRefresh;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * Hooks into the vanilla chunk map's entity trackers.
 *
 * <ul>
 * <li>Reevaluates every pairing after the distance settings change.</li>
 * <li>Pairs or unpairs one entity with one player at once, for pinned entities.</li>
 * </ul>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(ChunkMap.class)
public abstract class ChunkMapEntityTrackingMixin implements EntityPairingUpdate
{
    @Shadow @Final @SuppressWarnings("MismatchedCollectionQueryUpdate") // Shadowed target map, populated by the target.
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

    @Override
    public void flansmodultimateUpdatePairing(Entity entity, ServerPlayer player)
    {
        if (entityMap.get(entity.getId()) instanceof EntityTrackingRefresh tracker)
            tracker.flansmodultimateUpdatePlayer(player);
    }
}
