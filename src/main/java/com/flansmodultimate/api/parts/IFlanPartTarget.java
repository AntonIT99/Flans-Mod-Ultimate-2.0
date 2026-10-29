/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api.parts;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.damagesource.DamageSource;

import java.util.Optional;

/**
 * Implemented by an entity that stands in for a driveable and wants to be hit as one: by the part, with the hitbox, hit
 * points and armour of each part, rather than as one bounding box.
 *
 * <p>
 * While the entity hands out a body, Flan rounds and explosions ignore its own bounding box. They trace the body's
 * parts instead and report every hit through {@link #flansmodultimateHurtPart}. Everything else that hurts the entity
 * still reaches it the usual way.
 * </p>
 */
@ApiStatus.Experimental
public interface IFlanPartTarget extends IFlanDamageModel
{
    /**
     * Asked on either side, by the thread of the entity's level, whenever a round or blast might reach the entity.
     *
     * @return the body to hit, created by {@link FlansPartBodies#create} for this entity, or empty to be hit as an
     *         ordinary entity
     */
    Optional<IPartBody> flansmodultimatePartBody();

    /**
     * Deals the damage of a round or blast that struck one part. Server side. The amount is in the hit points of the
     * definition, already reduced by the armour of the part and face that were struck; the entity decides how it takes
     * it, usually by hurting itself and taking what gets through from the part with {@link IPartBody#damage}.
     *
     * @param part
     *            name of the part that was struck, as in type files
     * @param source
     *            what dealt the damage
     * @param amount
     *            damage in the hit points of the definition
     * @return whether the entity was hurt
     */
    boolean flansmodultimateHurtPart(String part, DamageSource source, float amount);
}
