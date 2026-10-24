/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.commandcentre;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

/**
 * A row of a list of the Teams command centre: a title over a line of detail, a coloured stripe on its left and a
 * badge on its right.
 *
 * @param key
 *            names the row within its list, given back when it is clicked
 * @param title
 *            the first line
 * @param detail
 *            the second line, drawn dimmer; empty for none
 * @param badge
 *            a short tag drawn at the right, such as a state; empty for none
 * @param colour
 *            the RGB colour of the stripe, such as a team's colour; 0 for the centre's own
 * @param badgeColour
 *            the RGB colour of the badge
 * @param selected
 *            whether the row is drawn as the chosen one
 */
@ApiStatus.Experimental
public record CommandCentreRow(String key, Component title, Component detail, Component badge, int colour, int badgeColour, boolean selected)
{
    /** The colour of a badge that reports nothing in particular. */
    public static final int NEUTRAL = 0xA9B4C2;
    /** The colour of a badge for something running or done. */
    public static final int GOOD = 0x6FD08C;
    /** The colour of a badge for something that waits or needs attention. */
    public static final int WARNING = 0xE3A13B;
    /** The colour of a badge for something stopped or failed. */
    public static final int BAD = 0xFF6B6B;

    /**
     * @return a row of a title over a line of detail, without a badge, stripe or selection
     */
    public static CommandCentreRow of(String key, Component title, Component detail)
    {
        return new CommandCentreRow(key, title, detail, Component.empty(), 0, NEUTRAL, false);
    }

    /**
     * @return this row with a badge
     */
    public CommandCentreRow withBadge(Component text, int rgb)
    {
        return new CommandCentreRow(key, title, detail, text, colour, rgb, selected);
    }

    /**
     * @return this row with a stripe of another colour
     */
    public CommandCentreRow withColour(int rgb)
    {
        return new CommandCentreRow(key, title, detail, badge, rgb, badgeColour, selected);
    }

    /**
     * @return this row drawn as the chosen one, or not
     */
    public CommandCentreRow selected(boolean chosen)
    {
        return new CommandCentreRow(key, title, detail, badge, colour, badgeColour, chosen);
    }
}
