/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * An engine audio clip, inspected on either side after content loading.
 *
 * @param sound
 *            registered sound event ID; unqualified paths select the flansmod namespace
 * @param range
 *            audible radius in blocks, including the server's pack-range policy
 * @param repeatTicks
 *            minimum interval between starts, in game ticks (20 per second)
 */
@ApiStatus.Experimental
public record EngineSound(String sound, float range, int repeatTicks)
{}
