/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.commandcentre;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Teams command centre as its pages see it: the content area a page lays itself out in, the sections the server
 * sent, and the commands a page runs. Coordinates are GUI pixels of the screen. Client thread.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ICommandCentreView
{
    /**
     * @return the left edge of the content area
     */
    int getLeft();

    /**
     * @return the top edge of the content area
     */
    int getTop();

    /**
     * @return the width of the content area; at least about 300 pixels
     */
    int getWidth();

    /**
     * @return the height of the content area; at least about 170 pixels
     */
    int getHeight();

    /**
     * @return the font the centre draws with
     */
    Font getFont();

    /**
     * Adds a widget to the page; it is drawn, takes input and is removed when the page is laid out again.
     *
     * @param widget
     *            any widget, such as a button
     * @return the widget
     */
    <T extends AbstractWidget> T addWidget(T widget);

    /**
     * Adds a text field whose text the centre keeps while the operator edits it: laying the page out again, as new state
     * from the server does, keeps the text and the focus. A field the operator has not typed in, or that a command run
     * from the page succeeded since, shows its initial text instead, so it follows what the server holds.
     *
     * @param key
     *            names the field within the page
     * @param initial
     *            the text it shows until the operator types, typically the value the server holds
     * @param hint
     *            what the field shows while it is empty
     * @return the field, 18 pixels high, taking up to 256 characters
     */
    EditBox addField(String key, int x, int y, int width, String initial, Component hint);

    /**
     * @param key
     *            names a field of the page
     * @return the field's text, or an empty string for a field the page has not added
     */
    String getField(String key);

    /**
     * Sets the text of a field as though the operator had typed it, such as from a button that fills in a position.
     *
     * @param key
     *            names a field of the page
     * @param value
     *            the text
     */
    void setField(String key, String value);

    /**
     * Adds a list of rows the operator picks from, scrolled with the mouse wheel; the centre keeps where it is scrolled
     * to while the page is laid out again.
     *
     * @param id
     *            names the list within the page
     * @param area
     *            where the list is drawn
     * @param rows
     *            the rows, 22 pixels high each
     * @param empty
     *            what the list shows without rows
     * @param onSelect
     *            called with the key of the row the operator clicks
     */
    void addList(String id, ScreenRectangle area, List<CommandCentreRow> rows, Component empty, Consumer<String> onSelect);

    /**
     * @param key
     *            a section's key
     * @return whether the server sent the section, which it does when the addon that registers it is installed there
     */
    boolean hasSection(ResourceLocation key);

    /**
     * @param key
     *            a section's key
     * @return the section as the server sent it last, a copy, or an empty tag when it sent none
     */
    CompoundTag getSection(ResourceLocation key);

    /**
     * @return client ticks since the sections arrived, to count timers on between updates
     */
    int getTicksSinceUpdate();

    /**
     * Runs a command on the server as the operator, and shows its output in the centre's status bar once the server
     * answers with new state.
     *
     * @param command
     *            a {@code /teams} or {@code /gamerule} command, with or without its slash; the server refuses any other
     */
    void runCommand(String command);

    /**
     * Asks the operator to confirm before something that cannot be undone.
     *
     * @param question
     *            what is about to happen
     * @param action
     *            runs once the operator confirms
     */
    void confirm(Component question, Runnable action);

    /**
     * Shows a message in the status bar, such as a field filled in wrongly.
     *
     * @param message
     *            the message
     * @param failure
     *            whether it reports a problem
     */
    void showMessage(Component message, boolean failure);

    /**
     * Shows another page of the centre.
     *
     * @param id
     *            the page's id; an unknown or unavailable one is ignored
     */
    void showPage(String id);

    /**
     * Lays the page out again, such as after the operator picked another tab of it.
     */
    void rebuild();

    /**
     * Draws a panel in the centre's style, to group what a page shows.
     *
     * @param graphics
     *            the screen's graphics
     * @param title
     *            a heading drawn in the panel's top strip, or null for none; a panel with a heading keeps its first 14
     *            pixels for it
     */
    void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, @Nullable Component title);
}
