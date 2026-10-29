/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.driveables;

import com.flansmodultimate.client.input.KeyInputHandler;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.KeyMapping;

/**
 * The key binds players set for Flan's driveables, so that addon vehicles answer to the same keys. Client only, on the
 * client thread.
 */
@ApiStatus.Experimental
public final class FlansDriveableKeys
{
    private FlansDriveableKeys()
    {}

    /**
     * The bind that moves a rider to the next free seat. Its conflict context is live only while the local player rides a
     * Flan's driveable, so it neither counts clicks nor reports itself down anywhere else: an addon vehicle reads the state
     * of its bound key or mouse button instead.
     *
     * @return the change-seat bind, as the player set it
     */
    public static KeyMapping changeSeat()
    {
        return KeyInputHandler.changeSeatKey();
    }
}
