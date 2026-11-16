package com.flansmodultimate.mixin;

import com.flansmodultimate.common.entity.EntityDistancePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;

/**
 * Hooks into the server's chunk ticket manager.
 *
 * <ul>
 * <li>Extends the player chunk tickets to the simulation distance when it exceeds the view distance, so that
 * driveables beyond the view distance stay loaded, ticking and tracked (see
 * {@link EntityDistancePolicy#playerTicketRadius(int, int)}). Chunks are still sent only within the view distance.</li>
 * </ul>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin
{
    @Shadow
    private int simulationDistance;

    /** The view distance the chunk map last set, or -1 before it does. Server thread only. */
    @Unique
    private int flansmodultimateViewDistance = -1;
    /** The player ticket radius last applied. Server thread only. */
    @Unique
    private int flansmodultimateTicketRadius = -1;

    @Shadow
    protected abstract void updatePlayerTickets(int viewDistance);

    @ModifyVariable(method = "updatePlayerTickets", at = @At("HEAD"), argsOnly = true)
    private int flansmodultimateLoadToSimulationDistance(int viewDistance)
    {
        flansmodultimateViewDistance = viewDistance;
        flansmodultimateTicketRadius = EntityDistancePolicy.playerTicketRadius(viewDistance, simulationDistance);
        return flansmodultimateTicketRadius;
    }

    /** Simulation distance and setting changes do not pass through the view distance update; apply them here. */
    @Inject(method = "runAllUpdates", at = @At("HEAD"))
    private void flansmodultimateRefreshTicketRadius(ChunkMap chunkMap, CallbackInfoReturnable<Boolean> callback)
    {
        if (flansmodultimateViewDistance >= 0 && EntityDistancePolicy.playerTicketRadius(flansmodultimateViewDistance, simulationDistance) != flansmodultimateTicketRadius)
            updatePlayerTickets(flansmodultimateViewDistance);
    }
}
