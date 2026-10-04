/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.List;

/**
 * One definition read from a content pack, such as a gun, an ammunition, an armour piece or a
 * driveable. Look definitions up through {@link FlansModApi}. Subtypes such as
 * {@link IDriveableType} describe the kinds of definition other mods can inspect further.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IContentType
{
    /**
     * @return the unique identifier of the definition, also the path of its item id in the
     *         {@code flansmod} namespace
     */
    String getShortName();

    /**
     * @return the name the definition declares, before any translation
     */
    String getName();

    /**
     * @return the description the definition declares, empty when it has none
     */
    String getDescription();

    IContentPack getContentPack();

    /**
     * @return the item standing for this definition, or null for definitions without one, such as teams
     */
    @Nullable
    Item getItem();

    /**
     * @return the texture the definition's model is drawn with by default, or null when it has none
     */
    @Nullable
    ResourceLocation getTexture();

    /**
     * Read on either side after content loading.
     * @return immutable paintjob descriptions in ID order, default first; empty for nonpaintable types
     */
    default List<PaintjobVariant> getPaintjobVariants()
    {
        return List.of();
    }
}
