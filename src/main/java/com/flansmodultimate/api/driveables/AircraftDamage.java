/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

/**
 * How much is left of the parts that keep an aircraft in the air, each as the share of its hit points it still has: 1
 * for a part that is whole or that the aircraft does not have, 0 for one that has been shot off. A real driveable and
 * an NPC drawn as an aircraft describe their damage the same way, so that both fly and fall alike.
 *
 * @param leftWing
 *            the left wing
 * @param rightWing
 *            the right wing
 * @param tail
 *            the tail: a plane's elevators and rudder, a helicopter's tail boom
 * @param airframe
 *            the structure carrying the wings; nothing flies once it is gone
 * @param engine
 *            the engines or propellers, as the share of their power still there
 * @param rotor
 *            a rotorcraft's main rotor blades
 * @param tailRotor
 *            a rotorcraft's tail rotor, which holds the fuselage against the main rotor's torque
 */
@ApiStatus.Experimental
public record AircraftDamage(float leftWing, float rightWing, float tail, float airframe, float engine, float rotor, float tailRotor)
{
    /** An aircraft without a scratch. */
    public static final AircraftDamage INTACT = new AircraftDamage(1F, 1F, 1F, 1F, 1F, 1F, 1F);

    /** Every share is kept between 0 and 1, and a share that is not a number counts as a whole part. */
    public AircraftDamage
    {
        leftWing = share(leftWing);
        rightWing = share(rightWing);
        tail = share(tail);
        airframe = share(airframe);
        engine = share(engine);
        rotor = share(rotor);
        tailRotor = share(tailRotor);
    }

    /** @return whether nothing at all has been damaged */
    public boolean isIntact()
    {
        return leftWing >= 1F && rightWing >= 1F && tail >= 1F && airframe >= 1F && engine >= 1F && rotor >= 1F && tailRotor >= 1F;
    }

    private static float share(float value)
    {
        if (!Float.isFinite(value))
            return 1F;
        return Math.max(0F, Math.min(1F, value));
    }
}
