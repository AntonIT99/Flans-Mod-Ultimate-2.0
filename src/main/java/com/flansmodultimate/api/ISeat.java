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
import java.util.Set;

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
     * aiming speed, within its traverse and elevation limits. The driver's aim turns the driveable's turret, or a
     * mecha's torso, as a player driver's does. Call it once per tick while the occupant aims; turning that a call cannot
     * finish carries on from the next call. A player's own input always wins: the call is refused while a player sits
     * in the seat. Server thread.
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
     * Turns the seat so its gun points at a world position, under the conditions of {@link #setAutomatedAim}. A driver
     * without a gun of its own points the driveable's primary weapon, or else its secondary one; see
     * {@link #getDriverWeaponOrigin}. The seat aims straight at the position; leading a moving target or dropping for
     * range is up to the caller.
     *
     * @param target
     *            the world position to point at
     * @return whether the seat took the request; false as well when the seat has no gun to point
     */
    boolean aimAt(Vec3 target);

    /**
     * Holds or releases the seat's triggers for a non-player occupant, under the conditions of
     * {@link #setAutomatedAim}. The seat gun fires on the primary trigger at its own cadence and fire mode for as long
     * as it is held, and only with ammunition in the driveable; the driver's triggers fire the driveable's primary and
     * secondary weapons, as a player driver's do. A semi-automatic weapon fires once each time its trigger is pressed.
     * The triggers stay as set until changed, and are released when the occupant leaves. Server thread.
     *
     * @param primary
     *            whether the primary trigger is held
     * @param secondary
     *            whether the secondary trigger is held
     * @return whether the seat took the request
     */
    boolean setAutomatedTriggers(boolean primary, boolean secondary);

    /**
     * Holds the movement controls of the driveable for a non-player occupant of the seat that controls its movement,
     * normally the driver's, as a player holding the keys of the same name would: the engine starts, fuel is used and
     * damage limits the driveable as for a player. Controls left out are released. The controls stay as set until
     * changed, and are released when the occupant leaves. Steering towards a place, or out of trouble, is up to the
     * caller; {@link #getDriveDirection()} tells where the driveable heads. Server thread.
     *
     * @param controls
     *            the controls held from now on
     * @return whether the seat took the request: it is on the server, controls the movement of a driveable that is not
     *         a wreck, and is occupied by a living entity that is not a player
     */
    boolean setAutomatedDriving(Set<DriveControl> controls);

    /**
     * @return the horizontal world direction, of unit length, in which {@link DriveControl#FORWARD} moves the driveable
     *         now: where a vehicle's hull or a plane's nose points, or where a mecha's torso faces, which its driver's
     *         aim turns. {@link DriveControl#RIGHT} steers a vehicle or plane towards the right of it and steps a mecha
     *         that way. Empty for a seat that does not control the driveable's movement. Server side.
     */
    Optional<Vec3> getDriveDirection();

    /**
     * @param secondary
     *            whether to describe the weapon of the secondary trigger rather than the primary one
     * @return where the driveable's weapon that the driver's trigger fires shoots from now, in world coordinates: the
     *         first shoot point of that weapon, or the mecha hand holding a gun. Empty for a seat that does not control
     *         the driveable, and for a driveable without such a weapon. Server side.
     */
    Optional<Vec3> getDriverWeaponOrigin(boolean secondary);

    /**
     * @param secondary
     *            whether to describe the weapon of the secondary trigger rather than the primary one
     * @return the unit world direction the driveable's weapon that the driver's trigger fires shoots along now; empty
     *         when {@link #getDriverWeaponOrigin} is. Server side.
     */
    Optional<Vec3> getDriverWeaponDirection(boolean secondary);
}
