/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;

/**
 * How an aircraft definition performs in the air when a player flies it, with its reference engine and intact parts.
 * Speeds are in blocks per tick. Values reflect the force-legacy, speed-scale and plane speed-cap configuration at the
 * time of the call. Safe to read on either side after content loading.
 *
 * @param topSpeed
 *            fastest the aircraft flies level at full throttle, above zero
 * @param minimumSpeed
 *            speed below which a fixed-wing aircraft no longer flies: its reference airspeed under a complete real-world
 *            profile, otherwise its {@code TakeoffSpeed}; zero for a rotorcraft, which hovers
 * @param climbRate
 *            authored rate of climb, when the definition has one
 * @param pitchAuthority
 *            the definition's elevator rates ({@code LookUpSpeed} and {@code LookDownSpeed}) relative to the default of 1
 * @param rollAuthority
 *            the definition's aileron rates ({@code RollLeftSpeed} and {@code RollRightSpeed}) relative to the default of 1
 * @param rotorcraft
 *            whether it flies on rotors and may hover, as a helicopter or VTOL does
 * @param realistic
 *            whether a complete real-world profile, rather than the legacy flight model, supplies the speeds
 */
@ApiStatus.Experimental
public record FlightPerformance(double topSpeed, double minimumSpeed, OptionalDouble climbRate, double pitchAuthority, double rollAuthority, boolean rotorcraft, boolean realistic)
{}
