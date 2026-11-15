/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.DriveableSeatSupport;
import com.flansmodultimate.config.ModCommonConfig;
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

    /**
     * Whether the occupant of a seat dies when a part is destroyed, as on a driveable: those seated inside a turret die
     * with it. The main turret holds every seat that turns with it; a numbered turret only the seats in itself.
     *
     * @param mount
     *            the seat
     * @param destroyedPart
     *            short name of the part just destroyed
     * @return whether the seat's occupant dies with that part
     */
    public static boolean diesWithPart(WeaponMount mount, String destroyedPart)
    {
        return EnumDriveablePart.isSeatInTurret(EnumDriveablePart.getPart(mount.part()), EnumDriveablePart.getPart(destroyedPart));
    }

    /**
     * Whether an occupied passenger gun stays in use while the part its seat sits in is destroyed, as the server's
     * {@code gunsInDestroyedPartsWork} setting says. A destroyed seat can still not be newly occupied.
     */
    public static boolean gunsInDestroyedPartsWork()
    {
        return ModCommonConfig.gunsInDestroyedPartsWork();
    }
}
