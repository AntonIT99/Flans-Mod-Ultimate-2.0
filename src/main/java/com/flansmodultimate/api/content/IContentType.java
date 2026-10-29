/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.content;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.driveables.IDriveableType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
     * Readable on either side. Pack teams use {@code team.flansmod.<shortname>} and classes use
     * {@code class.flansmod.<shortname>}, lowercasing the original definition ShortName before registry collision aliases.
     * Items and blocks retain their existing {@code item.flansmod.<registered-shortname>} and
     * {@code block.flansmod.<registered-shortname>} keys, including collision aliases. Other itemless definitions
     * use {@code <type>.flansmod.<original-shortname>}, such as {@code loadout_pool.flansmod.example}.
     * The key is presentation data, never a content identifier. Explicit editor names remain literal.
     *
     * @return the optional name translation key, empty for a literal definition name
     */
    @SuppressWarnings("SameReturnValue") // Loaded definition implementations override this default with derived keys.
    default String getNameTranslationKey()
    {
        return "";
    }

    /**
     * Builds a fresh name component on either side, retaining the canonical name as its fallback.
     * Send the component to clients without resolving it on the server so each viewer's resources apply.
     *
     * @return the unstyled display name; {@link #getName()} remains the canonical name for saves and scripts
     */
    default MutableComponent getNameComponent()
    {
        String key = getNameTranslationKey();
        return key.isBlank() ? Component.literal(getName()) : Component.translatableWithFallback(key, getName());
    }

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
     *
     * @return immutable paintjob descriptions in ID order, default first; empty for nonpaintable types
     */
    default List<PaintjobVariant> getPaintjobVariants()
    {
        return List.of();
    }
}
