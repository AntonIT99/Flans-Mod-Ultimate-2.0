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
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A seat of a driveable: the driver's position or a passenger's, possibly manning a gun.
 *
 * <p>
 * The gun state is synchronised to clients, so it can be read on both sides.
 * </p>
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

    /**
     * @return where the seat gun's first barrel fires from now, in world coordinates; empty when the seat mounts no
     *         gun of its own. Server side.
     */
    Optional<Vec3> getGunOrigin();

    /**
     * @return the unit world direction the seat gun fires along now; empty when the seat mounts no gun of its own.
     *         Server side.
     */
    Optional<Vec3> getGunDirection();

    /**
     * Turns the seat for a non-player occupant, such as an NPC gunner, the way a player's input would: at the seat's
     * aiming speed, within its traverse and elevation limits. Call it once per tick while the occupant aims; turning
     * that a call cannot finish carries on from the next call. A player's own input always wins: the call is refused
     * while a player sits in the seat. Server thread.
     *
     * @param yaw
     *            the aim to turn towards, in degrees relative to the driveable, as {@link #getAimYaw()}
     * @param pitch
     *            the aim to turn towards, in degrees relative to the driveable, as {@link #getAimPitch()}
     * @return whether the seat took the request: it is on the server, part of a driveable, and occupied by a living
     *         entity that is not a player
     */
    boolean setAutomatedAim(float yaw, float pitch);

    /**
     * Turns the seat so its gun points at a world position, under the conditions of {@link #setAutomatedAim}. The seat
     * aims straight at the position; leading a moving target or dropping for range is up to the caller.
     *
     * @param target
     *            the world position to point at
     * @return whether the seat took the request; false as well when the seat mounts no gun of its own
     */
    boolean aimAt(Vec3 target);

    /**
     * Holds or releases the seat's triggers for a non-player occupant, under the conditions of
     * {@link #setAutomatedAim}. The seat gun fires on the primary trigger at its own cadence and fire mode for as long
     * as it is held, and only with ammunition in the driveable. The triggers stay as set until changed, and are released
     * when the occupant leaves. Server thread.
     *
     * @param primary
     *            whether the primary trigger is held
     * @param secondary
     *            whether the secondary trigger is held
     * @return whether the seat took the request
     */
    boolean setAutomatedTriggers(boolean primary, boolean secondary);
}
