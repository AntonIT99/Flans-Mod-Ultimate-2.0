package com.flansmodultimate.common.driveables.collision;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Session-only opt-out from physical driveable hull collision, per player or for every entity. */
public final class DriveableCollisionBypass
{
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static volatile boolean allEntities;

    private DriveableCollisionBypass()
    {}

    public static void setEnabled(Player player, boolean enabled)
    {
        if (enabled)
            ENABLED_PLAYERS.add(player.getUUID());
        else
            ENABLED_PLAYERS.remove(player.getUUID());
    }

    /** Whether this player has their own bypass, regardless of the all-entities bypass. */
    public static boolean isEnabledForPlayer(Player player)
    {
        return ENABLED_PLAYERS.contains(player.getUUID());
    }

    public static void setAllEntities(boolean enabled)
    {
        allEntities = enabled;
    }

    public static boolean isAllEntities()
    {
        return allEntities;
    }

    public static boolean isEnabled(@Nullable Entity entity)
    {
        if (entity == null)
            return false;
        return allEntities || entity instanceof Player player && ENABLED_PLAYERS.contains(player.getUUID());
    }

    /** Clears a player's own bypass when their server session ends. */
    public static void clear(@Nullable Entity entity)
    {
        if (entity instanceof Player player)
            ENABLED_PLAYERS.remove(player.getUUID());
    }

    /** Clears every bypass, when the server stops or the client disconnects. */
    public static void reset()
    {
        ENABLED_PLAYERS.clear();
        allEntities = false;
    }
}
