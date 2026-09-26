/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.ApiStatus;

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
}
