/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;

/**
 * How a ground-vehicle definition gains and sheds speed along its hull on level ground, the way the vehicle itself
 * does when a player drives it: from engine power against mass under a complete real-world profile, otherwise from the
 * definition's legacy throttle response. Assumes the reference engine, intact parts and the overworld's drag; installed
 * engine parts, damage, slopes and water are not included. Speeds are in blocks per tick and accelerations in blocks per
 * tick squared, always as magnitudes. Values reflect the force-legacy, speed-scale and speed-cap configuration at the
 * time of the call. Safe to read on either side after content loading.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface GroundPropulsion
{
    /** @return speed the vehicle settles at under full forward demand, above zero */
    double topSpeed();

    /** @return speed it settles at under full reverse demand; zero when the definition cannot reverse */
    double reverseSpeed();

    /** @return whether a complete real-world profile, rather than the legacy throttle response, supplies the values */
    boolean realistic();

    /** @return mass in kilograms when the model uses it; empty for a legacy response that ignores mass */
    OptionalDouble massKg();

    /** @return reference engine power in kilowatts when the model uses it; empty for a legacy response */
    OptionalDouble enginePowerKw();

    /**
     * @param speed
     *            current speed along the hull, in either direction
     * @return speed gained per tick under full demand at that speed; it falls to zero toward the top speed of a realistic
     *         profile
     */
    double acceleration(double speed);

    /**
     * @param speed
     *            current speed along the hull, in either direction
     * @param brake
     *            0 when the driver only lets off the demand, 1 under full brake; values in between blend the two and
     *            values outside are clamped
     * @return speed lost per tick at that speed, above zero
     */
    double deceleration(double speed, double brake);
}
