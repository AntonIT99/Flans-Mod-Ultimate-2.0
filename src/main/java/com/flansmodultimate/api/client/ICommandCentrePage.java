/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A page of the Teams command centre, listed in its side bar. Addons implement it and register it with
 * {@link FlansCommandCentreClient#addPage}.
 *
 * <p>
 * One instance serves every time the centre is opened, so it may keep what the operator chose, such as the selected
 * entry of a list, for as long as the game runs; what it shows comes from the centre's sections each time. The centre
 * lays a page out again with {@link #init} whenever it is shown, the window is resized or new state arrives from the
 * server, so a page builds its widgets from the sections every time and keeps nothing it built. Client thread.
 * </p>
 */
@ApiStatus.Experimental
public interface ICommandCentrePage
{
    /**
     * @return the page's id, unique and namespaced with the addon's mod id, such as {@code mymod:convoys}
     */
    String getId();

    /**
     * @return the page's name in the side bar, short enough for about ten characters
     */
    Component getTitle();

    /**
     * @return what the page is for, shown when the side bar entry is hovered
     */
    default Component getDescription()
    {
        return Component.empty();
    }

    /**
     * @return the item drawn as the page's icon in the side bar
     */
    ItemStack getIcon();

    /**
     * @return where the page is listed: lower comes first. The main mod's pages use 100 to 500.
     */
    default int getOrder()
    {
        return 1000;
    }

    /**
     * @param view
     *            the centre, with the sections the server sent last
     * @return whether the page is listed; typically whether the server sent the section the page shows, which it does
     *         not when the addon is missing on the server
     */
    default boolean isAvailable(ICommandCentreView view)
    {
        return true;
    }

    /**
     * Lays the page out in the centre's content area: adds its widgets, fields and lists to the view.
     *
     * @param view
     *            the centre, with an empty content area
     */
    void init(ICommandCentreView view);

    /**
     * Draws what the page shows besides its widgets, such as panels and text, under the widgets.
     *
     * @param view
     *            the centre
     * @param graphics
     *            the screen's graphics, in GUI pixels
     * @param mouseX
     *            the mouse position, in GUI pixels
     * @param mouseY
     *            the mouse position, in GUI pixels
     * @param partialTick
     *            the fraction of the tick passed since the last one
     */
    default void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {}

    /**
     * Called once a client tick while the page is shown.
     *
     * @param view
     *            the centre
     */
    default void tick(ICommandCentreView view)
    {}
}
