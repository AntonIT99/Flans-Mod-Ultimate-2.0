package com.flansmodultimate.common.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Internal bridge into the vanilla chunk map, to pair or unpair one entity with one player at once. */
public interface EntityPairingUpdate
{
    void flansmodultimateUpdatePairing(Entity entity, ServerPlayer player);
}
