/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import com.flansmodultimate.common.teams.centre.CommandCentre;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

/**
 * The Teams command centre: the operators' screen for setting up and running Teams rounds, and whatever addons add to
 * it, such as skirmishes and wars fought by bots.
 *
 * <p>
 * The screen does nothing on its own. Everything an operator does in it runs as a {@code /teams} command on the server,
 * with the operator's own permissions, and the command's output is shown in the screen. What the screen shows comes
 * from <em>sections</em>: tag data that the server builds for the operator each time it sends the screen's state, after
 * every command and every few seconds while the screen is open. The main mod sends a section of its own for the Teams
 * game modes; an addon registers its own section here and adds pages that read it through
 * {@code com.flansmodultimate.api.client.FlansCommandCentreClient}.
 * </p>
 */
@ApiStatus.Experimental
public final class FlansCommandCentre
{
    /** The command every command the centre runs starts with, without its slash. */
    public static final String COMMAND = "teams";
    /** The operator permission level the centre and its commands require. */
    public static final int PERMISSION_LEVEL = 2;

    private FlansCommandCentre()
    {}

    /**
     * Registers a section of the state the centre sends operators. Call it once, while mods are set up; any thread.
     *
     * @param key
     *            the section's key, namespaced with the addon's mod id; a key registered again replaces the section
     * @param section
     *            builds the section for the operator it is sent to, on the server thread; it runs often, so it should
     *            stay quick, and should keep its data to what the addon's pages show. An exception it throws leaves the
     *            section out of that one state.
     */
    public static void addSection(ResourceLocation key, Function<ServerPlayer, CompoundTag> section)
    {
        CommandCentre.addSection(key, section);
    }
}
