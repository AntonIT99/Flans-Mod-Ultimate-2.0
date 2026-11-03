/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.common.entity.DriveableSeatSupport;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

/** Reads who is in charge of a driveable, for addons that answer for what it does. */
@ApiStatus.Experimental
public final class FlansDriveables
{
    private FlansDriveables()
    {}

    /**
     * Who drives the driveable, and so answers for the rounds its weapons fire and for what it runs into. A driveable's
     * seats are entities of their own rather than passengers of it, so its occupants cannot be read off its passenger list.
     *
     * @param entity
     *            any entity; one that is no driveable has no driver
     * @return the occupant of the driveable's controlling seat, or empty for an empty seat or another entity
     */
    public static Optional<LivingEntity> getDriver(Entity entity)
    {
        return DriveableSeatSupport.driver(entity);
    }
}
