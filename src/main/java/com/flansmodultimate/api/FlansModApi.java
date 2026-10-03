/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.PlatformEnvironment;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Entry point for looking up the definitions of the loaded content packs.
 *
 * <p>Content packs are read while Flan's Mod Ultimate is constructed, so every definition is
 * available once registry events fire, on both sides.</p>
 */
@ApiStatus.Experimental
public final class FlansModApi
{
    private FlansModApi() {}

    /**
     * Reevaluate player tracking after an addon changes an entity's distance classification.
     * Call on the server thread; pairings are refreshed on the next server tick.
     * @param level the owning server level
     */
    public static void refreshEntityTracking(ServerLevel level)
    {
        if (level.getServer().isSameThread())
            com.flansmodultimate.common.entity.EntityDistancePolicy.requestTrackingRefresh();
    }

    /**
     * @return the loaded content packs: packaged ones first, then the user's in alphabetical order
     */
    public static List<IContentPack> getContentPacks()
    {
        return Collections.unmodifiableList(ContentManager.getContentPacks());
    }

    /**
     * @param shortName the definition's short name, which is also the path of its item id
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
        return server != null && server == level.getServer() && server.isSameThread()
            && ModCommonConfig.setDimensionFactor(level.dimension().location(), true, value);
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
        return server != null && server == level.getServer() && server.isSameThread()
            && ModCommonConfig.setDimensionFactor(level.dimension().location(), false, value);
    }

    /** Remove the gravity override for this dimension and restore the factor of one. Server thread only. */
    public static boolean clearGravityFactor(ServerLevel level)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread()
            && ModCommonConfig.clearDimensionFactor(level.dimension().location(), true);
    }

    /** Remove the drag override for this dimension and restore the factor of one. Server thread only. */
    public static boolean clearDragFactor(ServerLevel level)
    {
        var server = PlatformEnvironment.currentServer();
        return server != null && server == level.getServer() && server.isSameThread()
            && ModCommonConfig.clearDimensionFactor(level.dimension().location(), false);
    }
}
