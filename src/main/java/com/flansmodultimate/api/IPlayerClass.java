/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A class of a {@link ITeam}: the loadout a player spawns with in the Teams game modes, and
 * optionally the skin worn while playing it. Read-only content data, reached through
 * {@link ITeam#getClasses()}. Readable on either side after content loading unless a method
 * says otherwise.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface IPlayerClass extends IContentType
{
    /**
     * The armour piece the class names for a slot. A slot the class leaves open is filled by
     * {@link ITeam#getArmour(EquipmentSlot)}.
     *
     * @param slot
     *            one of the four armour slots
     * @return a new stack, empty when the class names nothing for the slot
     */
    ItemStack getArmour(EquipmentSlot slot);

    /**
     * Builds the items the class hands out, in the order the definition lists them. Guns come
     * with the attachments the definition names and loaded with their default ammunition. Items
     * that are not loaded are left out.
     *
     * @return new stacks the caller owns
     */
    List<ItemStack> createStartingItems();

    /**
     * Client only: the location is resolved while client assets are read, so a dedicated server
     * always answers null. The texture is not checked: it may be missing, and it may use either
     * the square player skin layout or the old 2:1 one.
     *
     * @return the skin texture the class declares, or null when it declares none
     */
    @Nullable
    ResourceLocation getSkinOverrideTexture();
}
