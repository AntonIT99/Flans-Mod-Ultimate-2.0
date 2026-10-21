/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A capture point of the round being played, as it stood when it was read. Sides are numbered as in
 * {@link ITeamsRound}, and -1 means no side.
 *
 * @param id
 *            the point's persistent id
 * @param symbol
 *            the letter, from A, the point is known by
 * @param name
 *            the name a map maker gave the point
 * @param dimension
 *            the dimension the point is in
 * @param position
 *            the centre of the point, in world coordinates
 * @param radius
 *            the radius in blocks of the upright cylinder within which a combatant counts as standing on the point
 * @param ownerSide
 *            the side that owns the point, or -1
 * @param capturingSide
 *            the side taking a point nobody owns, or taking it down from its owner, or -1
 * @param progress
 *            from 0 to 1: how firmly the owner holds the point, or how far a neutral point has been taken
 * @param contested
 *            whether more than one side stands on the point
 * @param spawnPoint
 *            whether the side holding the point may come back at it
 */
@ApiStatus.Experimental
public record TeamsCapturePoint(UUID id, char symbol, String name, ResourceKey<Level> dimension, Vec3 position, int radius, int ownerSide, int capturingSide, float progress, boolean contested,
    boolean spawnPoint)
{
    /**
     * @return whether {@code side} holds the point outright, which earns it tickets and spawns
     */
    public boolean isHeldBy(int side)
    {
        return side >= 0 && ownerSide == side && progress >= 1F;
    }
}
