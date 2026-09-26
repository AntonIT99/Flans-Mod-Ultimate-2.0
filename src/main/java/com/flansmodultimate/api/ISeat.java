/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * A seat of a driveable: the driver's position or a passenger's, possibly manning a gun.
 *
 * <p>The gun state is synchronised to clients, so it can be read on both sides.</p>
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ISeat
{
    /**
     * @return the index of the seat in its driveable's definition, 0 being the driver's by default
     */
    int getSeatIndex();

    boolean isDriverSeat();

    /**
     * @return the entity sitting in this seat, if any
     */
    @Nullable
    Entity getRiddenByEntity();

    /**
     * @return the driveable entity this seat belongs to, or null before the seat is bound to it
     */
    @Nullable
    Entity getDriveable();

    /**
     * @return the definition of the driveable this seat belongs to
     */
    Optional<IDriveableType> getDriveableType();

    /**
     * @return where the seat aims, in degrees relative to the driveable
     */
    float getAimYaw();

    /**
     * @return where the seat aims, in degrees relative to the driveable
     */
    float getAimPitch();

    /**
     * @return the gun this seat mans, if any
     */
    Optional<IContentType> getGunType();

    /**
     * @return the rounds left in the seat gun's current magazine
     */
    int getGunRounds();

    int getGunMagazineSize();

    /**
     * @return the ticks left before the seat gun has reloaded, 0 when it is not reloading
     */
    int getGunReloadTicks();

    /**
     * @return the name of the ammunition the seat gun is loaded with, empty when it has none
     */
    Component getGunAmmoName();
}
