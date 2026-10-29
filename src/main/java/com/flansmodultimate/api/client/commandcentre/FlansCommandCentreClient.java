/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.commandcentre;

import com.flansmodultimate.client.gui.centre.CommandCentreClient;
import org.jetbrains.annotations.ApiStatus;

/**
 * The client side of the Teams command centre: the pages addons add to it, and opening it. Pages read the sections
 * their addon registers with {@link com.flansmodultimate.api.commandcentre.FlansCommandCentre#addSection}. Client only.
 */
@ApiStatus.Experimental
public final class FlansCommandCentreClient
{
    private FlansCommandCentreClient()
    {}

    /**
     * Adds a page to the centre. Call it once, during client setup.
     *
     * @param page
     *            the page; one with the id of a page added before replaces it
     */
    public static void addPage(ICommandCentrePage page)
    {
        CommandCentreClient.addPage(page);
    }

    /**
     * Asks the server to open the centre for the local player, showing a page of it. The server refuses players who are
     * not operators. Client thread.
     *
     * @param pageId
     *            the page to show, or an empty string for the one shown last
     */
    public static void open(String pageId)
    {
        CommandCentreClient.requestOpen(pageId);
    }
}
