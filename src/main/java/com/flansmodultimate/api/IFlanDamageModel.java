/*
 * Copyright (c) 2026 Wolff (AntonIT99)
 * SPDX-License-Identifier: MIT
 * See LICENSE-API at the root of the Flan's Mod Ultimate repository.
 */
package com.flansmodultimate.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Implemented by an entity that stands in for an AA gun or driveable, such as an NPC shaped like a tank, so that Flan
 * ammunition and explosions damage it as they would damage that definition.
 *
 * <p>
 * With a plane definition the entity takes the ammunition's damage against planes, with another driveable its damage
 * against vehicles, and with an AA gun the ammunition's base damage, as a placed AA gun does. Without a definition the
 * entity takes ordinary living-entity damage. Queried on the server thread whenever Flan damage is resolved.
 * </p>
 */
@ApiStatus.Experimental
public interface IFlanDamageModel
{
    /** @return the AA gun or driveable whose damage profile this entity takes, or empty for living-entity damage */
    Optional<IContentType> getFlansDamageModel();

    /**
     * Whether kinetic rounds and shaped charges are resolved against the driveable's core armour, on the face the shot
     * strikes as seen from the entity's body yaw. Armour that stops a round leaves the entity unhurt. Ignored for AA guns
     * and when no driveable is returned.
     *
     * @return whether the driveable's armour protects this entity
     */
    default boolean flansModelArmour()
    {
        return true;
    }
}
