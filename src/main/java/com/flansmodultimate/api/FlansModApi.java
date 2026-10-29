/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.content.IContentPack;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.content.ContentManager;
import com.flansmodultimate.platform.PlatformEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Entry point for looking up the definitions of the loaded content packs.
 *
 * <p>
 * Content packs are read while Flan's Mod Ultimate is constructed, so every definition is
 * available once registry events fire, on both sides.
 * </p>
 */
@ApiStatus.Experimental
public final class FlansModApi
{
    private static final Set<String> NAMESPACES = Set.of(FlansMod.MOD_ID, FlansMod.FLANSMOD_ID, FlansMod.APOCALYPSE_ID);

    private FlansModApi()
    {}

    /**
     * The namespaces Flan's Mod Ultimate registers under, so that an addon walking a registry or reading an identifier
     * can tell the mod's own content from another mod's: {@code flansmodultimate} for the mod itself, {@code flansmod}
     * for everything the content packs define, items, blocks, entities and sounds alike, and {@code flansmodapocalypse}
     * for the Apocalypse module. Readable on either side at any time.
     *
     * @return the namespaces, unmodifiable
     */
    public static Set<String> getModNamespaces()
    {
        return NAMESPACES;
    }

    /**
     * Reads the range, set by the server, within which NPCs using the Flan distance settings are sent to clients. An
     * NPC cannot usefully fight an enemy it is not sent to, so addons use it as the upper limit of NPC combat ranges.
     * Synchronised to clients, so either side may read it.
     *
     * @return tracking range in blocks
     */
    public static int getNpcTrackingRange()
    {
        return ModCommonConfig.flanNpcTrackingRange();
    }

    /**
     * Reevaluate player tracking after an addon changes an entity's distance classification.
     * Call on the server thread; pairings are refreshed on the next server tick.
     *
     * @param level
     *            the owning server level
     */
    public static void refreshEntityTracking(ServerLevel level)
    {
        if (level.getServer().isSameThread())
            com.flansmodultimate.common.entity.EntityDistancePolicy.requestTrackingRefresh();
    }

    /**
     * Keep an entity sent to one player whatever the distance, for example while that player edits it from afar. A player has
     * at most one pinned entity: pinning another replaces it. The pairing changes at once, so a packet naming the entity may
     * follow. Pins end when the player logs out. Call on the server thread.
     *
     * @param player
     *            the player who receives the entity
     * @param entity
     *            the entity to pin in the player's level, or {@code null} to remove the player's pin
     */
    public static void pinEntityTracking(ServerPlayer player, @Nullable Entity entity)
    {
        if (player.getServer() != null && player.getServer().isSameThread())
            com.flansmodultimate.common.entity.EntityTrackingPins.pin(player, entity);
    }

    /**
     * @return whether the entity is the one pinned for the player by {@link #pinEntityTracking}
     */
    public static boolean isEntityTrackingPinned(ServerPlayer player, Entity entity)
    {
        return com.flansmodultimate.common.entity.EntityTrackingPins.isPinned(entity, player);
    }

    /**
     * @return the loaded content packs: packaged ones first, then the user's in alphabetical order
     */
    public static List<IContentPack> getContentPacks()
    {
        return List.copyOf(ContentManager.getContentPacks());
    }

    /**
     * @param shortName
     *            the definition's short name, which is also the path of its item id
     */
    public static Optional<IContentType> getType(String shortName)
    {
        return Optional.ofNullable(InfoType.getInfoType(shortName));
    }

    /**
     * @return every definition that has an item, in no particular order
     */
    public static Collection<IContentType> getTypes()
    {
        return Collections.unmodifiableCollection(InfoType.getInfoTypes().values());
    }

    /**
     * @return effective pack and server-authored teams, unmodifiable and in loading order; disabled
     *         entries and built-in spectators are left out. Server-thread queries use server state;
     *         clients see synchronized definitions after login.
     */
    public static Collection<ITeam> getTeams()
    {
        return List.copyOf(Team.values());
    }

    /**
     * @return all effective classes, including server-created classes without a team; unmodifiable.
     *         Server thread sees server definitions; clients see synchronized definitions after login.
     */
    public static Collection<IPlayerClass> getPlayerClasses()
    {
        return List.copyOf(PlayerClass.values());
    }

    /**
     * @param shortName
     *            the effective class ID
     * @return the class, including server-created classes, or empty when unknown or disabled; either side
     */
    public static Optional<IPlayerClass> getPlayerClass(String shortName)
    {
        return Optional.ofNullable(PlayerClass.getPlayerClass(shortName));
    }

    /**
     * @param shortName
     *            the team's short name, as {@link ITeam#getShortName()} reports it
     * @return the effective team with that name, or empty for an unknown or disabled name and for the built-in
     *         spectators. Either side.
     */
    public static Optional<ITeam> getTeam(String shortName)
    {
        Team team = Team.getTeam(shortName);
        return team == Team.SPECTATORS ? Optional.empty() : Optional.ofNullable(team);
    }

    /**
     * Effective gravity multiplier for Flan's physics in this dimension. Unlisted dimensions
     * return one. Readable on either side; clients see the server value after config sync.
     */
    public static double getGravityFactor(Level level)
    {
        return ModCommonConfig.gravityFactor(level);
    }

    /**
     * Effective drag multiplier for Flan's physics in this dimension. Unlisted dimensions
     * return one. Readable on either side; clients see the server value after config sync.
     */
    public static double getDragFactor(Level level)
    {
        return ModCommonConfig.dragFactor(level);
    }

    /**
     * Persist and broadcast a gravity override for this dimension, in the range 0 to 10.
     * Call on the server thread. It changes only Flan's physics.
     *
     * @return true if the value changed; false for an invalid value or unavailable server
     */
    public static boolean setGravityFactor(ServerLevel level, double value)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread() && ModCommonConfig.setDimensionFactor(level.dimension().location(), true, value);
    }

    /**
     * Persist and broadcast a drag override for this dimension, in the range 0 to 10.
     * Call on the server thread. It changes only Flan's physics.
     *
     * @return true if the value changed; false for an invalid value or unavailable server
     */
    public static boolean setDragFactor(ServerLevel level, double value)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread() && ModCommonConfig.setDimensionFactor(level.dimension().location(), false, value);
    }

    /** Remove the gravity override for this dimension and restore the factor of one. Server thread only. */
    public static boolean clearGravityFactor(ServerLevel level)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread() && ModCommonConfig.clearDimensionFactor(level.dimension().location(), true);
    }

    /** Remove the drag override for this dimension and restore the factor of one. Server thread only. */
    public static boolean clearDragFactor(ServerLevel level)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread() && ModCommonConfig.clearDimensionFactor(level.dimension().location(), false);
    }
}
