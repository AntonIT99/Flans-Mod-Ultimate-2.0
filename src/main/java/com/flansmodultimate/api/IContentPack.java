/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

/** A loaded content pack: a folder or archive of definitions and assets, or a pack packaged in a mod. */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IContentPack
{
    /**
     * @return the pack's display name, usually its folder or archive name
     */
    String getName();

    /**
     * @return where the pack is loaded from. Read it, never modify it: the game may hold it open.
     */
    Path getPath();

    /**
     * @return whether the pack is official content bundled by the official-packs companion mod
     */
    default boolean isOfficial()
    {
        return false;
    }
}
