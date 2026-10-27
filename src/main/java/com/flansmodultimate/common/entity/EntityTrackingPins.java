package com.flansmodultimate.common.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entities kept sent to one player whatever the distance, at most one per player. Server thread only. Pairings change at once, so
 * a packet naming the entity can follow the pin.
 */
public final class EntityTrackingPins
{
    private static final Map<UUID, Entity> PINS = new HashMap<>();

    private EntityTrackingPins()
    {}

    /** Pins the entity for the player, replacing an earlier pin; {@code null} only removes the pin. */
    public static void pin(ServerPlayer player, @Nullable Entity entity)
    {
        Entity previous = entity == null ? PINS.remove(player.getUUID()) : PINS.put(player.getUUID(), entity);
        if (previous != null && previous != entity)
            updatePairing(previous, player);
        if (entity != null)
            updatePairing(entity, player);
    }

    public static boolean isPinned(Entity entity, ServerPlayer player)
    {
        return !PINS.isEmpty() && PINS.get(player.getUUID()) == entity;
    }

    public static void unpin(UUID player)
    {
        PINS.remove(player);
    }

    public static void clear()
    {
        PINS.clear();
    }

    private static void updatePairing(Entity entity, ServerPlayer player)
    {
        if (entity.level() instanceof ServerLevel level && player.level() == level)
            ((EntityPairingUpdate) level.getChunkSource().chunkMap).flansmodultimateUpdatePairing(entity, player);
    }
}
