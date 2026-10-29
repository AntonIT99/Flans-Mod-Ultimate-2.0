/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.entity.Entity;

import java.util.Optional;

/** Part hitboxes of driveable definitions for entities that are not driveables. See {@link IPartBody}. */
@ApiStatus.Experimental
public final class FlansPartBodies
{
    private FlansPartBodies()
    {}

    /**
     * Builds the part body of a definition for an entity and makes Flan's hit detection look for that entity. The
     * entity has to implement {@link IFlanPartTarget} and hand the body out; a body it no longer hands out is no longer
     * hit. Either side after content loading, on the thread of the entity's level.
     *
     * @param owner
     *            the entity that wears the body
     * @param type
     *            a vehicle, plane or mecha definition
     * @return the body at rest with every part intact, or empty when the definition is no driveable or gives no part a
     *         box
     */
    public static Optional<IPartBody> create(Entity owner, IContentType type)
    {
        return PartBodies.create(owner, type);
    }
}
