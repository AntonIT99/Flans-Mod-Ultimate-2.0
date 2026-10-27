/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.driveables;

import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;

/**
 * How a mecha definition walks when a player drives it, with its reference engine and no addons. It walks where it is
 * steered at once; {@code RotateSpeed} only turns its legs after it. Safe to read on either side after content loading.
 *
 * @param walkSpeed
 *            speed the chassis walks at, in blocks per tick: {@code RealMaxSpeedKmh}, or the legacy {@code MoveSpeed}
 * @param jumpHeight
 *            blocks it jumps when the definition authors {@code JumpHeight}; empty for the legacy default jump
 */
@ApiStatus.Experimental
public record MechaMobility(double walkSpeed, OptionalDouble jumpHeight)
{}
