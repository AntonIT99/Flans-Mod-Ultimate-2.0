/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.teams;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A flag of the Capture the Flag round being played, as it stood when it was read. Players take, return and capture a
 * flag by clicking it, combatants by touching it: standing within a block and a half of it.
 *
 * @param id
 *            the flag's persistent id
 * @param owner
 *            the team whose flag it is, or null when its base belongs to nobody
 * @param position
 *            where the flag is now, in world coordinates: at its base, on its carrier or where it was dropped
 * @param home
 *            the capture destination at its base, kept at interaction height even when the pole is taller
 * @param atHome
 *            whether the flag stands at its base
 * @param carrier
 *            the player or combatant carrying it, or null
 */
@ApiStatus.Experimental
public record TeamsFlag(UUID id, @Nullable ITeam owner, Vec3 position, Vec3 home, boolean atHome, @Nullable UUID carrier)
{}
