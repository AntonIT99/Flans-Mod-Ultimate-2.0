/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A Teams spawner block of the round being played, as it stood when it was read. It belongs to whoever holds the base it
 * is connected to.
 *
 * @param id
 *            the spawner's persistent id
 * @param kind
 *            what the spawner spawns
 * @param dimension
 *            the dimension the spawner is in
 * @param position
 *            the spawner block
 * @param baseId
 *            the base the spawner is connected to, or null when it is connected to none
 * @param owner
 *            the team holding that base, or null when nobody does
 */
@ApiStatus.Experimental
public record TeamsSpawner(UUID id, Kind kind, ResourceKey<Level> dimension, BlockPos position, @Nullable UUID baseId, @Nullable ITeam owner)
{
    /** What a spawner spawns. */
    @ApiStatus.Experimental
    public enum Kind
    {
        /** Players of the owning team come back on it. */
        PLAYER,
        /** Items circle it until a member of the owning team takes them. */
        ITEM,
        /** Driveables and AA guns appear at it. */
        VEHICLE
    }
}
