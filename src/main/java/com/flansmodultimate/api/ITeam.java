/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A team a content pack defines for the Teams game modes: its colours, the armour every member
 * wears and the classes its players choose from. Read-only content data; who plays for a team
 * and its score belong to the running round and are not exposed here. Look teams up through
 * {@link FlansModApi#getTeams()}. Readable on either side after content loading.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ITeam extends IContentType
{
    /**
     * @return the team's colour as {@code 0xRRGGBB}
     */
    int getTeamColour();

    /**
     * @return the chat colour the team's name is written in
     */
    ChatFormatting getTextColour();

    /**
     * @return the team's classes in the order the definition lists them, immutable; a class that
     *         is not loaded is left out
     */
    @SuppressWarnings("java:S1452") // The implementation returns its own class type through this covariant list.
    List<? extends IPlayerClass> getClasses();

    /**
     * The armour the team issues for a slot, which a member wears unless the chosen class names
     * its own piece there.
     *
     * @param slot
     *            one of the four armour slots
     * @return a new stack, empty when the team issues nothing for the slot
     */
    ItemStack getArmour(EquipmentSlot slot);
}
