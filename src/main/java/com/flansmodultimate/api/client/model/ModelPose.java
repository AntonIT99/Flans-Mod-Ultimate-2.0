/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.client.model;

import com.flansmodultimate.api.driveables.MountAim;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * The moving state, for one game tick, of a content-pack model drawn by {@link FlansModelPreviews#renderWorld} for an
 * owner that is not a Flan's Mod entity. The renderer derives wheel, track, propeller, leg and recoil motion from
 * successive poses of the same entity and interpolates between ticks. Client only.
 *
 * @param turret
 *            aim of the main turret and gun, or of an AA gun
 * @param seats
 *            aim of each seat's own gun, indexed by seat with the driver at 0; a seat past the end of the list
 *            faces its rest direction, and the driver's seat then follows the turret
 * @param throttle
 *            engine power from -1 (full reverse) to 1: it spins propellers and rotors and paces walker legs
 * @param travel
 *            blocks travelled along the hull this tick, negative in reverse: it turns wheels and tracks and paces
 *            mecha legs
 * @param turn
 *            degrees the hull turned this tick, positive to the right: it steers wheels and counter-rotates tracks
 * @param doorsOpen
 *            whether doors are drawn open
 * @param gearDeployed
 *            whether landing gear is drawn down
 * @param wingsFolded
 *            whether variable wings are drawn in their second position
 * @param engineRunning
 *            whether the engine runs; a stopped engine leaves propellers and rotors still
 * @param shots
 *            running count of shots fired by the owner; an increase starts a recoil stroke
 * @param barrel
 *            index of the AA gun barrel that fired the latest shot
 */
@ApiStatus.Experimental
public record ModelPose(MountAim turret, List<MountAim> seats, float throttle, float travel, float turn, boolean doorsOpen, boolean gearDeployed, boolean wingsFolded, boolean engineRunning, int shots,
    int barrel)
{

    /** The model at rest with its engine running: guns forward, doors closed, gear down. */
    public static final ModelPose REST = new ModelPose(MountAim.REST, List.of(), 0F, 0F, 0F, false, true, false, true, 0, 0);

    /** Makes the seat list immutable and replaces values no model can draw. */
    public ModelPose
    {
        turret = turret == null ? MountAim.REST : turret;
        seats = seats == null ? List.of() : List.copyOf(seats);
        throttle = Float.isFinite(throttle) ? Math.max(-1F, Math.min(1F, throttle)) : 0F;
        travel = Float.isFinite(travel) ? travel : 0F;
        turn = Float.isFinite(turn) ? turn : 0F;
        barrel = Math.max(0, barrel);
    }
}
